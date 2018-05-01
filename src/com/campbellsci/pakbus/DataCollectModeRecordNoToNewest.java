/* DataCollectModeRecordNoToNewest.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Tuesday 24 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;

import edu.nau.wnrl.NullNewestRecordException;


/**
 * Defines a data collection controller that implements collection
 * of all records starting at a specified record number and going up
 * to the newest.  If the specified begin does not exist, all records
 * in the table will be returned. 
 */
public class DataCollectModeRecordNoToNewest extends DataCollectMode
{
   /**
    * Constructor
    * 
    * @param begin_record_no  Specifies the beginning record number
    */
   public DataCollectModeRecordNoToNewest(
      long begin_record_no)
   {
      this.begin_record_no = begin_record_no;
      next_record_no = this.begin_record_no;
      state = state_type.query_newest;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "RecordNoToNewest(" + begin_record_no + ")"; }
   
   
   @Override
   public Packet get_next_command() throws NullNewestRecordException, Exception
   {
      Packet rtn = new Packet();
      rtn.protocol_type = Packet.protocol_bmp5;
      rtn.message_type = Packet.bmp5_collect_data_cmd;
      rtn.add_uint2(station.get_security_code());
      if(state == state_type.query_newest)
      {
         rtn.add_byte((byte)5);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_uint4((long)1);
         table_def.format_column_request(rtn);
      }
      else if(state == state_type.collect_holes)
      {
         rtn.add_byte((byte)6);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_uint4(next_record_no);
         if(newest_record != null)
         	rtn.add_uint4(newest_record.get_record_no());
         else
        	 throw new NullNewestRecordException();
         table_def.format_column_request(rtn);
      }
      else
         rtn = null;
      return rtn;
   } // get_next_command

   
   @Override
   public void on_response(List<Record> records)
   {
      if(state == state_type.query_newest)
      {
         if(!records.isEmpty())
            newest_record = records.get(0);
         if(table_def.size > 1)
         {
            records.clear();
            state = state_type.collect_holes;
         }
         else
            state = state_type.complete;
      }
      else if(state == state_type.collect_holes)
      {
         if(!records.isEmpty())
         {
            next_record_no = records.get(records.size() - 1).get_record_no() + 1;
            if(next_record_no >= newest_record.get_record_no())
               state = state_type.complete;
         }
         else
            state = state_type.complete;
         if(state == state_type.complete)
            records.add(newest_record);
      }
   } // on_response

   
   /**
    * Holds the begin record number
    */
   private long begin_record_no;
   
   
   /**
    * holds the newest record
    */
   private Record newest_record;
   
   
   /**
    * Holds the next record number
    */
   private long next_record_no;
   
   
   /**
    * lists the possible states for this transaction
    */
   private enum state_type
   {
      query_newest,
      collect_holes,
      complete
   }
   
   
   /**
    * Holds the state of this transaction
    */
   private state_type state;
}
