/* Network.java

   Copyright (C) 2006, 2010 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 27 September 2006
   Last Change: Tuesday 22 June 2010
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;



/**
 * <p>This class implements the majority of the PakBus stack for a PakBus
 * application.  Specifically, it implements the low level protocol(s) using a
 * supplied driver object for low level communications (it also defines methods
 * that are meant to be called from that driver object that provide timing and
 * byte delivery services).</p>
 *
 * <p>This class also manages a collection of transactions and provides a
 * synchronisation service that allows one transaction to have have "focus" at
 * a time.  These transaction objects will allow the application to send and
 * receive PakBus messages.</p>
 */
public class Network
{
   /**
    * Specifies the value of the broadcast address
    */
   public static final short broadcast_address = 0x0FFF;
   
   
   /**
    * Specifies the max amount of time in msec that a link can last with no
    * communication
    */
   public static final int link_timeout = 40000;
   

   /**
    * constructor for the network class.
    *
    * @param pakbus_address_  Specifies the PakBus address of this node
    * @param input_           Specifies the input stream
    * @param output_          Specifies the output stream
    */
   public Network(
      short pakbus_address_,
      InputStream input_,
      OutputStream output_)
   {
      pakbus_address = pakbus_address_;
      input = input_;
      output = output_;
      links = new HashMap<Short, Link>();
      defunct_links = new LinkedList<Short>();
      stations = new HashMap<Short, Datalogger>();
      unsent_messages = new LinkedList<Packet>();
      focus_queue = new LinkedList<TransactionBase>();
      low_level_logs = new LinkedList<LowLevelLogger>();
      decoder = new LowLevelDecoder();
      neighbours = new HashMap<Short, Neighbour>();
      current_verify = null;
      comms_attempts = 0;
      comms_retries = 0;
      random = new Random();
      allow_unquoted = false;
      reported_verify_interval = 0xfffe;
   } // constructor

   
   /**
    * Re-initialises the i/o streams for this network following a shut down or an error.
    * 
    * @param input_  Specifies the input stream
    * @param output_  Specifies the output stream
    */
   public void set_io_streams(
         InputStream input_,
         OutputStream output_)
   {
      input = input_;
      output = output_;
      decoder = new LowLevelDecoder();
      comms_attempts = 0;
      comms_retries = 0;
      links.clear();
   } // set_io_streams
   
   
   /**
    * Returns the pakbus address used to identify this network
    */
   public short get_pakbus_address()
   { return pakbus_address; }
   

   /**
    * Calculates the amount of delay period to apply to the link
    */
   public int get_link_delay()
   { return 2500; }
   

