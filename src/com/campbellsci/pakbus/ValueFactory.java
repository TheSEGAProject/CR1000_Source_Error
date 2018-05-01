/* ValueFactory.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Wednesday 18 February 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

/**
 * Defines an object that allocates value objects for created logger table records.
 * This class can be extended to deal with custom value types or it can be used as
 * is.
 */
public class ValueFactory
{
   /**
    * Responsible for allocating a value object based upon the 
    * provided column definition and array offset.
    * 
    * @param  column_def Specifies the column definition
    * @param  array_offset Specifies the linear array offset for the value to be created.
    */
   public ValueBase make_value(
      ColumnDef column_def,
      int array_offset)
   {
      ValueBase rtn = null;
      switch(column_def.data_type)
      {
      case ColumnDef.type_ascii:
         rtn = new ValueAscii(column_def,array_offset);
         break;
         
      case ColumnDef.type_bool:
         rtn = new ValueBool(column_def,array_offset);
         break;
         
      case ColumnDef.type_bool2:
         rtn = new ValueBool2(column_def,array_offset);
         break;
         
      case ColumnDef.type_bool4:
         rtn = new ValueBool4(column_def,array_offset);
         break;
         
      case ColumnDef.type_bool8:
         rtn = new ValueBool8(column_def,array_offset);
         break;
         
      case ColumnDef.type_fp2:
         rtn = new ValueFp2(column_def,array_offset);
         break;
         
      case ColumnDef.type_fp4:
         // we'll ignore FP4 for now since it is used only by the CR10X-PB
         break;
         
      case ColumnDef.type_ieee4:
         rtn = new ValueIeee4(column_def,array_offset);
         break;
         
      case ColumnDef.type_ieee4_lsf:
         rtn = new ValueIeee4Lsf(column_def,array_offset);
         break;
         
      case ColumnDef.type_ieee8:
         rtn = new ValueIeee8(column_def,array_offset);
         break;
         
      case ColumnDef.type_ieee8_lsf:
         rtn = new ValueIeee8Lsf(column_def,array_offset); 
         break;
         
      case ColumnDef.type_int1:
         rtn = new ValueInt1(column_def,array_offset);
         break;
         
      case ColumnDef.type_int2:
         rtn = new ValueInt2(column_def,array_offset);
         break;
         
      case ColumnDef.type_int4:
         rtn = new ValueInt4(column_def,array_offset);
         break;
         
      case ColumnDef.type_uint1:
         rtn = new ValueUInt1(column_def,array_offset);
         break;
         
      case ColumnDef.type_uint2:
         rtn = new ValueUInt2(column_def,array_offset);
         break;
         
      case ColumnDef.type_uint4:
         rtn = new ValueUInt4(column_def,array_offset);
         break;
         
      case ColumnDef.type_int2_lsf:
         rtn = new ValueInt2Lsf(column_def,array_offset);
         break;

      case ColumnDef.type_uint2_lsf:
         rtn = new ValueUInt2Lsf(column_def, array_offset);
         break;
         
      case ColumnDef.type_int4_lsf:
         rtn = new ValueInt4Lsf(column_def,array_offset);
         break;

      case ColumnDef.type_uint4_lsf:
         rtn = new ValueUInt4Lsf(column_def, array_offset);
         break;
         
      case ColumnDef.type_nsec:
         rtn = new ValueNSec(column_def,array_offset);
         break;
         
      case ColumnDef.type_nsec_lsf:
         rtn = new ValueNSecLsf(column_def,array_offset);
         break;
         
      case ColumnDef.type_sec:
         rtn = new ValueSec(column_def,array_offset);
         break;
         
      case ColumnDef.type_usec:
         break;
      }
      return rtn;
   } // make_value
}
