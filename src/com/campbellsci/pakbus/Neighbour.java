/* Neighbour.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Tuesday 10 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;



/**
 * Keeps track of the state of a neighbour for the network class.  We track the
 * neighbour address as well as the time since last verification.  We also
 * track the number of times that we have attempted verification and whether
 * the next verification attempt should be delayed or should take place as soon
 * as possible.
 */
final class Neighbour
{
   /**
    * Constructor
    *
    * @param neighbour_address_  Specifies the PakBus address of the neighbour.
    * @param network_            Specifies the network
    */
   Neighbour(
      short neighbour_address_,
      Network network_)
   {
      neighbour_address = neighbour_address_;
      network = network_;
      verification_attempts = 0;
      verify_timer = null;
      delay_timer = null;
      verification_interval = 300000;
      delay_timeout = 0;
   } // constructor


   /**
    * Starts the verification process by posting a hello message for this
    * address.
    */
   public void start_verify() throws Exception
   {
      Packet hello_cmd = new Packet();
      HopMetric my_metric = new HopMetric(network.get_link_delay());
      
      hello_cmd.message_type = Packet.pakctrl_hello_cmd;
      hello_cmd.protocol_type = Packet.protocol_pakctrl;
      hello_cmd.neighbour_dest_address = neighbour_address;
      hello_cmd.dest_address = neighbour_address;
      hello_cmd.expect_more_code = Packet.expect_more;
      hello_cmd.add_byte((byte)0); // not a router
      hello_cmd.add_byte(my_metric.get_coded_value());
      // for dialed links, we want the datalogger to remove our neighbour record as soon as we
      // close the link.  a verification interval of 0xfffe (65534) will do just that.
      hello_cmd.add_uint2(network.get_reported_verify_interval());
      network.post_message(hello_cmd);
      ++network.comms_attempts;
      if(verification_attempts > 0)
         ++network.comms_retries;
   } // start_verify


   /**
    * Set to true if this neighbour needs immediate verification.
    */
   public boolean needs_verify()
   {
      boolean rtn = false;
      if(verify_timer == null)
         rtn = true;
      else
      {
         int verify_timeout = 2 * verification_interval + verification_interval / 2;
         if(verify_timer.elapsed() >= verify_timeout)
            rtn = true;
      }
      if(rtn && delay_timer != null)
      {
         if(delay_timer.elapsed() < delay_timeout)
            rtn = false;
         else
            delay_timer = null;
      }
      return rtn;
   } // needs_verify


   /**
    * Sets the delay timer up for a random amount of delay (up to one minute)
    */
   public void set_random_delay()
   {
      if(delay_timer != null)
      {
         delay_timer = new Timer();
         delay_timeout = network.random.nextInt(60000);
      }
   } // set_random_delay


   /**
    * @return the time passed since the last verify was started
    */
   public int get_elapsed()
   {
      int rtn = 0;
      if(elapsed_timer != null)
         rtn = elapsed_timer.elapsed();
      return rtn;
   } // get_elapsed
   

   /**
    * Keeps track of the neighbour address
    */
   protected short neighbour_address;


   /**
    * Reference to the network
    */
   protected Network network;
   
   
   /**
    * Keeps track of the number of times that we have attempted to verify the
    * link with the neighbour.
    */
   protected int verification_attempts;


   /**
    * Keeps track of the time since the last successful verification.  Will be
    * null if the link has not been verified.
    */
   protected Timer verify_timer;


   /**
    * Keeps track of the negotiated verification interval for this link
    */
   int verification_interval;
   

   /**
    * Keeps track of the amount of time that we should delay verifying this
    * link.   Will be null if there should be no delay.
    */
   protected Timer delay_timer;


   /**
    * Keeps track of the amount of time that verification should be delayed.
    */
   protected int delay_timeout;


   /**
    * Keeps track of the time since the verification was started
    */
   Timer elapsed_timer;
}