   /**
    * <p>Calling this method will cause the network to process all available
    * bytes from the input stream and will also cause it to check the state of
    * all transactions, links, etc. that are time senstitive.  This method is
    * provided so that all i/o and output of events will happen in
    * synchronisation.  Because the network maintains time sensitive
    * components, this method should be called often (say, around 1/10
    * seconds).</p>
    *
    * @param close_open_links  Specifies that any links that are currently open
    * should be closed.
    * @return the number of links that are currently active.
    */
   public int check_state(boolean close_open_links) throws Exception
   {
      synchronized(stations)
      {
         // we'll first check to see if there is anything available from the input stream
         check_incoming();
         for(Link link : links.values())
            link.check_status(close_open_links);
      
         // we also need to check the state of all of the station
         for(Datalogger station: stations.values())
            station.check_state();
         
         // we need to check the state of our known neighbours
         if(current_verify != null)
         {
            if(current_verify.get_elapsed() > 5000)
            {  
               // if this was the third attempt, we need to get rid of this neighbour. 
               if(++current_verify.verification_attempts >= 3)
               {  
                  short neighbour_address = current_verify.neighbour_address;
                  links.remove(neighbour_address);
                  neighbours.remove(neighbour_address);
                  on_link_failure(neighbour_address);
               }
               else
                  current_verify.verify_timer = null;
               current_verify = null;
            }
         }
         else if(current_focus == null)
         {
            // we need to choose the neighbour that needs verification but has the least number of attempts.
            for(Neighbour neighbour: neighbours.values())
            {
               if(neighbour.needs_verify())
               {
                  if(current_verify == null)
                     current_verify = neighbour;
                  else if(neighbour.verification_attempts < current_verify.verification_attempts)
                     current_verify = neighbour;
               }
            }
            if(current_verify != null)
               current_verify.start_verify();
         }
         
         // we need to ensure that the current focus is still a valid transaction
         if(current_focus != null)
         {
            if(current_focus.get_is_satisfied())
               current_focus = null;
            else if(!current_focus.station.transactions.containsKey(current_focus.tran_no))
            {
               this.add_comment(
                  "The current focus, " + current_focus.get_name() + ", is no longer kept by its station");
               current_focus = null;
            }
         }
         if(current_focus == null && current_verify == null)
            set_next_focus();
         
         // we need to kick the low level loggers as well
         for(LowLevelLogger log: low_level_logs)
            log.check_state();
         
         // we need to remove all defunct links that are still in an off-line state
         for(Short neighbour_address: defunct_links)
         {
            Link link = links.get(neighbour_address);
            if(link != null && link.is_off_line())
               links.remove(neighbour_address);
         }
         defunct_links.clear();
         
         // we also need to scan through the queue of unsent messages.  if there are any messages 
         // waiting for which a link does not exist, we will need to start that link
         for(Packet message: unsent_messages)
         {
            Link link = links.get(message.neighbour_dest_address);
            if(link == null)
            {
               link = new Link(this, message.neighbour_dest_address);
               links.put(message.neighbour_dest_address, link);
               link.on_message_ready();
            }
         }
      }
      return links.size();
   } // check_state


   /**
    * This method invokes check_state(false)
    *
    * @return the number of links that are currently active
    */
   public int check_state() throws Exception
   { return check_state(false); } 


   /**
    * Adds the specified station to the list managed for this network.
    *
    * @param station  Specifies the station device that is to be added. 
    */
   public void add_station(
      Datalogger station)
   {
      synchronized(stations)
      {
         station.set_network(this);
         stations.put(
            station.get_pakbus_address(),
            station);
      }
   } // add_station


   /**
    * Looks up the associated station address.
    *
    * @return the datalogger object given the address or null.
    */
   public Datalogger get_station(short address)
   { return stations.get(address); }


   /**
    * Removes the station identified by the specified address from the list
    * managed by this network.
    *
    * @param station_pakbus_address  Specifies the station's PakBus address
    */
   public void remove_station(
      short station_pakbus_address)
   { 
      synchronized(stations)
      {
         Datalogger station = stations.get(station_pakbus_address);
         if(station != null)
         {
            stations.remove(station_pakbus_address);
            for(TransactionBase tran: station.transactions.values())
               on_transaction_close(tran);
         }
      }
   } // remove_station
   

   /**
    * Calculates the number of messages that are waiting to be sent for the specified address.  If
    * the specified address is zero, the count of all waiting messages will be returned.
    * 
    *  @return The number of waiting messages 
    */
   public int waiting_to_send_count(short neighbour_address)
   {
      int rtn = 0;
      if(pakbus_address == 0)
         rtn = unsent_messages.size();
      else
      {
         for(Packet message: unsent_messages)
         {
            if(message.neighbour_dest_address == neighbour_address)
               ++rtn;
         }
      }
      return rtn;
   }  // waiting_to_send_count


   /**
    * Adds a low level logger to the list of objects monitoring low level I/O.
    *
    * @param logger  An object that implements the LowLevelLogger interface.
    */
   public void add_low_level_logger(LowLevelLogger logger)
   { 
      low_level_logs.add(logger);
      logger.on_comment("\nLogging started: " + BuildInfo.version + "\n");
   } // add_low_level_logger


