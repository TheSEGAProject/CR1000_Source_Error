/* ValueInt1.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines an object for managing single byte signed integer data from datalogger
 * table records.
 */
public class ValueInt1 extends ValueBase
{
   protected ValueInt1(
      ColumnDef column_def,
      int array_offset)
   { super(column_def,array_offset); }
      

   @Override
   public String format()
   { return Integer.toString(to_int()); }
      

   @Override
   public int raw_size()
   { return 1; }
     

   @Override
   public int to_int() throws UnsupportedOperationException
   { return record_buff[record_buff_offset]; }


   @Override
   public float to_float() throws UnsupportedOperationException
   { return to_int(); }


   @Override
   public long to_long() throws UnsupportedOperationException
   { return to_int(); }
      
   
   @Override
   protected Object clone() throws CloneNotSupportedException
   { return new ValueInt1(column_def, array_offset); }
}
