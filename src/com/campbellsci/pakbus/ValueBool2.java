/* ValueBool2.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Manages data declared as two byte boolean in logger table records.
 */
public class ValueBool2 extends ValueUInt2
{
   protected ValueBool2(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }

   
   public Object clone()
   { return new ValueBool2(column_def,array_offset); }
}
