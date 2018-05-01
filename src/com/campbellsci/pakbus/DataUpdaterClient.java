/* DataUpdaterClient.java

   Copyright (C) 2007, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 04 April 2007
   Last Change: Thursday 12 April 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;


/**
 * Defines the call-back interface used to report the status of the {@link
 * DataUpdater} transaction.
 */
public interface DataUpdaterClient
{
   /**
    * Called after any poll has been completed.  
    * 
    * @param updater  Specifies the updater that just completed the poll
    * @param outcome  Specifies the outcome of this last poll.  Will be one of the 
    * outcome_xxx values defined in class {@link DataCollectTran}.  These include the 
    * following:
    * <ul>
    * <li>{@link DataCollectTran#outcome_unknown}
    * <li>{@link DataCollectTran#outcome_success}
    * <li>{@link DataCollectTran#outcome_link_failure}
    * <li>{@link DataCollectTran#outcome_port_failure}
    * <li>{@link DataCollectTran#outcome_timeout}
    * <li>{@link DataCollectTran#outcome_unroutable}
    * <li>{@link DataCollectTran#outcome_comm_failure}
    * <li>{@link DataCollectTran#outcome_unsupported}
    * <li>{@link DataCollectTran#outcome_permission_denied}
    * <li>{@link DataCollectTran#outcome_aborted}
    * <li>{@link DataCollectTran#outcome_invalid_table_name}
    * <li>{@link DataCollectTran#outcome_invalid_table_defs}
    * </ul>
    */
   public abstract void on_poll_complete(
      DataUpdater updater,
      int outcome) throws Exception;
   
   
   /**
    * Called when records have been received
    *
    * @param updater The updater that collected these records
    * @param records  The list of records that have been collected.  These records
    * will be recycled for future collection so, if the client must keep the
    * records beyond the scope of this call, it should make copies of the
    * record objects.
    */
   public abstract void on_records(
      DataUpdater updater,
      List<Record> records);
}


