/* FileInfo.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Monday 04 December 2006
   Last Change: Monday 04 December 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines and object that describes a file in a datalogger file system.   This
 * class is used by class {@link ListFilesTran}.
 */
public class FileInfo
{
   /**
    * Specifies the name of the file
    */
   protected String name;

   /**
    * Specifies the size of the file. Will be zero if the attribute is not
    * supported.
    */
   protected long size;

   /**
    * Specifies the last time that the file was updated.  Will be an empty
    * string if not supported.
    */
   protected String last_update_time;

   /**
    * Set to true if this file is the currently running program
    */
   protected boolean running_now;

   /**
    * Set to true of this file is a program marked to run on power up.
    */
   protected boolean run_on_power_up;

   /**
    * Set to true if this file is "read-only" meaning that it cannot be deleted
    * or replaced without stopping the current program
    */
   protected boolean read_only;

   /**
    * Set to true if this program is set to a "paused" state.
    */
   protected boolean paused;

   /**
    * @return The name of the file
    */
   public String get_name()
   { return name; }

   /**
    * @return The size of the file.  This value will be zero if the attribute
    * is not supported.
    */
   public long get_size()
   { return size; }

   /**
    * @return Returns the last update time.  Will be an empty string if the
    * attribute is not supported.
    */
   public String get_last_update_time()
   { return last_update_time; }

   /**
    * @return true if this file is marked as the program currently running on
    * datalogger
    */
   public boolean get_running_now()
   { return running_now; }

   /**
    * @return true if this file is the prgoram makred to run on power up.
    */
   public boolean get_run_on_power_up()
   { return run_on_power_up; }

   /**
    * @return true if this file is marked as "read-only" meaning that it cannot
    * be deleted or replaced unless the currently running program is stopped.
    */
   public boolean get_read_only()
   { return read_only; }

   /**
    * @return true if the file is a program file that is paused.
    */
   public boolean get_paused()
   { return paused; }
};

