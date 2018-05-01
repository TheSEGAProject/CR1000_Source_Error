/* DataCollectTran.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 16 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:54 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.nau.wnrl.NullNewestRecordException;


/**
 * This class implements data collection for a single datalogger table.  
 */
public class DataCollectTran extends TransactionBase
{
   /**
    * Specifies the client and table name for this transaction.  The default 
    * collect mode will be to collect the most recent record.  This can be
    * changed by calling one of the set_mode_xxx() methods.  
    *
    * @param table_name  Specifies the name of the table to be queried
    * @param client      Specifies the object that will receive completion and
    * @param collect_mode  Specifies how the query will be carried out.
    */
   public DataCollectTran(
      String table_name,
      DataCollectClient client,
      DataCollectMode collect_mode)
   {
      this.table_name = table_name;
      this.client = client;
      this.collect_mode = collect_mode;
      collect_mode.transaction = this;
      response_set = new LinkedList<Record>();
      record_data = new Packet();
   } // constructor
   
   
   /**
    * Specifies the client and the table definition object that will be used for 
    * data collection.  By using this constructor, the application can avoid the 
    * time required to look up the table definition for each poll.  This also 
    * provides the application to select the column pieces that will be polled.
    * 
    * @param table_def_  Specifies the table definition that will be used.  This 
    * must be a valid object.
    * @param client_ Specifies the object that will receive data and completion
    * notifications.
    * @param collect_mode_  Controls how the poll will be carried out.
    */
   DataCollectTran(
      TableDef table_def_,
      DataCollectClient client_,
      DataCollectMode collect_mode_)
   {
      table_def = table_def_;
      table_name = table_def.name;
      client = client_;
      collect_mode = collect_mode_;
      response_set = new LinkedList<Record>();
      record_data = new Packet();
   }
   
   
   @Override
   public String get_name()
   {
      return "DataCollect(" + table_name + ", " + collect_mode.get_name() + ")";
   }




   // the following variables declare the possible outcome codes for this transaction
   /**
    * Used to report that an unknown failure has occurred
    */
   public static final int outcome_unknown = -1;

   /**
    * Used to report that the transaction has succeeded
    */
   public static final int outcome_success = 0;

   /**
    * Used to report that the transaction failed due to a failure in the serial
    * packet protocol
    */
   public static final int outcome_link_failure = 1;

   /**
    * Used to report that the transaction failed due to the low level link
    */
   public static final int outcome_port_failure = 2;

   /**
    * Used to report that the transaction failed do to no response from the datalogger
    * after retries
    */
   public static final int outcome_timeout = 3;

   /**
    * Used to report that the transaction failed because the command message could not
    * be routed to the datalogger.
    */
   public static final int outcome_unroutable = 4;

   /**
    * Used to report that the datalogger response could not be interpreted.
    */ 
   public static final int outcome_comm_failure = 5;

   /**
    * Used to report that the command message is not supported by the datalogger.
    */
   public static final int outcome_unsupported = 6;

   /**
    * Used to report that the datalogger rejected the station security code.
    */
   public static final int outcome_permission_denied = 7;

   /**
    * Used to report that the transaction was aborted because the client returned
    * false when {@link DataCollectClient#on_records} was called.
    */
   public static final int outcome_aborted = 8;

   /**
    * Used to report that the transaction failed because the requested table name is not
    * in the set of current table definitions.
    */
   public static final int outcome_invalid_table_name = 9;

   /**
    * Used to report that the transaction failed because the current table definitions do
    * not match those on the logger.
    */
   public static final int outcome_invalid_table_defs = 10;
   
   /**
    * Throws custom exception whenever data is requested from a table that doesn't have data
    * points yet
    */
   public static final int outcome_null_newest_record = 11;
   
