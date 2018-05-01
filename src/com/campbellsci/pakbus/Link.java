/* Link.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 28 September 2006
   Last Change: Thursday 22 May 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;



/**
 * Defines a session with a PakBus neighbour using the serial packet protocol.  By keeping this
 * state in a separate class, the Network object can simultaneously manage several concurrent PakBus
 * links on the same media (as would happen on a radio or MD485 type link).
 * 
 * @author jon
 *
 */
final class Link
{
   /**
    * constructor for this class
    */
   public Link(
      Network network_,
      short neighbour_address_)
   {
      network = network_;
      neighbour_address = neighbour_address_;
      expect_more_addresses = new LinkedList<ExpectMoreEntry>();
      watch_dog = new Timer();
      sub_protocol = Packet.sub_link_state;
      link_state = link_state_offline;
   } // constructor


   /**
    * Checks expect more addresses as well as our own watchdog.  It is assumed
    * that this method will be invoked by the Network method of the same name.
    */
   public void check_status(boolean close) throws Exception
   {
      if(ring_timer != null)
         send_ring(false);
      if(before_finish_timer != null)
         send_finished();
      if(sub_protocol != Packet.sub_unquoted && watch_dog.elapsed() > 40000)
         link_state = link_state_offline;
      if(sub_protocol != Packet.sub_unquoted && 
         link_state == link_state_finished && 
         watch_dog.elapsed() > 5000)
         link_state = link_state_offline;
      if(sub_protocol == Packet.sub_unquoted && close)
         link_state = link_state_offline;
      if(link_state == link_state_offline)
         network.on_link_offline(neighbour_address);
   } // check_status
   
   
   /**
    * Updates the internal state of this link based upo the contents of the
    * incoming message.  Fields that will be updated include the link state as
    * well as the expect more lists.
    */
   public void process_incoming_frame(Packet frame) throws Exception
   {
      // if this frame is a full message, we will need to send it to the
      // network to be processed.
      if(frame.sub_protocol == Packet.sub_unquoted)
         link_state = link_state_ready;
      if(!frame.short_header)
      {
         if(frame.dest_address == Network.broadcast_address)
            frame.dest_address = network.get_pakbus_address();
         update_expect_more(
            frame.source_address,
            frame.dest_address,
            frame.expect_more_code);
         network.on_message_received(frame);
      }

      // we also need to adjust our own link state based upon what was received
      if(frame.neighbour_dest_address != Network.broadcast_address &&
         frame.sub_protocol != Packet.sub_unquoted)
      {
         examine_link_state(frame.link_state);
         if(link_state != link_state_offline)
            watch_dog.reset();
      }
      else if(frame.sub_protocol == Packet.sub_unquoted)
         link_state = link_state_ready;
   } // process_incoming_frame

   
   /**
    * Called when a message has been queued that will involve this link.
    */
   public void on_message_ready() throws Exception
   {
      if(link_state == link_state_ready)
         on_ready_to_send(false);
      else if(link_state == link_state_offline)
         send_ring(true);
   } // on_message_ready
   

   /**
    * Returns the number of queued messages that can be sent through this
    * link.
    */
   public int waiting_to_send_count()
   { return network.waiting_to_send_count(neighbour_address); }
      
   
   /**
    * Called when the link is in a state where it is ready to send.
    */
   public void on_ready_to_send(boolean send_if_ringing) throws Exception
   {
      boolean ok_to_send = true;
      if(waiting_to_send_count() == 0)
         ok_to_send = false;
      else if(link_state != link_state_ready &&
              !(link_state == link_state_ringing && send_if_ringing))
      {
         ok_to_send = false;
         if(link_state != link_state_finished && ring_timer == null)
            send_ring(true);
      }
      if(ok_to_send)
      {
         Packet message = network.get_next_out_message(neighbour_address);
         ring_timer = null;
         if(message != null)
         {
            byte reported_link_state = Packet.link_ready;
            update_expect_more(
               message.source_address,
               message.dest_address,
               message.expect_more_code);
            if(should_keep_link())
               link_state = link_state_ready;
            else
            {
               reported_link_state = Packet.link_finished;
               link_state = link_state_finished; 
            }
            send_serial_packet(message,reported_link_state);
         }
      } 
   } // on_ready_to_send
   

