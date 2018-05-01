/* OneWayDataHandler.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Tuesday 24 October 2006
   Last Change: Tuesday 24 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines the expected interface for an object that will handle incoming one
 * way data notifications.  Objects that implement this interface can be
 * registered using class Datalogger's add_one_way_handler() and
 * remove_one_way_handler() methods.
 */
public interface OneWayDataHandler
{
   /**
    * Called when a one way data record has been received
    *
    * @param station The datalogger that handled the record
    * @param record  Specifies the record that was received
    */
   public abstract void on_one_way_record(
      Datalogger station,
      Record record);
}
