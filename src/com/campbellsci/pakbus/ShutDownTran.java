/* TranShutDown.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 09 October 2006
   Last Change: Thursday 07 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines a transaction that will shut down the link for this logger.
 */
class ShutDownTran extends TransactionBase
{
   /**
    * constructor
    */
   ShutDownTran()
   { }


   /**
    * Overloads the base class version to send an empty message
    */
   public void on_focus_start() throws Exception
   {
      is_satisfied = true;
      post_message(new Packet());
      close();
   } // on_focus_start


   /**
    * Overloads the base class version to do nothing.
    */
   public void on_message(Packet message) throws Exception
   { }


   /**
    * Overloads the base class version.
    *
    * @return true to indicate that this transaction will close the link.
    */
   public boolean will_close()
   { return true; }
}