   /**
    * Returns true if the link should be maintained.
    */
   public boolean should_keep_link()
   {
      boolean rtn = (waiting_to_send_count() > 0 || sub_protocol == Packet.sub_unquoted);
      int i = 0;

      while(!rtn && i < expect_more_addresses.size())
      {
         ExpectMoreEntry entry = expect_more_addresses.get(i);
         if(entry.age.elapsed() > Network.link_timeout)
            expect_more_addresses.remove(entry);
         else
         {
            ++i;
            rtn = true;
         }
      } 
      return rtn;
   } // should_keep_link
   
   
   /**
    * Returns true if this link is in an off-line state
    */
   public boolean is_off_line()
   { return link_state == link_state_offline; }
   
   
   /**
    * Sets the sub-protocol that this link should use.  This is done based upon the
    * network having received capabilities associated with this link.
    * 
    * @param sub_protocol_  Specifies the new sub-protocol for this link
    */
   public void set_sub_protocol(byte sub_protocol_)
   { sub_protocol = sub_protocol_; }
   
   
   /**
    * Responsible for transmitting a message.  Will fill in the link
    * destination address for the neighbour destination
    */
   private void send_serial_packet(
      Packet packet,
      byte send_link_state) throws Exception
   {
      packet.neighbour_dest_address = neighbour_address;
      packet.link_state = send_link_state;
      packet.sub_protocol = sub_protocol;
      network.send_packet(packet);
   } // send_serial_packet

   
   /**
    * Responsible for determining our own link state based upon the reported
    * link state.
    */
   private void examine_link_state(byte peer_link_state) throws Exception
   {
      switch(peer_link_state)
      {
      case Packet.link_off_line:
         link_state = link_state_offline;
         network.on_link_offline(neighbour_address);
         break;
         
      case Packet.link_ring:
         if(link_state == link_state_offline ||
            link_state == link_state_ringing ||
            link_state == link_state_finished ||
            link_state == link_state_ready)
         {
            link_state = link_state_ready;
            if(waiting_to_send_count() == 0)
            {
               Packet empty = new Packet();
               empty.short_header = true;
               send_serial_packet(empty,Packet.link_ready);
            }
            else
               on_ready_to_send(false); 
         }
         break;

      case Packet.link_ready:
         if(link_state == link_state_ready && !should_keep_link())
            send_finished();
         else if(
            link_state == link_state_offline ||
            link_state == link_state_ringing ||
            link_state == link_state_finished)
         {
            link_state = link_state_ready;
            on_ready_to_send(false);
         }
         break;

      case Packet.link_finished:
         if(should_keep_link())
         {
            if(waiting_to_send_count() > 0)
            {
               link_state = link_state_ringing;
               ringing_retry_count = 0;
               on_ready_to_send(true);
            }
            else
            {
               ringing_retry_count = 0;
               send_ring(false);
            }
         }
         else
         {
            Packet empty = new Packet();
            empty.short_header = true;
            empty.neighbour_dest_address = neighbour_address;
            empty.link_state = Packet.link_off_line;
            network.send_packet(empty);
            link_state = link_state_offline;
            network.on_link_offline(neighbour_address);
         }
         break;

      case Packet.link_pause:
         if(link_state != link_state_finished)
         {
            Packet empty = new Packet();
            empty.short_header = true;
            send_serial_packet(empty,Packet.link_finished);
            link_state = link_state_finished;
         }
         else if(link_state == link_state_offline)
         {
            Packet empty = new Packet();
            empty.short_header = true;
            send_serial_packet(empty,Packet.link_off_line);
         }
         break;
      }
   } // examine_link_state


