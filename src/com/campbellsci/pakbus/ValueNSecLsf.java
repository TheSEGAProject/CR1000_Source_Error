/* ValueNSecLsf.java

   Copyright (C) 2007, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 21 March 2007
   Last Change: Wednesday 02 May 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;



/**
 * Defines a value that represents a datalogger time-stamp as two four byte
 * little-endian (least significant byte first) integers that records seconds
 * elasped since midnight 1 January 1990 and nano-seconds into the second.
 */
class ValueNSecLsf extends ValueNSec
{
   /**
    * Constructor
    */
   ValueNSecLsf(ColumnDef column_def, int array_offset)
   { super(column_def, array_offset); }


   /**
    * Converts to seconds since midnight 1 Januuary 1990.
    */
   @Override
   public int to_int() throws UnsupportedOperationException
   {
      int seconds =
         ((record_buff[record_buff_offset + 3] << 24) & 0xFF000000) |
         ((record_buff[record_buff_offset + 2] << 16) & 0x00FF0000) |
         ((record_buff[record_buff_offset + 1] <<  8) & 0x0000FF00) |
          (record_buff[record_buff_offset + 0]        & 0x000000FF);
      return seconds;
   }


   /**
    * converts to nano-seconds since midnight, 1 January 1990
    */
   @Override
   public long to_long() throws UnsupportedOperationException
   {
      long seconds = to_int();
      long nsec =
         ((record_buff[record_buff_offset + 7] << 24) & 0xFF000000) |
         ((record_buff[record_buff_offset + 6] << 16) & 0x00FF0000) |
         ((record_buff[record_buff_offset + 5] <<  8) & 0x0000FF00) |
          (record_buff[record_buff_offset + 4]        & 0x000000FF);
      return nsec * LoggerDate.nsec_per_sec + seconds;
   } // to_long
};

