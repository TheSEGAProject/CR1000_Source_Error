/* UserIoTran.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 02 November 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.IOException;

/**
 * Defines a transaction object that allows an application to send
 * or receive user I/O packets to and from the datalogger
 */
public class UserIoTran extends TransactionBase
{
   /**
    * Constructor
    * 
    * @param client_  The application provided object that will receive
    * event notifications associated with this transaction
    */
   public UserIoTran(UserIoClient client_)
   {
      client = client_;
      can_send = false;
      buff = new byte[1024];
      send_buff = new Packet();
   } // constructor
   
   
   @Override
   public void on_focus_start() throws Exception
   {
      can_send = true;
      client.on_started(this);
   } // on_focus_start
   
   
   /**
    * Encapsulates the provided data into packets sent to datalogger
    * as user i/o packets.  
    * 
    * @param buff  Specifies the data to send
    * @param buff_len  specifies the number of bytes to send.  Note that if this
    * value exceeds the packet size limit for the station, the data will be broken 
    * up into multiple packets.
    */
   public void send_data(byte[] buff, int buff_len) throws Exception
   {
      send_buff.add_bytes(buff,buff_len);
      if(can_send)
         send_next_data();
   } // send_data

   
   @Override
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5)
         {
            if(message.message_type == Packet.bmp5_user_io_cmd)
            {
               message.move_past(2);
               int buff_len = message.whats_left();
               message.read_bytes(buff,buff_len);
               client.on_bytes_received(this, buff, buff_len);
            }
            else if(message.message_type == Packet.bmp5_user_io_ack)
            {
               int response_code = message.read_byte();
               if(response_code == 0)
               {
                  int buff_len = message.whats_left();
                  message.read_bytes(buff,buff_len);
                  client.on_bytes_received(this, buff, buff_len);
                  reset_watchdog();
                  can_send = true;
                  send_next_data();
               }
               else
                  on_failure(failure_permission_denied);
            }
            
         }
      }
      catch(IOException e1)
      { throw e1; }
      catch(Exception e2)
      { on_failure(failure_comms); }
   } // on_message


   // lists possible failure codes for this transaction.
   public static final int failure_link = 1;
   public static final int failure_port = 2;
   public static final int failure_timeout = 3;
   public static final int failure_unroutable = 4;
   public static final int failure_comms = 5;
   public static final int failure_unsupported = 6;
   public static final int failure_permission_denied = 7;
   
   
   /**
    * Called to handle a failure.
    * 
    * @param reason  Specifies the reason for the failure
    */
   public void on_failure(int reason) throws Exception
   {
      close();
      if(client != null)
      {
         client.on_failure(this, reason);
         client = null;
      }
   } // on_failure
   

   /**
    * Called to form the next command packet to the logger
    */
   private void send_next_data() throws Exception
   {
      if(can_send && send_buff.whats_left() > 0)
      {
         try
         {
            Packet command = new Packet();
            int bytes_to_send = Math.min(
               send_buff.whats_left(),
               station.get_max_packet_size() - 2);
         
            command.protocol_type = Packet.protocol_bmp5;
            command.message_type = Packet.bmp5_user_io_cmd;
            command.add_uint2(station.get_security_code());
            send_buff.read_bytes(buff,bytes_to_send);
            command.add_bytes(buff, bytes_to_send);
            if(send_buff.whats_left() == 0)
               send_buff.clear();
            can_send = false;
            post_message(command);
         }
         catch(IOException e1)
         { throw e1; }
         catch(Exception e2)
         { on_failure(failure_comms); }
      }
   } // send_next_data
   
   
   /**
    * Holds the client reference
    */
   private UserIoClient client;
   
   
   /**
    * Set to true if this transaction has been started (has focus)
    */
   private boolean can_send;
   
   
   /**
    * Used to buffer bytes received.  
    */
   private byte[] buff;
   
   
   /**
    * Used to hold bytes that need to be transmitted
    */
   private Packet send_buff;
}
