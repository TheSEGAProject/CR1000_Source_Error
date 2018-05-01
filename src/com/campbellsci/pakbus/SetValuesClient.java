/* SetValuesClient.java

   Copyright (C) 2007, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Monday 07 May 2007
   Last Change: Monday 07 May 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines the call-back interface used to report the outcome of the
 * {@link SetValuesTran} transaction.
 */
public interface SetValuesClient
{
   /**
    * Called when the transaction has finished.
    * 
    * @param transaction  Specifies the set values transaction that just completed.
    * @param outcome  Specifies the outcome of the transaction.  Will be one of the following:
    * <ul>
    * <li>{@link SetValuesTran#outcome_success}
    * <li>{@link SetValuesTran#outcome_comm_failure}
    * <li>{@link SetValuesTran#outcome_failure_timeout}
    * <li>{@link SetValuesTran#outcome_link_failure}
    * <li>{@link SetValuesTran#outcome_failure_unroutable}
    * <li>{@link SetValuesTran#outcome_failure_unsupported}
    * <li>{@link SetValuesTran#outcome_conversion_not_supported}
    * <li>{@link SetValuesTran#outcome_invalid_table_or_field}
    * <li>{@link SetValuesTran#outcome_permission_denied}
    * <li>{@link SetValuesTran#outcome_memory_bound_error}
    * </ul>
    * @throws Exception 
    */
   void on_complete(
      SetValuesTran transaction,   
      int outcome) throws Exception;

}