   /**
    * @return the table name
    */
   public String get_table_name()
   { return table_name; }
   
   
   /**
    * Responsible for initiating communications
    */
   @Override
   public void on_focus_start() throws Exception
   {
      // we need to look up the table definition from the station.  This
      // represents our first opportunity since the station object must now
      // be assigned
      if(table_def == null)
         table_def = station.get_table(table_name);
      if(table_def != null)
      {
         // we need to make sure that the collect mode has the correct parameters
         collect_mode.table_def = table_def;
         collect_mode.station = station;
         
         // get the first command from the collect mode
         Packet command = collect_mode.get_next_command();
         if(command != null)
            post_message(command);
         else
            on_complete(outcome_success);
      }
      else
         on_complete(outcome_invalid_table_name);
   } // on_focus_start

   
   /**
    * Handles incoming messages
    */
   @Override
   public void on_message(Packet message) throws Exception
   {
      try
      {
         if(message.protocol_type == Packet.protocol_bmp5 &&
            message.message_type == Packet.bmp5_collect_data_ack)
         {
            byte response_code = message.read_byte();
            reset_watchdog();
            if(response_code == 0)
            {
               boolean completing_partial = false;
               while(message.whats_left() > 2 && !completing_partial)
               {
                  // read the block header
                  int table_no = message.read_uint2();
                  long begin_record_no = message.read_uint4();
                  int records_count = message.read_uint2();
                  boolean partial_record = (records_count & 0x8000) != 0;
                  
                  if(table_no != table_def.table_no)
                     break;
                  if(partial_record)
                  {
                     records_count = 1;
                     message.move_past(2);
                  }
                  else
                     records_count &= 0x7fff;
                  if(records_count == 0)
                     continue;
                  
                  // we now need to determine how much data is required for the response
                  // to be complete.
                  int required_size = table_def.get_native_block_size(records_count);
                  int block_size = Math.min(
                     required_size, 
                     message.whats_left() - 1);
                  record_data.add_bytes(message.read_bytes(block_size), block_size);
                  
                  // at this point, we need to determine whether to request more fragments
                  // or whether we can proceed
                  if(partial_record && record_data.whats_left() < required_size)
                  {
                     Packet command = new Packet();
                     command.protocol_type = Packet.protocol_bmp5;
                     command.message_type = Packet.bmp5_collect_data_cmd;
                     command.add_uint2(station.get_security_code());
                     command.add_byte((byte)8);
                     command.add_uint2(table_def.table_no);
                     command.add_uint2(table_def.def_sig);
                     command.add_uint4(begin_record_no);
                     command.add_uint4((long)record_data.whats_left());
                     table_def.format_column_request(command);
                     station.change_transaction_id(this);
                     post_message(command);
                     completing_partial = true;
                  }
                  else
                     read_records(begin_record_no,records_count);
               }
               
               // if we are not comleting a partial set, we have now read 
               // all of the records sent.  This must now be sent back to the 
               // collect mode so it can determine the next course of action
               if(!completing_partial)
               {
                  // let the collect mode have first crack at the set.  Then send it to the client
                  boolean continue_transaction = true;
                  collect_mode.on_response(response_set);
                  if(!response_set.isEmpty() && client != null)
                     continue_transaction = client.on_records(this, response_set);
                  for(Record record: response_set)
                     table_def.cache_record(record);
                  response_set.clear();
                  record_data.clear();
                  
                  // we now need to get the next command
                  if(continue_transaction)
                  {
                     Packet command = collect_mode.get_next_command();
                     if(command != null)
                     {
                        station.change_transaction_id(this);
                        post_message(command);
                     }
                     else
                        on_complete(outcome_success);
                  }
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
                  outcome = outcome_comm_failure;
                  break;
                  
               case 7:
                  outcome = outcome_invalid_table_defs;
                  break;
               }
               on_complete(outcome);
            }
         }
      }
      catch(IOException e1)
      {
         e1.printStackTrace();
         throw e1; 
      }
      catch(NullNewestRecordException e2){
    	  //Throw null newest record exception
    	  on_complete(outcome_null_newest_record);
      }
      catch(Exception e3)
      { 
         e3.printStackTrace();
         on_complete(outcome_comm_failure); }
   } // on_message
   
   
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


   /**
    * Called when this transaction has been completed.
    * 
    * @param outcome  Specifies the outcome of this transaction
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
    * Used to read a collection of records from the record buffer 
    * into the response set.
    * 
    * @param begin_record_no  Specifies the record number for the first
    * record
    * @param records_count    Specifies the number of records expected
    */
   private void read_records(long begin_record_no, int records_count) throws Exception
   {
      table_def.read_records(
         record_data,
         response_set,
         begin_record_no,
         records_count);
   } // read_records
   
   
   /**
    * Stores the table name
    */
   private String table_name;
   
   
   /**
    * Stores the client reference
    */
   private DataCollectClient client;
   
   
   /**
    * Reference to the collect mode object that will govern this
    * transaction. 
    */
   private DataCollectMode collect_mode;
   
   
   /**
    * Reference to the table definition that will involve this query
    */
   private TableDef table_def;
   
   
   /**
    * Holds the response set of records
    */
   private List<Record> response_set;
   
   
   /**
    * Used to accumulate the fragments of record data 
    */
   Packet record_data;
}
