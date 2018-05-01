/* DataCollectModeRecordNoRange.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Tuesday 24 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;


/**
 * Defines a data collection transaction controller that implements
 * a query by record number range.  This query will collect all records
 * in the logger that have a record number greater than the begin record 
 * number and less than the end record number. 
 */
public class DataCollectModeRecordNoRange extends DataCollectMode
{
   /**
    * Constructor
    * 
    * @param begin_record_no  Specifies the beginning record number
    * @param end_record_no    Specifies the end record number.  The transaction
    * will collect up to, but not include this record number
    */
   public DataCollectModeRecordNoRange(
      long begin_record_no,
      long end_record_no)
   {
      this.begin_record_no = begin_record_no;
      this.end_record_no = end_record_no;
      next_record_no = this.begin_record_no;
      state = state_type.collect_holes;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "RecordNoRange(" + begin_record_no + ", " + end_record_no + ")"; }
   
   
   @Override
   public Packet get_next_command()
   {
      Packet rtn = new Packet();
      rtn.protocol_type = Packet.protocol_bmp5;
      rtn.message_type = Packet.bmp5_collect_data_cmd;
      rtn.add_uint2(station.get_security_code());
      if(state == state_type.collect_holes)
      {
         rtn.add_byte((byte)6);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_uint4(next_record_no);
         rtn.add_uint4(end_record_no);
         table_def.format_column_request(rtn);
      }
      else
         rtn = null;
      return rtn;
   }

   
   @Override
   public void on_response(List<Record> records)
   {
      if(state == state_type.collect_holes)
      {
         if(!records.isEmpty())
         {
            next_record_no = records.get(records.size() - 1).get_record_no() + 1;
            if(next_record_no >= end_record_no)
               state = state_type.complete;
         }
         else
            state = state_type.complete;
      }
   } // on_response
   
   
   /**
    * Holds the beginning record number
    */
   private long begin_record_no;
   
   
   /**
    * Holds the end record number
    */
   private long end_record_no;
   
   
   /**
    * Holds the next record number
    */
   private long next_record_no;
   
   
   /**
    * Describes the possible states
    */
   private enum state_type
   {
      collect_holes,
      complete
   }
   
   
   /**
    * Holds the state of the transaction
    */
   private state_type state; 
}
