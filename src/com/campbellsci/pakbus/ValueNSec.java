/* ValueNSec.java

   Copyright (C) 2007, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 21 March 2007
   Last Change: Wednesday 02 May 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;



/**
 * Defines a value that represents a datalogger time-stamp encoded as two four
 * byte integers which record seconds elapsed since midnight 1 January 1990 and
 * nano-seconds into the second.  
 */
class ValueNSec extends ValueBase
{
   /**
    * Constructor
    */
   protected ValueNSec(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }

   public int raw_size()
   { return 8; }

   
   /**
    * Formats this value using the following pattern: "YYYY-MM-DD HH:MM:SS.nnn"
    */
   @Override
   public String format()
   {
      LoggerDate stamp = new LoggerDate(to_long());
      return stamp.format("%Y-%m-%d %H:%M:%S%x");
   }


   /**
    * Converts to seconds since midnight, 1 January 1990
    */
   public int to_int() throws UnsupportedOperationException
   {
      int seconds =
         ((record_buff[record_buff_offset + 0] << 24) & 0xFF000000) |
         ((record_buff[record_buff_offset + 1] << 16) & 0x00FF0000) |
         ((record_buff[record_buff_offset + 2] <<  8) & 0x0000FF00) |
          (record_buff[record_buff_offset + 3]        & 0x000000FF);
      return seconds;
   }


   /**
    * Converts to nano-seconds since midnight 1 January 1990
    */
   public long to_long() throws UnsupportedOperationException
   {
      long seconds = to_int();
      long nsec =
         ((record_buff[record_buff_offset + 4] << 24) & 0xFF000000) |
         ((record_buff[record_buff_offset + 5] << 16) & 0x00FF0000) |
         ((record_buff[record_buff_offset + 6] <<  8) & 0x0000FF00) |
          (record_buff[record_buff_offset + 7]        & 0x000000FF);
      return seconds * LoggerDate.nsec_per_sec + nsec; 
   }


   /**
    * Converts to seconds since midnight 1 January 1990
    */
   public float to_float() throws UnsupportedOperationException
   { return to_int(); }


   /**
    * Converts to nan-seconds since midnight 1 January 1990
    */
   public double to_double() throws UnsupportedOperationException
   { return to_long(); }

   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueNSec(column_def,array_offset); }
};
