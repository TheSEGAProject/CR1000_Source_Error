/* TranClockSet.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 05 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;



/**
 * Defines a transaction that can either be used to read the datalogger clock
 * or to synchronise the datalogger clock with the host clock.
 */
public class ClockSetTran extends TransactionBase
{
   /**
    * class constructor
    *
    * @param client_   Reference to the object that will receive the completion
    * notification
    * @param nsec_diff_  Specifies the interval, in nano-seconds, by which the
    * clock should be adjusted.  Set to zero if the clock should only be read.
    */
   public ClockSetTran(
      ClockSetClient client_,
      long nsec_diff_)
   {
      priority = Packet.pri_high;
      client = client_;
      nsec_diff = nsec_diff_;
   } // constructor
   
   
   @Override
   public String get_name()
   { 
      String rtn = "Clock Set";
      if(nsec_diff == 0)
         rtn = "Clock Check";
      return rtn;
   }


   /**
    * Overloads the base class version to start the transaction
    */
   public void on_focus_start() throws Exception
   {
      Packet command = new Packet();
      int seconds = (int)(nsec_diff / 1000000000);
      int nsec = (int)(nsec_diff % 1000000000);

      command.protocol_type = Packet.protocol_bmp5;
      command.message_type = Packet.bmp5_clock_set_cmd;
      command.add_uint2(station.get_security_code());
      command.add_int4(seconds);
      command.add_int4(nsec);
      post_message(command);
   } // on_focus_start


   // the following fields specify the possible outcomes for the clock set
   // transaction
   public static final int outcome_unknown = 0;
   public static final int outcome_checked = 1;
   public static final int outcome_set = 2;
   public static final int outcome_link_failed = 3;
   public static final int outcome_port_failed = 4;
   public static final int outcome_comm_timeout = 5;
   public static final int outcome_unroutable = 6;
   public static final int outcome_unsupported = 7;
   public static final int outcome_security_failed = 8;
   

   /** 
    * Overloads the base class version to handle failures
    */
   public void on_failure(int reason) throws Exception
   {
      int outcome = outcome_unknown;
      release_focus();
      switch(reason)
      {
      case failure_link:
         outcome = outcome_link_failed;
         break;
         
      case failure_port:
         outcome = outcome_port_failed;
         break;
         
      case failure_timeout:
         outcome = outcome_comm_timeout;
         break;
         
      case failure_unroutable:
         outcome = outcome_unroutable;
         break;
         
      case failure_unsupported:
         outcome = outcome_unsupported;
         break;
      }
      client.on_complete(this,outcome);
      close();
   } // on_failure
   
   
   /**
    * Overloads the message handler
    */
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5 &&
            message.message_type == Packet.bmp5_clock_set_ack)
         {
            byte resp_code = message.read_byte();
            int outcome = outcome_checked;
            if(resp_code == 0)
               logger_time = message.read_nsec();
            else
               outcome = outcome_security_failed;
            if(nsec_diff != 0)
               outcome = outcome_set;
            close();
            client.on_complete(this,outcome);
         }
      }
      catch(Exception e)
      { on_failure(failure_comms); }
   } // on_message
   
   
   /**
    * @return the datalogger time or null if the transaction failed
    */
   public LoggerDate get_logger_time()
   { return logger_time; }
   
   
   /**
    * stores the client reference
    */
   private ClockSetClient client;


   /**
    * stores the nsec_diff parameter
    */
   private long nsec_diff;
   
   
   /**
    * Holds the time received from the datalogger.  Null if not received
    */
   private LoggerDate logger_time;
};
