/* SendFileClient.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 12 October 2006
   Last Change: Tuesday 27 May 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines the call-back interface used to report the status and outcome of the 
 * {@link SendFileTran} transaction.
 */
public interface SendFileClient
{
   /**
    * Called when some progress has been made in sending the file.  
    * 
    * @param transaction   The transaction sending the file
    * @param bytes_to_send  Specifies the total number of bytes that have 
    * to be sent
    * @param bytes_sent     Specifies the number of bytes that have already 
    * been acknowledged
    * @return true if the transaction is to continue or false if the 
    * transaction should abort.
    */
   public abstract boolean on_progress(
      SendFileTran transaction,
      int bytes_to_send,
      int bytes_sent);
   
   
   /**
    * Called when the transaction has finished
    *
    * @param transaction   the file send transaction
    * @param outcome       specifies the outcome of the transaction.  Potential values include the
    * following:
    * <ul>
    * <li>{@link SendFileTran#outcome_success}
    * <li>{@link SendFileTran#outcome_link_failure}
    * <li>{@link SendFileTran#outcome_port_failure}
    * <li>{@link SendFileTran#outcome_timeout}
    * <li>{@link SendFileTran#outcome_comm_failure}
    * <li>{@link SendFileTran#outcome_unroutable}
    * <li>{@link SendFileTran#outcome_unsupported}
    * <li>{@link SendFileTran#outcome_permission_denied}
    * <li>{@link SendFileTran#outcome_invalid_file_name}
    * <li>{@link SendFileTran#outcome_file_not_accessable}
    * <li>{@link SendFileTran#outcome_aborted}
    * <li>{@link SendFileTran#outcome_storage_full}
    * <li>{@link SendFileTran#outcome_read_failure}
    * <li>{@link SendFileTran#outcome_root_dir_full}
    * </ul>
    */
   public abstract void on_complete(
      SendFileTran transaction,
      int outcome) throws Exception;
}