   /**
    * Removes the specified low level logger from the list associated with this network.
    *
    * @param logger  The logger meant to be removed.
    */
   public void remove_low_level_logger(LowLevelLogger logger)
   { low_level_logs.remove(logger); }
   
   
   /**
    * Returns the total number of messages (not including link state packets) that have been 
    * sent since the network was initialised.
    * 
    * @return The total number of messages that have been sent
    */
   public int get_comms_attempts()
   { return comms_attempts; }
   
   
   /**
    * Returns the total number of retry attempts that have taken place
    * since the network was last initialised.
    * 
    * @return The total number of retry attempts.
    */
   public int get_comms_retries()
   { return comms_retries; }
   
   
   /**
    * Returns the total number of failures that have taken place since 
    * the network was last initialised.
    * 
    * @return the total number of failures.
    */
   public int get_comms_failures()
   { return comms_failures; }
   
   /**
    * Adds a comment to all of the low level logs
    * 
    * @param comment  The comment string to be added
    */
   public void add_comment(String comment)
   {
      for(LowLevelLogger log: low_level_logs)
         log.on_comment(comment);
   } // add_comment


   /**
    * Returns true if this network is configured to allow the use of the "unquoted" low level
    * protocol.
    */
   public boolean get_allow_unquoted()
   { return allow_unquoted; }


