/* GetFileClient.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the call-back interface used to report the outcome and status  
 * of the {@link GetFileTran} transaction.
 */ 
public interface GetFileClient
{
   /**
    * Called when a file fragment has been received.
    *
    * @param transaction  The file receive transaction
    * @param fragment  Holds the contents of the fragment
    * @return Should return true if the transaction is to continue.  A value
    * false will cause the transaction to abort.
    */
   public abstract boolean on_fragment(
      GetFileTran transaction,
      byte[] fragment) throws Exception;


   /**
    * Called when the transaction is complete.
    *
    * @param transaction The transaction that is complete
    * @param outcome     Specifies the outcome of the transaction.  This will
    * correspond with one of the outcome_xxx members defined in TranGetFile. 
    */
   public abstract void on_complete(
      GetFileTran transaction,
      int outcome) throws Exception;
}
