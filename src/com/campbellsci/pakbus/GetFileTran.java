/* TranGetFile.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.IOException;


/**
 * Defines a transaction object that implements the BMP5 file receive
 * transaction.
 */
public class GetFileTran extends TransactionBase
{
   // the following variables declare the possible outcome codes for this transaction
   public static final int outcome_success = 0;
   public static final int outcome_link_failure = 1;
   public static final int outcome_port_failure = 2;
   public static final int outcome_timeout = 3;
   public static final int outcome_unroutable = 4;
   public static final int outcome_comm_failure = 5;
   public static final int outcome_unsupported = 6;
   public static final int outcome_permission_denied = 7;
   public static final int outcome_invalid_file_name = 8;
   public static final int outcome_file_not_accessable = 9;
   public static final int outcome_aborted = 10;
   
   
   /**
    * Constructor
    * 
    * @param file_name_  Specifies the name of the file to be retrieved.  
    * This name must conform to the following syntax:
    * 
    *    file_name := device-name ":" file.
    *    device-name := "CPU" | "USR" | "CRD".
    *    
    * @param client_    Specifies the object that will receive completion
    * notification as well as file fragments.   
    */
   public GetFileTran(
      String file_name_,
      GetFileClient client_)
   {
      file_name = file_name_;
      client = client_;
      current_offset = 0;
   } // constructor

   
   @Override
   public String get_name()
   { return "GetFile(" + file_name + ")"; }
   
   
   @Override
   public void on_focus_start() throws Exception
   {
      current_offset = 0;
      max_fragment_size = station.get_max_packet_size() - 7;
      send_next();
   } // on_focus_start

   
   @Override
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5 && 
            message.message_type == Packet.bmp5_file_receive_ack)
         {
            byte response_code = message.read_byte();
            if(response_code == 0)
            {
               // the reported file offset should be the same as the last offset requested.  If it isn't, this may be a repeat due to a retry so we will ignore it.
               long file_offset = message.read_uint4();
               if(file_offset == current_offset)
               {
                  if(message.whats_left() > 0)
                  {
                     byte[] fragment = message.read_bytes(message.whats_left());
                     current_offset += fragment.length;
                     if(client.on_fragment(this, fragment))
                     {
                        if(fragment.length == max_fragment_size)
                        {
                           reset_watchdog();
                           send_next();
                        }
                        else
                           on_complete(outcome_success);
                     }
                     else
                        on_complete(outcome_aborted);
                  }
                  else
                     on_complete(outcome_success);
               }
            }
            else
            {
               int outcome;
               switch(response_code)
               {
               case 1:
                  outcome = outcome_permission_denied;
                  break;
                  
               case 13:
                  outcome = outcome_invalid_file_name;
                  break;
                  
               case 14:
                  outcome = outcome_file_not_accessable;
                  break;
                  
               default:
                  outcome = outcome_comm_failure;
                  break;   
               }
               on_complete(outcome);
            }
         }
      }  
      catch(Exception e)
      { on_complete(outcome_comm_failure); }
   } // on_message
   
      
   @Override
   public void on_failure(int reason) throws Exception
   {
      int outcome = outcome_comm_failure;
      switch(reason)
      {
      case TransactionBase.failure_link:
         outcome = outcome_link_failure;
         break;
         
      case TransactionBase.failure_port:
         outcome = outcome_port_failure;
         break;
         
      case TransactionBase.failure_timeout:
         outcome = outcome_timeout;
         break;
         
      case TransactionBase.failure_unroutable:
         outcome = outcome_unroutable;
         break;
         
      case TransactionBase.failure_unsupported:
         outcome = outcome_unsupported;
         break;
      }
      on_complete(outcome);
   }


   /**
    * Sends the next command packet
    */
   private void send_next() throws Exception
   {
      try
      {
         Packet get_command = new Packet();
         get_command.protocol_type = Packet.protocol_bmp5;
         get_command.message_type = Packet.bmp5_file_receive_cmd;
         get_command.add_uint2(station.get_security_code());
         get_command.add_string(file_name);
         get_command.add_byte((byte)0); // don't close
         get_command.add_uint4(current_offset);  // current offset
         get_command.add_uint2(max_fragment_size);
         post_message(get_command);
      }
      catch(IOException e)
      { throw e; }
      catch(Exception e2)
      { on_complete(outcome_comm_failure); }
   } // send_next
   
   
   /**
    * Called when the transaction needs to end
    */
   private void on_complete(int outcome) throws Exception
   {
      is_satisfied = true;
      if(client != null)
      {
         close();
         client.on_complete(this, outcome);
         client = null;
      }
   } // on_complete
   

   /**
    * Specifies the file name to be retrieved
    */
   private String file_name;
   
   
   /**
    * Specifies the client object for this transaction
    */
   private GetFileClient client;
   
   
   /**
    * Keeps track of the current offset
    */
   private long current_offset;
   
   /** 
    * Keeps track of the fragment size.
    */
   private int max_fragment_size;
}