   /**
    * Can be called to configure this network to allow the "unquoted" low level protocol to
    * be negotiated and used.
    *
    * @param allow_unquoted_  Set to true if the "unquoted" protocol is to be allowed. 
    */
   public void set_allow_unquoted(boolean allow_unquoted_)
   { allow_unquoted = allow_unquoted_; }
   
   
   /** 
    * Returns the verify interval that this network will in hello commands and hello responses. 
    */
   public int get_reported_verify_interval()
   { return reported_verify_interval; }
   
   
   /** 
    * Can be called to set the verify interval that this network will report in hello
    * commands and hello responses.  By default, this value will be set to 0xFFFE which is a 
    * special code recognised by many PakBus implementations that the neighbour record should
    * be deleted as soon as the link goes to an off-line state. 
    * 
    * @param reported_verify_interval_ Specifies the interval in seconds, that this network
    * should report in hello commands and responses.
    */
   public void set_reported_verify_interval(int reported_verify_interval_)
   { reported_verify_interval = reported_verify_interval_; }
   
   
   /**
    * Called when a transaction object is being closed down by the datalogger
    * 
    * @param transaction
    */
   protected void on_transaction_close(TransactionBase transaction)
   {
      // we need to make sure that this transaction is removed from the
      // focus queue
      int i = 0; 
      while(i < focus_queue.size())
      {
         TransactionBase waiting = focus_queue.get(i);
         if(waiting == transaction)
            focus_queue.remove(i);
         else
            ++i;
      }
      if(current_focus == transaction)
         current_focus = null;
      
      // we also need to update the comms and retries attempts
      comms_retries += transaction.total_retries;
      comms_attempts += transaction.total_messages_sent;
      comms_failures += transaction.total_failures;
   } // on_transaction_close
   
   
   /**
    * @return The transaction that currently has focus
    */
   public TransactionBase get_focus()
   { return current_focus; }

   
   /**
    * Posts the specified message to the end of the message queue.  Will also
    * inform the link object that the message is ready to send.
    *
    * @param message  The message object to be queued.
    */
   protected void post_message(Packet message) throws Exception
   {
      // add the message to the queue
      message.source_address = pakbus_address;
      message.neighbour_source_address = pakbus_address;
      unsent_messages.add(message);
      
      // we now need to look up or create the link to carry the message.
      Link link = links.get(message.neighbour_dest_address);
      if(link == null)
      {
         link = new Link(this,message.neighbour_dest_address);
         links.put(message.neighbour_dest_address, link);
      }
      link.on_message_ready();
   } // post_message

    
   /**
    * Called by a link when a complete message has arrived that needs to either
    * be processed by the network or relayed on to the application.
    */
   protected void on_message_received(Packet message) throws Exception
   {
      boolean send_to_station = true;
      if(message.protocol_type == Packet.protocol_pakctrl)
      {
         send_to_station = false;
         switch((short)message.message_type)
         {
         case Packet.pakctrl_delivery_failure:
            on_delivery_failure(message);
            break;
            
         case Packet.pakctrl_hello_cmd:
            on_hello_cmd(message);
            break;
            
         case Packet.pakctrl_hello_ack:
            on_hello_ack(message);
            break;
            
         case Packet.pakctrl_hello_req:
            on_hello_req(message);
            break;
            
         case Packet.pakctrl_echo_cmd:
            on_echo_cmd(message);
            break;
            
         default:
            send_to_station = true;
            break;   
         }
      }
      synchronized(stations)
      {
         if(send_to_station)
         {
            Datalogger station = stations.get(message.source_address);
            if(station != null)
               station.on_message_received(message);
         }
      }
      
      // we might need to use this message to reset the verification timer for the 
      // associated neighbour
      Neighbour neighbour = neighbours.get(message.neighbour_source_address);
      if(neighbour == null)
      {
         neighbour = new Neighbour(message.neighbour_source_address,this);
         neighbours.put(message.neighbour_source_address, neighbour);
      }
      if(neighbour.verify_timer != null)
         neighbour.verify_timer.reset();
   } // on_message_received
   
   
   /**
    * Called to transmit a packet.  This should be generally left to be called
    * by the link.
    */
   protected void send_packet(Packet packet) throws Exception
   {
      // we need to ensure that packet's source address fields are filled in
      packet.source_address = pakbus_address;
      packet.neighbour_source_address = pakbus_address;
      
      try
      {
         if(packet.sub_protocol == Packet.sub_control ||
            packet.sub_protocol == Packet.sub_link_state)
         {
            // we can now stream the packet to an array of bytes which can then be quoted and sent
            // to the output stream
            byte[] serial_packet = packet.write_serial_packet();
            low_level_write(LowLevelDecoder.synch_byte);
            for(int i = 0; i < serial_packet.length; ++i)
            {
               int ch = (serial_packet[i] & 0x000000ff);
               if(ch == LowLevelDecoder.synch_byte ||
                  ch == LowLevelDecoder.quote_byte)
               {
                  low_level_write(LowLevelDecoder.quote_byte);
                  low_level_write(ch + 0x20);
               }
            else
               low_level_write(ch);
            }
            
            // we need to output the quoted version of the frame signature as well
            int sig_null = Utils.calc_sig_nullifier(
               Utils.calc_sig(
                  serial_packet,
                  serial_packet.length));
            int sig_byte1 = (sig_null & 0xFF00) >> 8;
            int sig_byte2 = (sig_null & 0x00FF);
            if(sig_byte1 == LowLevelDecoder.quote_byte ||
               sig_byte1 == LowLevelDecoder.synch_byte)
            {
               low_level_write(LowLevelDecoder.quote_byte);
               low_level_write(sig_byte1 + 0x20);
            }
            else
               low_level_write(sig_byte1);
            if(sig_byte2 == LowLevelDecoder.quote_byte ||
               sig_byte2 == LowLevelDecoder.synch_byte)
            {
               low_level_write(LowLevelDecoder.quote_byte);
               low_level_write(sig_byte2 + 0x20);
            }
            else
               low_level_write(sig_byte2);
            low_level_write(LowLevelDecoder.synch_byte);
         }
         else if(packet.sub_protocol == Packet.sub_unquoted)
         {
            byte[] unquoted_packet = packet.write_unquoted_packet();
            low_level_write(LowLevelDecoder.synch_byte);
            low_level_write(0xf0);
            for(int i = 0; i < unquoted_packet.length; ++i)
            {
               int ch = (unquoted_packet[i] & 0x000000ff);
               low_level_write(ch);
            }
            low_level_write(LowLevelDecoder.synch_byte);
         }
      }
      catch(IOException e)
      { 
         on_link_failure((short)0);
         throw e;
      }
   } // send_packet
   
   
   /**
    * Called when a link object has made the transistion to an off-line state.
    */
   protected void on_link_offline(short neighbour_address)
   {
      defunct_links.add(neighbour_address);
   } // on_link_offline
   
   
   /**
    * Called when a link failure has occurred.  The address will be zero if the
    * link failure is general or will indicate a neighbour address otherwise.
    * This method will report failures on all transactions that depend upon the
    * link and will also remove the link object.
    * 
    * @param neighbour_address Specifies the address of the neighbour that
    * failed or zero for a more catastrophic failure.
    */
   protected void on_link_failure(short neighbour_address) throws Exception
   {
      // if the neighbour address is zero, then a low level error has occurred and
      // communications are no longer viable for this network.  
      if(neighbour_address == 0)
      {
         // clear out the i/o streams
         input = null;
         output = null;
         unsent_messages.clear();
         links.clear();
         
         // propogate the error to all stations and erase the neighbours
         neighbours.clear();
         current_verify = null;
         synchronized(stations)
         {
            for(Datalogger station: stations.values())
               station.on_link_failure(neighbour_address);
         }
      }
      else
      {
         // we need to clear out the neighbour
         if(current_verify != null && current_verify.neighbour_address == neighbour_address)
         {
            neighbours.remove(neighbour_address);
            current_verify = null;
         }
         
         // we need to clear out any remaining messages that rely on this link address
         int i = 0;
         while(i < unsent_messages.size())
         {
            Packet message = unsent_messages.get(i);
            if(message.neighbour_dest_address == neighbour_address)
               unsent_messages.remove(i);
            else
               ++i;
         }
         
         // we also need to notify any stations that use this link of the failure
         synchronized(stations)
         {
            for(Datalogger station: stations.values())
            {
               if(station.get_neighbour_address() == neighbour_address)
                  station.on_link_failure(neighbour_address);
            }
         }
         
         // this link also needs to be removed.  We will mark it to be removed on the next state check  
         defunct_links.add(neighbour_address);
      }
   } // on_link_failure


