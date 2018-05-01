/* ListFilesTran.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 04 December 2006
   Last Change: Saturday 12 April 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;


/**
 * Defines a transaction object that reads the directory of files from the datalogger. 
 */
public class ListFilesTran extends TransactionBase implements GetFileClient
{
   /**
    * Indicates that the transaction succeeded
    */
   public static final int outcome_success = 1;
   
   /**
    * Specifies the transaction failed due to a general failure in communications.
    */
   public static final int outcome_comm_failure = 2;
   
   /**
    * Specifies that the transaction failed because the PakBus link was lost
    */
   public static final int outcome_link_failure = 3;
   
   /**
    * Specifies that the transaction failed because low level I/O failed.
    */
   public static final int outcome_port_failure = 4;
   
   /**
    * Specifies that the transaction failed because no response was received event after retries. 
    */
   public static final int outcome_failure_timeout = 5;
   
   /**
    * Specifies that the transaction failed because the command message could not be routed.
    */
   public static final int outcome_failure_unroutable = 6;
   
   /**
    * Specifies that the transaction failed because our security code is invalid
    */
   public static final int outcome_permission_denied = 7;
   
   /**
    * Specifies that this transaction is not supported by the datalogger.
    */
   public static final int outcome_failure_unsupported = 8;
   
   
   /**
    * Constructor
    *
    * @param client_  Specifies an object that implements the {@link
    * ListFilesClient} interface.  This object's {@link
    * ListFilesClient#on_complete on_complete()} method will be invoked when
    * the transaction is complete.
    */
   public ListFilesTran(ListFilesClient client_)
   { 
      client = client_;
      dir_buff = new Packet();
   } // constructor

   
   @Override
   public void on_focus_start() throws Exception
   {
      file_getter = new GetFileTran("CPU:.dir",this);
      station.add_transaction_without_focus(file_getter);
      if(files != null)
         files.clear();
      file_getter.on_focus_start();
   } // on_focus_set

   
   @Override
   public void on_message(Packet message) throws Exception
   { }

   
   public void on_complete(GetFileTran transaction, int outcome_) throws Exception
   {
      if(outcome_ == GetFileTran.outcome_success)
      {
         try
         {
            int version = dir_buff.read_byte();
            if(version == 1)
            {
               files = new LinkedList<FileInfo>();
               while(dir_buff.whats_left() > 0)
               {
                  FileInfo info = new FileInfo();
                  byte attr;
                  
                  info.name = dir_buff.read_string();
                  info.size = dir_buff.read_uint4();
                  info.last_update_time = dir_buff.read_string();
                  while((attr = dir_buff.read_byte()) != 0)
                  {
                     switch(attr)
                     {
                     case 0x01:
                        info.running_now = true;
                        break;
                        
                     case 0x02:
                        info.run_on_power_up = true;
                        break;
                        
                     case 0x03:
                        info.read_only = true;
                        break;
                        
                     case 0x05:
                        info.paused = true;
                        break;
                     }
                  }
                  files.add(info);
               }
               on_complete(outcome_success);
            }
            else
               on_complete(outcome_comm_failure);
         }
         catch(Exception e)
         { 
            files = null;
            on_complete(outcome_comm_failure); 
         }
      }
      else
      {
         int outcome;
         switch(outcome_)
         {
         case GetFileTran.outcome_comm_failure:
         case GetFileTran.outcome_file_not_accessable:
            outcome = outcome_comm_failure;
            break;
            
         case GetFileTran.outcome_invalid_file_name:
            outcome = outcome_failure_unsupported;
            break;
            
         case GetFileTran.outcome_link_failure:
            outcome = outcome_link_failure;
            break;
            
         case GetFileTran.outcome_permission_denied:
            outcome = outcome_permission_denied;
            break;
            
         case GetFileTran.outcome_port_failure:
            outcome = outcome_port_failure;
            break;
            
         case GetFileTran.outcome_timeout:
            outcome = outcome_failure_timeout;
            break;
            
         case GetFileTran.outcome_unroutable:
            outcome = outcome_failure_unroutable;
            break;
            
         case GetFileTran.outcome_unsupported:
            outcome = outcome_failure_unsupported;
            break;
            
         default:
            outcome = outcome_comm_failure;
            break;
         }
         on_complete(outcome);
      }
   } // on_complete


   public boolean on_fragment(GetFileTran transaction, byte[] fragment) throws Exception
   {
      dir_buff.add_bytes(fragment,fragment.length);
      return true;
   } // on_fragment


   /**
    * Called when this transaction has ben completed
    */
   private void on_complete(int outcome) throws Exception
   {
      close();
      if(outcome != outcome_success)
         files = null;
      if(client != null)
         client.on_complete(this,outcome,files);
   } // on_complete
   
   
   /**
    * Holds the reference to the client
    */
   private ListFilesClient client;
   
   
   /**
    * Holds the file information that was read
    */
   private List<FileInfo> files;
   
   /**
    * Holds the reference to the transaction that will get the directory file from the logger.
    */
   private GetFileTran file_getter;
   
   /**
    * Buffers the contents of the directory file as it is received
    */
   private Packet dir_buff;
}
