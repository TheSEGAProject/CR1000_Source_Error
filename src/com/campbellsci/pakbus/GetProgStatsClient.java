/* GetProgStatsClient.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines the call-back interface used to report the outcome of the 
 * {@link GetProgStatsTran} transaction.
 */
public interface GetProgStatsClient
{
   /**
    * Called when the operation has been completed
    * 
    * @param transaction  the transaction object
    * @param outcome   Specifies the transaction outcome.  Will be one of the 
    * outcome_xxx values defined in class TranGetProgStats.
    */
   public abstract void on_complete(
      GetProgStatsTran transaction,
      int outcome) throws Exception;
}