   /**
    * Called when a link is in a state to send a message.  The message with the
    * highest priority and that has been waiting for the longest time that uses
    * the specified neighbour address should be returned first.  If there is no
    * such message, a null object should be returned.
    *
    * The message object that is returned will have its expect more code and
    * address fields filled in when this method returns.  
    *
    * @param neighbour_address   Specifies the route the message should take
    * @return The message with the highest priority
    */
   protected Packet get_next_out_message(short neighbour_address)
   {
      // we need to choose a message from those not yet sent.
      Packet rtn = null;
      for(Packet message: unsent_messages)
      {
         if(message.neighbour_dest_address == neighbour_address)
         {
            if(rtn == null || message.priority > rtn.priority)
               rtn = message;
         }
      }
      
      // we will inform the owning station that the message is being sent
      if(rtn != null)
      {
         if(rtn.message_type == Packet.pakctrl_hello_cmd && current_verify != null)
         {
            current_verify.elapsed_timer = new Timer();
         }
         else
         {
            synchronized(stations)
            {
               Datalogger station = stations.get(rtn.dest_address);
               if(station != null)
                  station.on_message_being_sent(rtn);
            }
         }
         unsent_messages.remove(rtn);
      }
      return rtn;
   } // get_next_out_message

   
   /**
    * Handles a delivery failure
    * 
    * @param message the delivery failure message
    */
   private void on_delivery_failure(Packet message)
   {
      synchronized(stations)
      {
         try
         {
            byte reason = message.read_byte();
            int word1 = message.read_uint2();
            int word2 = message.read_uint2();
            byte protocol = (byte)((word1 & 0xf000) >> 12);
            short dest_address = (short)(word1 & 0x0fff);
            short source_address = (short)(word2 & 0x0fff);
            
            if(source_address == pakbus_address && message.whats_left() >= 4)
            {
               Datalogger station = stations.get(dest_address);
               byte message_type = message.read_byte();
               byte tran_no = message.read_byte();
               
               if(station != null)
                  station.on_delivery_failure(reason,protocol,message_type,tran_no);
            }
         }
         catch(Exception e)
         { }
      }
   } // on_delivery_failure
   
   
   /**
    * Handles an incoming echo command
    * 
    * @param message  the echo command contents
    */
   private void on_echo_cmd(Packet message) throws Exception
   {
      Packet reply = new Packet();
      int body_len = message.whats_left() - 8;
      
      reply.dest_address = message.source_address;
      reply.neighbour_dest_address = message.source_address;
      reply.tran_no = message.tran_no;
      reply.message_type = (byte)Packet.pakctrl_echo_ack;
      reply.add_nsec(LoggerDate.system());
      
      try
      {
         if(body_len > 0)
         {
            // we need to add the contents of the command to the reply
            message.move_past(8);
            if(message.whats_left() > 0)
               reply.add_bytes(message.read_bytes(body_len), body_len);
         }
      }
      catch(Exception e)
      { }
      post_message(reply);
   } // on_echo_cmd
   
   
   /**
    * Handles an incoming hello command
    * 
    * @param message  the hello command contents
    */
   private void on_hello_cmd(Packet message) throws Exception
   {
      try
      {
         // we will set the interval at which the API performs verification to be the least of our own setting
         // and that reported by the station.  If the station reports an "inifinite" value, we will bottom it
         // out at five minutes. 
         message.move_past(2);
         int verify_interval = message.read_uint2();

         if(verify_interval == 0 || verify_interval >= 0xFFFE)
            verify_interval = 300; // set default to 5 minutes
         if(reported_verify_interval != 0 && 
            reported_verify_interval < verify_interval)
            verify_interval = reported_verify_interval;
         
         // we can compose a reply to the hello command
         Packet reply = new Packet();
         HopMetric my_metric = new HopMetric(get_link_delay());
         
         reply.dest_address = message.source_address;
         reply.neighbour_dest_address = message.neighbour_source_address;
         reply.tran_no = message.tran_no;
         reply.message_type = (byte)Packet.pakctrl_hello_ack;
         reply.protocol_type = Packet.protocol_pakctrl;
         reply.add_bool(false);    // not a router
         reply.add_byte(my_metric.get_coded_value());
         // for dialed links, we want the datalogger to remove our neighbour record as soon as we
         // close the link.  A verification interval of 0xfffe does just that. 
         reply.add_uint2(reported_verify_interval);
         post_message(reply);
         
         // we can also update (or create) the neighbour record
         Neighbour neighbour = neighbours.get(message.neighbour_source_address);
         if(neighbour == null)
         {
            neighbour = new Neighbour(message.neighbour_source_address,this);
            neighbours.put(message.neighbour_source_address, neighbour);
         }
         neighbour.verify_timer = new Timer();
         neighbour.verification_interval = verify_interval * 1000; // convert secs to msecs
         add_comment("verify interval for " + neighbour.neighbour_address + " is " + verify_interval + " seconds");

         // this command should also satisfy our own need for verification as well
         if(current_verify != null &&
            current_verify.neighbour_address == neighbour.neighbour_address)
         {
            current_verify = null;
            set_next_focus();
         }
      }
      catch(Exception e)
      { } 
   } // on_hello_cmd


