/* GetValuesClient.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Thursday 07 December 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the call-back interface used to report the outcome of the
 * {@link GetValuesTran} transaction.
 */
public interface GetValuesClient
{
   /**
    * Called when the transaction has been completed.
    *
    * @param transaction  Specifies the now completed transaction
    * @param outcome      Specifies the outcome of the transaction.  Values include
    * the following:
    * <ul>
    * <li>{@link GetValuesTran#outcome_success}
    * <li>{@link GetValuesTran#outcome_comm_failure}
    * <li>{@link GetValuesTran#outcome_failure_timeout}
    * <li>{@link GetValuesTran#outcome_link_failure}
    * <li>{@link GetValuesTran#outcome_failure_unroutable}
    * <li>{@link GetValuesTran#outcome_failure_unsupported}
    * <li>{@link GetValuesTran#outcome_conversion_not_supported}
    * <li>{@link GetValuesTran#outcome_invalid_table_or_field}
    * <li>{@link GetValuesTran#outcome_permission_denied}
    * <li>{@link GetValuesTran#outcome_memory_bound_error}
    * </ul>
    * @param values       Specifies the list of values returned from the
    * datalogger.  This will be null unless the value of outcome is equal to
    * {@link GetValuesTran#outcome_success}).
    */
   public abstract void on_complete(
      GetValuesTran transaction,
      int outcome,
      Record values) throws Exception;
}
