/* DataCollectModeDateToNewest.java

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
 * Defines a data collect mode controller that will query the datalogger
 * for records starting at a specified begin date and going up to and including
 * the newest record in the table.
 */
public class DataCollectModeDateToNewest extends DataCollectMode
{
   /**
    * Constructor
    * 
    * @param begin_date  Specifies the beginning date
    */
   public DataCollectModeDateToNewest(LoggerDate begin_date)
   {
      this.begin_date = begin_date;
      state = state_type.query_newest;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "DateToNewest(" + begin_date.toString() + ")"; }
   
   
   @Override
   public Packet get_next_command()
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
      else if(state == state_type.query_date)
      {
         rtn.add_byte((byte)7);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_nsec(begin_date);
         rtn.add_nsec(newest_record.get_time_stamp());
         table_def.format_column_request(rtn);
      }
      else if(state == state_type.collect_holes)
      {
         rtn.add_byte((byte)6);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_uint4(next_record_no);
         rtn.add_uint4(newest_record.get_record_no());
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
         if(records.isEmpty() || table_def.size == 1)
            state = state_type.complete;
         else
         {
            newest_record = records.get(records.size() - 1);
            records.clear();
            if(begin_date.compareTo(newest_record.get_time_stamp()) <= 0)
               state = state_type.query_date;
            else
               state = state_type.complete;   
         }
      }
      else if (state ==  state_type.query_date)
      {
         if(!records.isEmpty())
         {
            state = state_type.collect_holes;
            next_record_no = records.get(records.size() - 1).get_record_no() + 1;
            if(next_record_no >= newest_record.get_record_no())
               state = state_type.complete;
         }
         else
            state = state_type.complete;
         if(state == state_type.complete &&
            begin_date.compareTo(newest_record.get_time_stamp()) <= 0)
            records.add(newest_record);
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
         if(state == state_type.complete &&
            begin_date.compareTo(newest_record.get_time_stamp()) <= 0)
            records.add(newest_record);
      }
   } // on_response
   
   
   /**
    * Holds the begin date
    */
   private LoggerDate begin_date;
   
   
   /**
    * Holds the newest record
    */
   private Record newest_record;
   
   
   /**
    * Holds the next record number to query
    */
   private long next_record_no;
   
   
   /**
    * Defines the values for the state
    */
   private enum state_type
   {
      query_newest,
      query_date,
      collect_holes,
      complete
   }
   
   
   /**
    * Holds the current state of the transaction
    */
   private state_type state;
}