   /**
    * Handles an incoming hello response
    *
    * @param message The response message
    */
   private void on_hello_ack(Packet message)
   {
      try
      {
         // we will set the interval at which the API performs verification to be the least of our own setting
         // and that reported by the station.  If the station reports an "inifinite" value, we will bottom it
         // out at five minutes. 
         message.move_past(2);
         int verify_interval = message.read_uint2();

         if(verify_interval <= 0 || verify_interval >= 0xfffe)
            verify_interval = 300; // default of five minutes
         if(reported_verify_interval > 0 &&
            reported_verify_interval < verify_interval)
            verify_interval = reported_verify_interval;
         
         // we can also update (or create) the neighbour record
         Neighbour neighbour = neighbours.get(message.neighbour_source_address);
         if(neighbour == null)
         {
            neighbour = new Neighbour(message.neighbour_source_address,this);
            neighbours.put(message.neighbour_source_address, neighbour);
         }
         neighbour.verify_timer = new Timer();
         neighbour.verification_interval = verify_interval * 1000; // convert secs to msecs
         add_comment("verify interval for " + neighbour.neighbour_address + " is " + verify_interval + " seconds");

         // this response should also satisfy our own need for verification as well
         if(current_verify != null &&
            current_verify.neighbour_address == neighbour.neighbour_address)
         {
            current_verify = null;
            set_next_focus();
         }
      }
      catch(Exception e)
      { }
   } // on_hello_ack
   
   
   /**
    * Handles an incoming hello request
    * 
    * @param message  the hello request contents
    */
   private void on_hello_req(Packet message)
   {
      // look up the neighbour
      Neighbour neighbour = neighbours.get(message.neighbour_source_address);
      if(neighbour == null)
      {
         neighbour = new Neighbour(message.neighbour_source_address, this);
         neighbours.put(message.neighbour_source_address, neighbour);
      }
      
      // we need to make sure that the neighbour will be verified
      neighbour.verify_timer = null;
      if(message.neighbour_dest_address == broadcast_address &&
         neighbour.delay_timer == null)
      {
         neighbour.delay_timer = new Timer();
         neighbour.delay_timeout = random.nextInt(15000);;
      }
   } // on_hello_req


