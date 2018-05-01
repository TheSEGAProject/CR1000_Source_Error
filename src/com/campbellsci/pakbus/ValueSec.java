/* ValueSec.java

   Copyright (C) 2007, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Thursday 22 March 2007
   Last Change: Thursday 22 March 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines an object that manages four byte timestamp (represented as seconds
 * since midnight, 1 January 1990) stored as timestamps or data in datalogger
 * table records.  This format is used by CR200 class dataloggers.
 */
public class ValueSec extends ValueBase
{
   public ValueSec(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }

   /**
    * Formats this value using the following pattern: "YYYY-MM-DD HH:MM:SS.nnn"
    */
   @Override
   public String format()
   {
      LoggerDate stamp = new LoggerDate(to_long() * LoggerDate.nsec_per_sec);
      return stamp.format("%Y-%m-%d %H:%M:%S%x");
   }

   @Override
   public int raw_size()
   { return 4; }


   @Override
   public float to_float() throws UnsupportedOperationException
   { return to_int(); }

   
   @Override
   public int to_int() throws UnsupportedOperationException
   {
      int seconds =
         ((record_buff[record_buff_offset + 0] << 24) & 0xFF000000) |
         ((record_buff[record_buff_offset + 1] << 16) & 0x00FF0000) |
         ((record_buff[record_buff_offset + 2] <<  8) & 0x0000FF00) |
          (record_buff[record_buff_offset + 3]        & 0x000000FF);
      return seconds;
   }

   @Override
   public long to_long() throws UnsupportedOperationException
   { return to_int(); }

   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueSec(column_def,array_offset); }
}
