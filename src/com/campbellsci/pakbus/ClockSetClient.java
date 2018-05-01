/* ClockSetClient.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 05 October 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the call-back interface used to report the outcome of the 
 * {@link ClockSetTran} transaction.
 */
public interface ClockSetClient
{
   /**
    * Invoked when the clock check operation has completed.
    *
    * @param transaction  Specifies the transaction that is completing.
    * @param outcome  Specifies the outcome of the operation
    */
   public abstract void on_complete(
      ClockSetTran transaction,
      int outcome) throws Exception; 
}
