/* TranManageCommResource.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 12 October 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Implements a transaction that will keep the shut down from being sent out
 * due to its presence.  This feature can be used by invoking the
 * start_manage_comms() method of class Datalogger and can be cancelled by
 * invoking stop_manage_comms().
 */
class ManageCommResourceTran extends TransactionBase
{
   protected ManageCommResourceTran()
   { priority = Packet.pri_low; }

   
   @Override
   public void on_focus_start() throws Exception
   {
      // we do nothing here because this class will never have focus
   }

   @Override
   public void on_message(Packet message) throws Exception
   {
      // we don's send any messages and so won't expect any in return.  This
      // might be changed later to implement a timer that will send occasional
      // commands in order to keep the comms link alive.
   }


   @Override
   public void close() throws Exception
   {
      station.comm_resource_manager = null;
      super.close();
   } // close
}