   /**
    * Responsible for maintaining the state of the expect more lists as
    * messages are sent and received.
    */
   private void update_expect_more(
      short source_address,
      short dest_address,
      byte expect_more_code)
   {
      if(source_address != Network.broadcast_address &&
         dest_address != Network.broadcast_address)
      {
         // we need to locate the expect more member, if any.  The entry_index
         // value will specify the index if found or will be equal to the size
         // of the container
         int entry_index = 0;
         short address1 = (expect_more_code == Packet.expect_reverse ? dest_address : source_address);
         short address2 = (expect_more_code == Packet.expect_reverse ? source_address : dest_address);
         while(entry_index < expect_more_addresses.size())
         {
            ExpectMoreEntry entry = expect_more_addresses.get(entry_index);
            
            if(entry.source_address == address1 &&
               entry.dest_address == address2)
               break;
            else
               ++entry_index;
         }

         // we can now process the code
         if(expect_more_code == Packet.expect_more ||
            expect_more_code == Packet.expect_reverse)
         {
            if(entry_index == expect_more_addresses.size())
               expect_more_addresses.add(
                  entry_index,
                  new ExpectMoreEntry(
                     address1,
                     address2));
            else
               expect_more_addresses.get(entry_index).age.reset();
         }
         else if(expect_more_code == Packet.expect_last)
         {
            if(entry_index < expect_more_addresses.size())
               expect_more_addresses.remove(
                  expect_more_addresses.get(
                     entry_index));
         } 
      }
   } // update_expect_more


   /**
    * Forms a ring request
    */
   private void send_ring(boolean first_ring) throws Exception
   {
      if(sub_protocol != Packet.sub_unquoted)
      {
         int ring_timeout = Math.min(
            2000,
            Math.max(
               600,
               network.get_link_delay()));
         if(first_ring || ring_timer == null)
         {
            ring_timer = new Timer();
            ringing_retry_count = 0;
         }
         if((first_ring || ring_timer.elapsed() >= ring_timeout) && ringing_retry_count++ <= 4)
         {
            // send the ring packet
            Packet empty = new Packet();
         
            link_state = link_state_ringing;
            empty.capabilities = 0x09; // indicate support for both link state and unquoted protocols
            empty.short_header = true;
            send_serial_packet(empty,Packet.link_ring);
            ring_timer = new Timer();
         }
         else if(ringing_retry_count > 4)
         {
            link_state = link_state_offline;
            network.on_link_failure(neighbour_address);
         }
      }
      else
      {
         ring_timer = null;
         link_state = link_state_ready;
         on_ready_to_send(true);
      }
   } // send_ring


   /**
    * Either sends a finish message or sets up the conditions to send one in
    * the future.
    */
   private void send_finished() throws Exception
   {
      if(sub_protocol != Packet.sub_unquoted)
      {
         link_state = link_state_finished;
         if(before_finish_timer == null)
            before_finish_timer = new Timer();
         else if(before_finish_timer.elapsed() >= 1000)
         {
            Packet empty = new Packet();
            empty.short_header = true;
            before_finish_timer = null;
            send_serial_packet(empty,Packet.link_finished);
         }
      }
   } // send_finished

   
   /**
    * Specifies the network that owns this link
    */
   private Network network;
   
   /**
    * Specifies the remote PakBus address for this link
    */
   short neighbour_address;

   // the following constants define the internal values of the link_state
   // member.
   private static final byte link_state_offline = 0;
   private static final byte link_state_ringing = 1;
   private static final byte link_state_ready = 2;
   private static final byte link_state_finished = 3;

   /**
    * Specifies the current state of this link.
    */
   private int link_state;
   

   /**
    * Defines the ExpectMore structure
    */
   private class ExpectMoreEntry
   {
      short source_address;
      short dest_address;
      Timer age;

      public ExpectMoreEntry(
         short source_address_,
         short dest_address_)
      {
         source_address = source_address_;
         dest_address = dest_address_;
         age = new Timer();
      } // constructor
   }

   
   /**
    * Specifies the sub-protocol that should be used with this link
    */
   private byte sub_protocol;

   /**
    * Specifies the list of sessions currently being supported by this link.
    */
   List<ExpectMoreEntry> expect_more_addresses;


   /**
    * Measures the amount of time that has passed since this link last received any message.
    */
   Timer watch_dog;


   /**
    * Keeps track of the number of ring attempts that have been sent
    */
   int ringing_retry_count;

   
   /**
    * Used to space out ring attempts over a given interval
    */
   Timer ring_timer;


   /**
    * Used to delay the sending of the finish packet
    */
   Timer before_finish_timer;
}
