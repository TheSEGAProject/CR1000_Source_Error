/* FileControlClient.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Saturday 02 December 2006
   Last Change: Tuesday 27 May 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the call-back interface used to report the outcome of the 
 * {@link FileControlTran} transaction.
 */
public interface FileControlClient
{
   /**
    * Called when the file control transaction has been completed.
    *
    * @param transaction The transaction that has been completed
    * @param outcome Specifies the outcome of this transaction.  Potential
    * values include the following:
    * <ul>
    * <li>{@link FileControlTran#outcome_success}
    * <li>{@link FileControlTran#outcome_comm_failure}
    * <li>{@link FileControlTran#outcome_failure_timeout}
    * <li>{@link FileControlTran#outcome_failure_unroutable}
    * <li>{@link FileControlTran#outcome_invalid_command_code}
    * <li>{@link FileControlTran#outcome_invalid_file_spec}
    * <li>{@link FileControlTran#outcome_link_failure}
    * <li>{@link FileControlTran#outcome_not_enough_storage}
    * <li>{@link FileControlTran#outcome_permission_denied}
    * <li>{@link FileControlTran#outcome_port_failure}
    * <li>{@link FileControlTran#outcome_root_dir_full}
    * </ul>
    * @param hold_off Specifies a time period (in seconds) that the application
    * should wait before attempting another transaction with this datalogger.
    * This value will generally be non-zero following a command to set the
    * currently running program or setting the operating system.  In these
    * cases, the datalogger must reboot.
    *
    * If the application is connected directly to the datalogger via TCP/IP
    * (either using the NL115 or the PPP interface), it should close that
    * connection after this transaction if the value of <tt>hold_off</tt> is
    * non-zero.  The reason for this is that, in rebooting, the datalogger will
    * have invalidated the TCP connection and future attempts to use it will
    * only cause communication errors.
    */
   public abstract void on_complete(
      FileControlTran transaction,
      int outcome,
      int hold_off) throws Exception;
}
