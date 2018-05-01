/* ValueAscii.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines a value class for managing table string data 
 */
public class ValueAscii extends ValueBase
{
   protected ValueAscii(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }
   
   
   @Override
   public String format()
   {
      StringBuilder rtn = new StringBuilder();
      for(int i = 0; 
          i < column_def.dims.back() && record_buff[i + record_buff_offset] != 0; 
          ++i)
         rtn.append((char)record_buff[i + record_buff_offset]);
      return rtn.toString();
   }

   @Override
   public int raw_size()
   { return column_def.dims.back(); }
      

   @Override
   public float to_float() throws UnsupportedOperationException
   {
      float rtn = 0;
      try
      {
         rtn = Float.parseFloat(format());
      }
      catch(NumberFormatException e)
      { throw new UnsupportedOperationException(); }
      return rtn;
   }

   @Override
   public int to_int() throws UnsupportedOperationException
   {
      int rtn = 0;
      try
      {
         rtn = Integer.parseInt(format());
      }
      catch(NumberFormatException e)
      { throw new UnsupportedOperationException(); }
      return rtn;
   }

   @Override
   public long to_long() throws UnsupportedOperationException
   {
      long rtn = 0;
      try
      {
         rtn = Long.parseLong(format());
      }
      catch(NumberFormatException e)
      { throw new UnsupportedOperationException(); }
      return rtn;
   } // to_long


   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueAscii(column_def, array_offset); }
}
