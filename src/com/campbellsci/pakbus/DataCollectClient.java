/* DataCollectClient.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Monday 16 October 2006
   Last Change: Friday 08 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;


/**
 * Defines the call-back interface used to report the outcome and status
 * of the {@link DataCollectTran} transaction.
 */
public interface DataCollectClient
{
   /**
    * Called when records have become available to be processed by the client.
    * The client has the option to terminate the transaction by returning false.
    * 
    * @param transaction   Specifies the data collection transaction
    * @param records       The list of records that the client can process.
    * The transaction will return the list of records to a queue that allows
    * them to be "recycled" (used again for future reports).  If the client 
    * is going to make continued use of these record objects after this method
    * call, it should clear this queue to prevent this recycling behaviour.
    * @return true if the transaction should continue or false if the transaction
    * must abort. 
    */
   public abstract boolean on_records(
      DataCollectTran transaction,
      List<Record> records);
   
   
   /**
    * Called when the data collect transaction has finished or failed.
    * 
    * @param transaction  The transaction object 
    * @param outcome      Specifies the outcome of the transaction.  Will match
    * one of the outcome_xxx members of class {@link DataCollectTran}.  These values
    * include the following:
    * <ul>
    * <li>{@link DataCollectTran#outcome_success}
    * <li>{@link DataCollectTran#outcome_aborted}
    * <li>{@link DataCollectTran#outcome_comm_failure}
    * <li>{@link DataCollectTran#outcome_invalid_table_defs}
    * <li>{@link DataCollectTran#outcome_invalid_table_name}
    * <li>{@link DataCollectTran#outcome_link_failure}
    * <li>{@link DataCollectTran#outcome_permission_denied}
    * <li>{@link DataCollectTran#outcome_port_failure}
    * <li>{@link DataCollectTran#outcome_timeout}
    * <li>{@link DataCollectTran#outcome_unknown}
    * <li>{@link DataCollectTran#outcome_unroutable}
    * <li>{@link DataCollectTran#outcome_unsupported}
    * </ul>
    */
   public abstract void on_complete(
      DataCollectTran transaction,
      int outcome) throws Exception;
}
