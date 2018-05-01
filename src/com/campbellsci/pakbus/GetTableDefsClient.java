/* GetTableDefsClient.java

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
 * {@link GetTableDefsTran} transaction.
 */
public interface GetTableDefsClient
{
   /**
    * Called after the table definitions have been received and parsed by the
    * datalogger.
    *
    * @param transaction the table definitions transaction
    * @param outcome     Specifies the outcome of the transaction.  This will
    * correspond with one of the outcome_xxx values defined in class
    * TranGetTableDefs.
    */
   public abstract void on_complete(
      GetTableDefsTran transaction,
      int outcome) throws Exception; 
}
