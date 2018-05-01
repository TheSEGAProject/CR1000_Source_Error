/* TranSendFile.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 12 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.io.*;


/**
 * Defines a transaction that implements the BMP5 send file transaction.
 */
public class SendFileTran extends TransactionBase
{
   /**
    * Constructor
    *
    * @param client  Specifies the object that will receive progress and
    * completion notifications.
    * @param input   Specifies the file input stream.  The contents of this
    * stream up to the end of the stream will be transmitted to the
    * datalogger.
    * @param logger_file_name  Specifies the name of the file in the logger
    * file system.  This value must conform to the following syntax:
    *
    *   logger_file_name := device-name ":" file-name.
    *   device-name      := "CPU" | "USR" | "CRD".
    */
   public SendFileTran(
      SendFileClient client,
      InputStream input,
      String logger_file_name) throws Exception
   {
      this.client = client;
      this.input = input;
      this.logger_file_name = logger_file_name;
      bytes_to_send = input.available();
      bytes_sent = 0;
      fragment_buffer = new byte[1000];
   } // constructor
    
   
   @Override
   public String get_name()
   { return "SendFile(" + logger_file_name + ")"; }
   
   
   /**
    * Specifies that the transaction has successfully completed.
    */
   public static final int outcome_success = 0;

   /**
    * Specifies that the transaction failed because the PakBus link was lost.
    */
   public static final int outcome_link_failure = 1;

   
   /**
    * Specifies that the transaction failed because the low level I/O failed.
    */
   public static final int outcome_port_failure = 2;

   /**
    * Specifies that the transaction failed because no response was received from the datalogger
    * even after retries.
    */
   public static final int outcome_timeout = 3;

   /**
    * Specifies that the transaction failed because the messages could not be routed to the
    * datalogger.
    */
   public static final int outcome_unroutable = 4;

   /**
    * Specifies that the transaction failed because the datalogger response could not be
    * interpreted.
   */
   public static final int outcome_comm_failure = 5;

   /**
    * Specifies that the transaction failed because the datalogger does not support the command
    * message.
   */
   public static final int outcome_unsupported = 6;

   /**
    * Specifies that the transaction failed because our security code is invalid.
    */
   public static final int outcome_permission_denied = 7;

   /**
    * Specifies that the transaction failed because the file name specified is not valid.
    */
   public static final int outcome_invalid_file_name = 8;

   /**
    * Specifies that the transaction failed because the datalogger could not overwrite the specified
    * file.
   */
   public static final int outcome_file_not_accessable = 9;

   /**
    * Specifies that the transaction terminated because it was aborted.
    */
   public static final int outcome_aborted = 10;

   /**
    * Specifies that transaction failed because the datalogger does not have enough space of the
    * storage device for the new file.
   */
   public static final int outcome_storage_full = 11;

   /**
    * Specifies that the transaction failed because we could not read the file locally.
    */ 
   public static final int outcome_read_failure = 12;

   /**
    * Specifies that the transaction failed for a reason that we cannot identify.
    */
   public static final int outcome_unknown = 13;
   
   /**
    * Specifies that the transaction failed because the datalogger does not have space in the root
    * file system of the specified device to store a new file.
    */
   public static final int outcome_root_dir_full = 14;
   
   @Override
   public void on_failure(int reason) throws Exception
   {
      int outcome = outcome_unknown;
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
      send_next();
   } // on_focus_start


   @Override
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5 &&
            message.message_type == Packet.bmp5_file_send_ack)
         {
            byte response_code = message.read_byte();
            if(response_code == 0)
            {
               int file_offset = message.read_int4();
               if(file_offset == bytes_sent)
               {
                  boolean ok_to_continue = true;
                  bytes_sent += fragment_buffer_len;
                  if(client != null)
                     ok_to_continue = client.on_progress(this, bytes_to_send, bytes_sent);
                  if(!at_end && ok_to_continue)
                  {
                     reset_watchdog();
                     send_next();
                  }
                  else if(at_end)
                     on_complete(outcome_success);
                  else
                     on_complete(outcome_aborted);
               }
            }
            else
            {
               int outcome = outcome_unknown;
               switch(response_code)
               {
               case 1:
                  outcome = outcome_permission_denied;
                  break;
                  
               case 2:
                  outcome = outcome_storage_full;
                  break;
                  
               case 13:
                  outcome = outcome_invalid_file_name;
                  break;
                  
               case 14:
                  outcome = outcome_file_not_accessable;
                  break;

               case 20:
                  outcome = outcome_root_dir_full;
                  break;
               }
               on_complete(outcome);
            }
         }
      }
      catch(IOException e1)
      { throw e1; }
      catch(Exception e2)   
      { on_complete(outcome_comm_failure); }
   } // on_message

   
   /**
    * Called to send the next command message
    */
   private void send_next() throws Exception
   {
      try
      {
         Packet command = new Packet();
         command.protocol_type = Packet.protocol_bmp5;
         command.message_type = Packet.bmp5_file_send_cmd;
         command.add_uint2(station.get_security_code());
         if(bytes_sent == 0)
            command.add_string(logger_file_name);
         else
            command.add_byte((byte)0);
         command.add_byte((byte)0);  // attributes terminator
         read_next_fragment();
         command.add_byte(at_end ? (byte)1 : (byte)0);
         command.add_int4(bytes_sent);
         command.add_bytes(fragment_buffer,fragment_buffer_len);
         post_message(command);
      }
      catch(Exception e)
      { on_complete(outcome_comm_failure); }
   } // send_next
   
   
   /**
    * Reads the next fragment into the fragment buffer.
    */
   private void read_next_fragment()
   {
      // we need to determine the number of bytes that can be sent
      int available = bytes_to_send - bytes_sent;
      fragment_buffer_len = station.get_max_packet_size() - 9;
      if(bytes_sent == 0)
         fragment_buffer_len -= logger_file_name.length();
      if(fragment_buffer_len >= available)
      {
         fragment_buffer_len = available;
         at_end = true;
      }
      
      // we can now attempt to read the bytes from the input stream
      try
      {
         input.read(fragment_buffer,0,fragment_buffer_len);
      }
      catch(IOException e)
      { 
         fragment_buffer_len = 0;
         at_end = true;
      }
   } // read_next_fragment
   
   
   /**
    * Called when this transaction is complete
    */
   private void on_complete(int outcome) throws Exception
   {
      close();
      if(client != null)
      {
         client.on_complete(this, outcome);
         client = null;
      }
   } // on_complete
   

   /**
    * Reference to the object that will receive progress and completion notifications. 
    */
   private SendFileClient client;
   
   
   /**
    * Reference to the input stream
    */
   private InputStream input;
   
   
   /**
    * Holds the logger file name
    */
   private String logger_file_name;
   
   
   /**
    * Holds the number of bytes that need to be sent
    */
   private int bytes_to_send;
   
   
   /**
    * Holds the number of bytes that have been sent.
    */
   private int bytes_sent;
   
   
   /**
    * Used to copy data from the input stream to the next outgoing message
    */
   private byte[] fragment_buffer;
   
   
   /**
    * Holds the number of bytes currently in the fragment buffer.
    */
   private int fragment_buffer_len;
   
   
   /**
    * Specifies whether all of the bytes have been read into the fragment buffer.
    */
   private boolean at_end;
}


