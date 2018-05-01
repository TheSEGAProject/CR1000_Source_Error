/* ValueBool8.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Manages data declared as a array of eight bit flags in logger table data.
 */
public class ValueBool8 extends ValueUInt1
{
   protected ValueBool8(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }
   
   
   @Override 
   public String format()
   { return Integer.toBinaryString(to_int()); }
   
   
   @Override
   public Object clone()
   { return new ValueBool8(column_def,array_offset); }
}
