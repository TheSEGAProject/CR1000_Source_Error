/* ValueUInt2.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines an object that manages two byte unsigned integer values from
 * datalogger table records.
 */
public class ValueUInt2 extends ValueBase
{
   protected ValueUInt2(
      ColumnDef column_def,
      int array_offset)
   { super(column_def, array_offset); }
      

   @Override
   public String format()
   { return Integer.toString(to_int()); }
      

   @Override
   public int raw_size()
   { return 2; }
     

   @Override
   public int to_int() throws UnsupportedOperationException
   {
      return 
         ((record_buff[record_buff_offset + 0] << 8) & 0xff00) |
          (record_buff[record_buff_offset + 1])      & 0x00ff;
   }


   @Override
   public float to_float() throws UnsupportedOperationException
   { return to_int(); }
      

   @Override
   public long to_long() throws UnsupportedOperationException
   { return to_int(); }
      

   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueUInt2(column_def,array_offset); }
      
}
