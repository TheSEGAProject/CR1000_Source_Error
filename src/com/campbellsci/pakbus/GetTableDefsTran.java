/* TranGetTableDefs.java

   Copyright (C) 2006, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Monday 19 March 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines a transaction that gets up to date table definitions from the
 * datalogger and reports the outcome of this operation to a specified client.
 */
public class GetTableDefsTran extends TransactionBase
   implements  GetFileClient, GetProgStatsClient
{
   // the following constants define the possible outcome codes for this transaction.
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
    * @param client_  The object that will receive notifications when the
    * transaction is complete.  If this value is null, no such notification
    * will be sent.
    */
   public GetTableDefsTran(GetTableDefsClient client_)
   {
      client = client_;
      priority = Packet.pri_high;
   } //  constructor

   
   @Override
   public void on_focus_start() throws Exception
   {
      stats_getter = new GetProgStatsTran(this);
      station.add_transaction_without_focus(stats_getter);
      stats_getter.on_focus_start();
   } // on_focus_start
   

   @Override
   public void on_message(Packet message) throws Exception
   {
      // this transaction will not be directly involved in processing messages
   }

   
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


   public void on_complete(GetFileTran transaction, int outcome_) throws Exception
   {
      if(outcome_ == GetFileTran.outcome_success)
      {
         station.set_raw_table_defs(raw_table_defs);
         on_complete(outcome_success);
      }
      else
      {
         int outcome = outcome_unknown;
         switch(outcome_)
         {
         case GetFileTran.outcome_comm_failure:
         case GetFileTran.outcome_file_not_accessable:   
         case GetFileTran.outcome_invalid_file_name:
            outcome = outcome_comm_failure;
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
            outcome = outcome_timeout;
            break;
            
         case GetFileTran.outcome_unroutable:
            outcome = outcome_unroutable;
            break;
            
         case GetFileTran.outcome_unsupported:
            outcome = outcome_unsupported;
            break;
         }
         on_complete(outcome);
      }
   } // on_complete


   public boolean on_fragment(GetFileTran transaction, byte[] fragment)
   {
      if(raw_table_defs == null)
         raw_table_defs = new Packet();
      raw_table_defs.add_bytes(fragment,fragment.length);
      return true;
   }
   
   
   /**
    * Overloads the completion method for getting program stats
    */
   public void on_complete(GetProgStatsTran transaction, int outcome) throws Exception
   {
      if(outcome == GetProgStatsTran.outcome_success)
      {
         file_reader = new GetFileTran(".TDF",this);
         station.add_transaction_without_focus(file_reader);
         file_reader.on_focus_start();
      }
      else
         on_complete(outcome);
   } // on_complete
   

   /**
    * Called when this transaction has completed
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
    * Reference to the object that gets notified when table definitions have
    * been received.
    */
   private GetTableDefsClient client;


   /**
    * Reference to the transaction that reads the file for this transaction
    */
   private GetFileTran file_reader;
   
   
   /**
    * Accumulates the table definitions file so that the whole can be processed. 
    */
   private Packet raw_table_defs;
   
   
   /** 
    * Reference to the transaction that updates the current compile info
    */
   private GetProgStatsTran stats_getter;
}