   private void check_incoming() throws Exception
   {
      try
      {
         while(input.available() > 0)
         {
            Packet packet = decoder.decode(input,low_level_logs);
            if(packet == null ||
               !(packet.neighbour_dest_address == broadcast_address ||
                 packet.neighbour_dest_address == pakbus_address))
               continue;
            if(allow_unquoted && packet.sub_protocol == Packet.sub_control)
            {
               if(packet.supports_unquoted())
               {
                  Link link = links.get(packet.neighbour_source_address);
                  if(link == null)
                  {
                     link = new Link(this,packet.neighbour_source_address);
                     links.put(packet.neighbour_source_address, link);
                  }
                  link.set_sub_protocol(Packet.sub_unquoted);
               }
               else if(packet.control_type == Packet.control_ring)
                  packet.sub_protocol = Packet.sub_link_state;
            }
            if(packet.sub_protocol == Packet.sub_link_state ||
               packet.sub_protocol == Packet.sub_unquoted)
            {
               // we need to look up or create the link
               Link link = links.get(packet.neighbour_source_address);
               if(link == null)
               {
                  link = new Link(this,packet.neighbour_source_address);
                  links.put(
                     packet.neighbour_source_address,
                     link);
               }
               link.process_incoming_frame(packet);
            }
         }
      }
      catch(IOException e)
      { 
         on_link_failure((short)0);
         throw e;
      }
   } // check_incoming
   
   
   /**
    * Places the specified transaction on the focus queue
    */
   protected void request_focus(TransactionBase transaction) throws Exception
   {
      focus_queue.add(transaction);
      if(current_focus == null)
         set_next_focus();
   } // request_focus
   
   
   
   /** 
    * Release focus for the current transaction(if that is the one specified) or removes the transaction from the focus queue.
    */
   protected void release_focus(TransactionBase transaction)  throws Exception
   {
      if(transaction == current_focus)
         current_focus = null;
      else
         focus_queue.remove(transaction);
      set_next_focus();
   } // release_focus
   
   
   /**
    * Prepares the set the next focus
    */
   private void set_next_focus() throws Exception
   {
      if(current_focus == null && current_verify == null)
      {
         // we need to iterate the focus queue to find the highest priority transaction that has been waiting the longest time
         for(TransactionBase transaction: focus_queue)
         {
            if(current_focus == null || transaction.priority > current_focus.priority)
               current_focus = transaction;
         }

         if(current_focus != null)
         {
            // we've found the next candidate but we need to make make sure
            // that the neighbour link is verified before focus is awarded.
            Neighbour neighbour = neighbours.get(current_focus.get_neighbour_address());
            if(neighbour == null)
            {
               neighbour = new Neighbour(current_focus.get_neighbour_address(), this);
               neighbours.put(
                  current_focus.get_neighbour_address(),
                  neighbour);
            }
            // if the neighbour has not been verified, we need to first verify
            // it before we can allow the focus to take place.  
            if(neighbour.verify_timer == null)
            {
               current_focus = null;
               current_verify = neighbour;
               neighbour.start_verify();
            }
            else
            {
               focus_queue.remove(current_focus);
               current_focus.on_focus_start();
            }
         }
      }
   } // set_next_focus


