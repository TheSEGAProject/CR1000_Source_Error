/* ListFilesClient.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 04 December 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;

/**
 * Defines the call-back interface used to report the outcome of the 
 * {@link ListFilesTran} transaction.
 */
public interface ListFilesClient
{
   /**
    * Called by the transaction when it has completed.
    *
    * @param transaction The completed transaction
    * @param outcome  Specifies the outcome of the transaction.  This value
    * will correspond with one of the outcome_xxx values in class {@link
    * ListFilesTran}.   The following values are defined:
    *
    * <ul>
    * <li>{@link ListFilesTran#outcome_success}
    * <li>{@link ListFilesTran#outcome_comm_failure}
    * <li>{@link ListFilesTran#outcome_link_failure}
    * <li>{@link ListFilesTran#outcome_port_failure}
    * <li>{@link ListFilesTran#outcome_failure_timeout}
    * <li>{@link ListFilesTran#outcome_failure_unroutable}
    * <li>{@link ListFilesTran#outcome_permission_denied}
    * </ul>
    * @param files  List of objects that describe the files on the datalogger.
    * This will be a null reference unless the value of outcome is equal to <tt>outcome_success</tt>.
    */
   public abstract void on_complete(
      ListFilesTran transaction,
      int outcome,
      List<FileInfo> files) throws Exception;

}
