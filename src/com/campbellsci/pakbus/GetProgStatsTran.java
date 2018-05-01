/* TranGetProgStats.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines a transaction that gets datalogger program statistics from the
 * station.
 */
public class GetProgStatsTran extends TransactionBase
{
   // the following constants define the possible outcom values
   // for this transaction.
   public static final int outcome_success = 0;
   public static final int outcome_link_failure = 1;
   public static final int outcome_port_failure = 2;
   public static final int outcome_timeout = 3;
   public static final int outcome_unroutable = 4;
   public static final int outcome_comm_failure = 5;
   public static final int outcome_unsupported = 6;
   public static final int outcome_permission_denied = 7;
   public static final int outcome_unknown = 8;
   
   
   /**
    * Constructor
    *
    * @param client_  Specifies the object that will receive an on_complete()
    * notification when the transaction has completed.
    */
   public GetProgStatsTran(GetProgStatsClient client_)
   { client = client_; }


   @Override
   public void on_failure(int reason) throws Exception
   {
      int outcome = outcome_unknown;;
      switch(reason)
      {
      case failure_comms:
         outcome = outcome_comm_failure;
         break;
         
      case failure_link:
         outcome = outcome_link_failure;
         break;
         
      case failure_port:
         outcome = outcome_port_failure;
         break;
         
      case failure_timeout:
         outcome = outcome_timeout;
         break;
         
      case failure_unroutable:
         outcome = outcome_unroutable;
         break;
         
      case failure_unsupported:
         outcome = outcome_unsupported;
         break;
      }
      on_complete(outcome);
   } // on_failure


   @Override
   public void on_focus_start() throws Exception
   {
      Packet command = new Packet();
      command.protocol_type = Packet.protocol_bmp5;
      command.message_type = Packet.bmp5_get_program_stats_cmd;
      command.add_uint2(station.get_security_code());
      post_message(command);
   } // on_focus_start


   @Override
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5 &&
            message.message_type == Packet.bmp5_get_program_stats_ack)
         {
            byte response_code = message.read_byte();
            if(response_code == 0)
            {
               // read info from the message
               station.os_version = message.read_string();
               station.os_signature= message.read_uint2();
               station.serial_no = message.read_string();
               station.power_up_program = message.read_string();
               station.compile_state = message.read_byte();
               station.program_name = message.read_string();
               station.program_signature = message.read_uint2();
               station.compile_time = message.read_nsec();
               station.compile_result = message.read_string();
               
               // we will imply the model number from the os version string
               int dot_pos = station.os_version.indexOf('.');
               if(dot_pos >= 0)
                  station.model_no = station.os_version.substring(0,dot_pos);
               
               // we now need to get the station name by querying the status table
               Packet command = new Packet();
               command.protocol_type = Packet.protocol_bmp5;
               command.message_type = Packet.bmp5_get_values_cmd;
               command.add_uint2(station.get_security_code());
               command.add_string("Status");
               command.add_byte(ColumnDef.type_ascii);
               command.add_string("StationName");
               command.add_uint2(64);  // send up to 256 bytes
               station.change_transaction_id(this);
               post_message(command);
            }  
            else
               on_complete(outcome_permission_denied);
         }
         else if(message.protocol_type == Packet.protocol_bmp5 &&
                 message.message_type == Packet.bmp5_get_values_ack)
         {
            byte resp_code = message.read_byte();
            if(resp_code == 0)
               station.station_name = message.read_string();
            on_complete(outcome_success);
         }
      }
      catch(Exception e)
      { on_complete(outcome_comm_failure); }
   } // on_message

   
   /**
    * Called when this transaction has finished its work.
    */
   private void on_complete(int outcome) throws Exception
   {
      close();
      if(client != null)
         client.on_complete(this,outcome);
   } // on_complete
   
   
   /**
    * Keeps the reference to the object that will be notified when this transaction is complete.
    */
   private GetProgStatsClient client;
}


