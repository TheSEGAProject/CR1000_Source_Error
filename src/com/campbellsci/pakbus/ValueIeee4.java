/* ValueIeee4.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines an object for managing four byte floating point data (IEEE754) from 
 * datalogger tables.
 */
public class ValueIeee4 extends ValueBase
{
   protected ValueIeee4(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }
      

   @Override
   public String format()
   { 
      return Utils.csi_float_to_string(
         to_float(), 
         7, 
         false, 
         false);
   } // format

   
   @Override
   public int raw_size()
   { return 4; }
      

   @Override
   public float to_float() throws UnsupportedOperationException
   {
      // we need to read an integer pattern from the buffer
      int float_pattern =
         ((record_buff[record_buff_offset + 0] << 24) & 0xff000000) |
         ((record_buff[record_buff_offset + 1] << 16) & 0x00ff0000) |
         ((record_buff[record_buff_offset + 2] <<  8) & 0x0000ff00) |
          (record_buff[record_buff_offset + 3]        & 0x000000ff);
      return Float.intBitsToFloat(float_pattern);
   } // to_float

   
   @Override
   public int to_int() throws UnsupportedOperationException
   { return (int)to_float(); }


   @Override
   public long to_long() throws UnsupportedOperationException
   { return (long)to_float(); }


   @Override
   public Object clone()
   { return new ValueIeee4(column_def,array_offset); }
}
