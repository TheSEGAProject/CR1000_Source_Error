/* UserIoClient.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 02 November 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the call-back interface used to report the outcome and status of
 * the {@link UserIoTran} transaction.
 */
public interface UserIoClient
{
   /**
    * Called when the transaction is ready to send or receive user I/O
    * messages.  The application must wait until this is called before 
    * attempting to send user I/O content.
    * 
    * @param transaction  the transaction that has been started
    */
   public abstract void on_started(UserIoTran transaction) throws Exception;
   
   
   /**
    * Called when the transaction has received bytes from the datalogger.
    * 
    * @param transaction  the user I/O transaction
    * @param buff     Holds the bytes that have been received
    * @param buff_len  The number of bytes stored in the buff parameter.
    */
   public abstract void on_bytes_received(
      UserIoTran transaction,
      byte[] buff,
      int buff_len) throws Exception;
   
   
   /**
    * Called when a failure has occurred that will keep the user I/O
    * transaction from continuing.
    * 
    * @param transaction  The failed transaction
    * @param reason  Specifies the reason for the failure. Will
    * correspond with one of the failure_xxx constants defined in
    * class UserIoTran.
    */
   public abstract void on_failure(
      UserIoTran transaction,
      int reason) throws Exception;
}
