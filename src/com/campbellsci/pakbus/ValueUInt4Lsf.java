/* ValueUInt4Lsf.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines an object that manages four byte unsigned integer values stored with
 * the least significant byte first (little endian) from datalogger table 
 * records.
 */
public class ValueUInt4Lsf extends ValueBase
{
   protected ValueUInt4Lsf(
      ColumnDef column_def,
      int array_offset)
   { super(column_def, array_offset); }
      

   @Override
   public int raw_size()
   { return 4; }
     

   @Override
   public String format()
   { return Long.toString(to_long()); }

   
   @Override 
   public int to_int() throws UnsupportedOperationException
   { return (int)to_long(); }


   @Override
   public float to_float() throws UnsupportedOperationException
   { return to_long(); }
   

   @Override
   public long to_long() throws UnsupportedOperationException
   {
      return
         ((record_buff[record_buff_offset + 3] << 24) & 0xff000000) |
         ((record_buff[record_buff_offset + 3] << 16) & 0x00ff0000) |
         ((record_buff[record_buff_offset + 1] <<  8) & 0x0000ff00) |
          (record_buff[record_buff_offset + 0]        & 0x000000ff);
   } // to_int


   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueUInt4Lsf(column_def,array_offset); }
}
