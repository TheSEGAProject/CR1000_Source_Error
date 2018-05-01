/* DataCollectModeDateRange.java

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
 * Defines a data collect mode controller that implements the query
 * to collect a range of data from a table by time stamp starting
 * at a specified begin date and going up, but not including, an end date. 
 */
public class DataCollectModeDateRange extends DataCollectMode
{
   /**
    * Constructor
    * 
    * @param begin_date  Specifies the beginning date.  The first record in
    * the response set will have a time stamp that is either the closest possible
    * or equal to this value
    * @param end_date    Specifies the end date.  This collect mode will collect
    * records until a time stamp greater than or equal to this end date is found. 
    */
   public DataCollectModeDateRange(
      LoggerDate begin_date,
      LoggerDate end_date)
   {
      this.begin_date = begin_date;
      this.end_date = end_date;
      next_record_no = 0xffffffff;
      state = state_type.query_newest;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "DateRange(" + begin_date.toString() + ", " + end_date.toString() + ")"; }
   /**
    * Overrides the base class version to generate the next command for this 
    * transaction.  This will be done based upon the internal state of this 
    * controller.  If the transaction is complete, the return value from 
    * this method will be null.
    * 
    * @return the next command to send or null
    */
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
         rtn.add_nsec(end_date);
         table_def.format_column_request(rtn);
      }
      else if(state == state_type.collect_holes && 
              next_record_no < newest_record.get_record_no())
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

   
   /**
    * Overrides the base class method to handle the response
    * from the last query.
    * 
    * @param records  Specifies the records the logger sent
    * in response to the last command.
    */
   @Override
   public void on_response(List<Record> records)
   {
      if(state == state_type.query_newest)
      {
         if(!records.isEmpty())
            newest_record = records.get(0);
         if(!records.isEmpty() && table_def.size > 1)
         {
            records.clear();
            state = state_type.query_date;
         }
         else
         {
            if(newest_record != null && 
               (begin_date.compareTo(newest_record.get_time_stamp()) < 0 ||
                end_date.compareTo(newest_record.get_time_stamp()) >= 0))
               records.clear();
            state = state_type.complete;
         }
      }
      else if(state == state_type.query_date)
      {
         // the datalogger will have applied the criteria for the date ranges.
         // we need to know if the newest record was included in the response set
         boolean includes_newest = false;
         for(Record record: records)
         {
            if(record.get_record_no() == newest_record.get_record_no())
            {
               includes_newest = true;
               break;
            }
         }
         if(includes_newest || records.isEmpty())
            state = state_type.complete;
         else
         {
            state = state_type.collect_holes;
            next_record_no = records.get(records.size() - 1).get_record_no() + 1;
            if(next_record_no >= newest_record.get_record_no())
               state = state_type.complete;
         }    
      }
      else if(state == state_type.collect_holes)
      {
         // we need to eliminate any records that are outside of the query range
         int index = 0;
         while(index < records.size())
         {
            Record record = records.get(index);
            if(begin_date.compareTo(record.get_time_stamp()) < 0 ||
               end_date.compareTo(record.get_time_stamp()) >= 0)
            {
               records.remove(index);
               state = state_type.complete;
            }
            else
               ++index;
         }
         
         // if there are no records left, we are done
         if(state != state_type.complete)
         {
            if(records.isEmpty())
               state = state_type.complete;
            else
            {
               next_record_no = records.get(records.size() - 1).get_record_no() + 1;
               if(next_record_no >= newest_record.get_record_no())
                  state = state_type.complete;
            }
         }
         
         // if we are done, we need to determine whether the newest record should
         // be added to the response set.
         if(state == state_type.complete &&
            begin_date.compareTo(newest_record.get_time_stamp()) >= 0 &&
            end_date.compareTo(newest_record.get_time_stamp()) < 0)
            records.add(newest_record);
      }
   } // on_response

   
   /**
    * Stores the begin date
    */
   private LoggerDate begin_date;
   
   
   /**
    * Stores the end date
    */
   private LoggerDate end_date;
   
   
   /**
    * Stores the next record number to query
    */
   private long next_record_no;
   
   
   /**
    * Stores the newest record queried when this transaction began
    */
   private Record newest_record;
   
   
   /**
    * possible values for the state of this controller
    */
   private enum state_type
   {
      query_newest,
      query_date,
      collect_holes,
      complete
   }
   
   
   /**
    * Holds the state of this transaction
    */
   private state_type state;
}
