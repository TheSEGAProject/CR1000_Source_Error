/* ValueFp2.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines an object that manages two byte floating point data (decimal 
 * encoded) from data logger table data.
 */
public class ValueFp2 extends ValueBase
{
   protected ValueFp2(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }
      

   @Override
   public String format()
   {
      StringBuilder rtn = new StringBuilder();
      int b0 = record_buff[record_buff_offset] & 0xff;
      int b1 = record_buff[record_buff_offset + 1] & 0xff;
      if(b0 == 0x9f && b1 == 0xfe)
         rtn.append("NAN");
      else if(b0 == 0x9f && b1 == 0xff)
         rtn.append("-INF");
      else if(b0 == 0x1f && b1 == 0xff)
         rtn.append("INF");
      else
      {
         int mantissa = ((b0 & 0x1f) << 8) | b1;
         boolean is_negative = (b0 & 0x80) != 0;
         int decimal_locator = (b0 & 0x60) >> 5;
         rtn.append(mantissa);
         while(rtn.length() < decimal_locator)
            rtn.insert(0, '0');
         if(decimal_locator > 0)
            rtn.insert(rtn.length() - decimal_locator,'.');
         if(is_negative && mantissa != 0)
            rtn.insert(0,'-');
      }
      return rtn.toString();
   } // format
   

   @Override
   public int raw_size()
   { return 2; }
      

   @Override
   public float to_float() throws UnsupportedOperationException
   { return Utils.csi_string_to_float(format()).floatValue(); }
      
   
   @Override
   public int to_int() throws UnsupportedOperationException
   { return Utils.csi_string_to_float(format()).intValue(); }
      

   @Override
   public long to_long() throws UnsupportedOperationException
   { return Utils.csi_string_to_float(format()).longValue(); }
      

   @Override
   public Object clone()
   { return new ValueFp2(column_def,array_offset); }
}
