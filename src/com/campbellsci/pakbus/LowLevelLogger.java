/* LowLevelLogger.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 06 October 2006
   Last Change: Friday 06 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Specifies the interface for an object that can process low level log events
 * from the network class.  An application object can implement this interface
 * and be associated with a Network object via add_low_level_logger().  Once
 * this association is complete, the objects will receive notifications when a
 * low level event occurs within the network.
 */
public interface LowLevelLogger
{
   /**
    * Called when a signle byte has been sent or received by the network
    * object.
    *
    * @param value Specifies the byte
    * @param transmitted Specifies whether the byte was sent or received.
    */
   public abstract void on_io(
      int value,
      boolean transmitted);


   /**
    * Called when the network is sending text comments.
    */
   public abstract void on_comment(String comment);
   
   
   /**
    * Called by the network when its state is being checked which should
    * happened on a regular interval.  This should represent an opportunity
    * flush the contents of the buffer if they have become to old.  
    */
   public abstract void check_state();
}
