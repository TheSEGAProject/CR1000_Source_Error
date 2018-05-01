/* ValueIeee8Lsf.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines an object that manages eight byte floating point (IEEE754) values
 * stored with the least significant byte first (little endian) from datalogger 
 * table records.
 */
public class ValueIeee8Lsf extends ValueBase
{
   protected ValueIeee8Lsf(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }
      

   @Override
   public String format()
   { 
      return Utils.csi_float_to_string(
         to_float(), 
         15, 
         false, 
         false);
   } // format

   
   @Override
   public int raw_size()
   { return 8; }
      

   @Override
   public float to_float() throws UnsupportedOperationException
   { return (float)to_double(); }
   
   
   @Override
   public double to_double() throws UnsupportedOperationException
   {
      long float_pattern =
         ((record_buff[record_buff_offset + 7] << 56) & 0xff00000000000000L) |
         ((record_buff[record_buff_offset + 6] << 48) & 0x00ff000000000000L) |
         ((record_buff[record_buff_offset + 5] << 36) & 0x0000ff0000000000L) |
         ((record_buff[record_buff_offset + 4] << 32) & 0x000000ff00000000L) |
         ((record_buff[record_buff_offset + 3] << 24) & 0x00000000ff000000L) |
         ((record_buff[record_buff_offset + 2] << 16) & 0x0000000000ff0000L) |
         ((record_buff[record_buff_offset + 1] <<  8) & 0x000000000000ff00L) |
          (record_buff[record_buff_offset + 0]        & 0x00000000000000ffL);
      return Double.longBitsToDouble(float_pattern);
   } // to_double

   
   @Override
   public int to_int() throws UnsupportedOperationException
   { return (int)to_float(); }


   @Override
   public long to_long() throws UnsupportedOperationException
   { return (long)to_float(); }


   @Override
   public Object clone()
   { return new ValueIeee8Lsf(column_def,array_offset); }
}