   /**
    * Writes the specified byte and sends it to all loggers
    */
   private void low_level_write(int value) throws Exception
   {
      output.write(value);
      for(LowLevelLogger logger: low_level_logs)
         logger.on_io(value,true);
   } // low_level_write
   
   
   /**
    * This member stores the address that will be used to identify all outgoing
    * messages and also will be used to filter all incoming messages.
    */
   private short pakbus_address;
   
   
   /**
    * Provides the input for this network.
    */
   private InputStream input;


   /**
    * Provides the output for this network
    */
   private OutputStream output;
   
   
   /**
    * Used to decode packets as they are read from the input stream
    */
   private LowLevelDecoder decoder;
   
   
   /**
    * Maintains the list of active links keyed by their neighbour addresses.
    */
   private Map<Short, Link> links;
   
   
   /**
    * Keeps track of link addresses that have gone off-line
    */
   private List<Short> defunct_links;


   /**
    * Keeps track of the set of datalogger objects registered with this
    * network.  Note that the map is keyed by the datalogger PakBus address.
    */
   private Map<Short, Datalogger> stations;


   /**
    * Holds the list of messages that are waiting to be sent.
    */
   private List<Packet> unsent_messages;
   
   
   /**
    * Holds the list of transactions that are waiting for focus.
    */
   private List<TransactionBase> focus_queue;
   
   
   /**
    * Holds a reference to the transaction that currently has focus
    */
   private TransactionBase current_focus;


   /**
    * Holds the list of low level monitoring objects.
    */
   private List<LowLevelLogger> low_level_logs;


   /**
    * Keeps track of known neighbours keyed by their address.
    */
   private Map<Short, Neighbour> neighbours;


   /**
    * Keeps track of the neighbour that the router is trying to verify at this
    * time.  Will be null if there is no such neighbour.  If this value is
    * non-null, transaction focus will not be allowed until it is cleared.
    */
   private Neighbour current_verify;
   
   
   /**
    * Keeps track of the total number of transaction level messages have been sent.
    * This does not include the messages used to support the LinkState protocol.
    */
   protected int comms_attempts;
   
   /**
    * Keeps track of the total number of transaction level messages that
    * were sent as retries. 
    */
   protected int comms_retries;
   
   /**
    * Keeps track of the total number of transactions that have failed.
    */
   protected int comms_failures;
   
   /**
    * Used to generate random retries for hello messages
    */
   protected Random random;
   
   /**
    * Controls whether the "unquoted" version of the low level protocol should be supported.  
    * Some released operating systems (cr1000.std.15) appear to have bugs associated handling 
    * large packets when the unquoted protocol is used.  
    * 
    *  The "unquoted" protocol is advantageous when using PakBus/TCP because it takes less time
    *  for the datalogger to form the packet (no special formatting of packet contents is needed
    *  and no packet signature is needed either).
    */
   protected boolean allow_unquoted;
   
   /**
    * Controls the verify interval, in seconds that will be used when sending a hello command or a hello response
    * to a neighhbour.  The default value, 65534, is a special code recognised by the CR1000 and similar
    * loggers that forces the logger to remove the neighbour entry as soon as the link goes off-line.
    */
   protected int reported_verify_interval;
}
  

