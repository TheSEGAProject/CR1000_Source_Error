/* Timer.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 22 September 2006
   Last Change: Friday 22 September 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;
import java.util.*;


/**
 * Defines an object that implements a passive timer.  The timer can be initialised and then, at
 * later times, can be polled to determine the amount of time that has elapsed.
 */
final public class Timer
{
   private int start;

   /**
    * Initialises the timer so that all future references are relative to the current time
    */
   public Timer()
   { reset(); }
   

   /**
    * Resets the timer so that all future references derive from this time.
    */
   public void reset()
   { start = msec_in_day(); }


   /**
    * Returns the amount of time that has elapsed since construction (or the last reset) in units of
    * milli-seconds.
    */
   public int elapsed()
   { return diff(start, msec_in_day()); }


   /**
    * Calculates the number of milli-seconds into the current day.
    */
   private static final int msec_per_day = 86400000;
   public static int msec_in_day()
   { return (int)((new Date()).getTime() % msec_per_day); }


   /**
    * Calculates the difference between two time measurements.
    */
   private static int diff(int t1, int t2)
   {
      int rtn = t2 - t1;
      if(t2 < t1)
         rtn += msec_per_day;
      return rtn;
   }
};

