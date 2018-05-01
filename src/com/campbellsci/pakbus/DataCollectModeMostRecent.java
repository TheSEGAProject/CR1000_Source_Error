/* DataCollectModeMostRecent.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 19 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;


/**
 * Defines a data collection transaction controller that collects the
 * most recent records from the datalogger. 
 */
public class DataCollectModeMostRecent extends DataCollectMode
{
   /**
    * Constructor
    * 
    * @param records_count  Specifies the number of records to poll
    */
   public DataCollectModeMostRecent(int records_count)
   {
      this.records_count = records_count;
      state = state_type.query_newest;
      next_record_no = 0xffffffff;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "MostRecent(" + records_count + ")"; }
   
   
   @Override
   public Packet get_next_command()
   {
      Packet rtn = new Packet();
      rtn.protocol_type = Packet.protocol_bmp5;
      rtn.message_type = Packet.bmp5_collect_data_cmd;
      if(state == state_type.query_newest)
      {
         rtn.add_uint2(station.get_security_code());
         rtn.add_byte((byte)5);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_uint4((long)1);
         table_def.format_column_request(rtn);
      }
      else if(state == state_type.collect_holes && 
              next_record_no < newest_record.get_record_no())
      {
         rtn.add_uint2(station.get_security_code());
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
    * Overloads the base class version to handle the records that came in response 
    * to the last command
    * 
    * @param records  The set of records that came in response to the last command
    */
   @Override
   public void on_response(List<Record> records)
   {
      if(state == state_type.query_newest)
      {
         if(!records.isEmpty() && records_count > 1 && table_def.size > 1)
         {
            int records_to_poll = Math.min(records_count, table_def.size);
            newest_record = records.get(0);
            next_record_no = newest_record.get_record_no() - records_to_poll + 1;
            state = state_type.collect_holes;
            records.clear();
         }
         else
            state = state_type.complete;
      }
      else
      {
         if(!records.isEmpty())
         {
            next_record_no = records.get(records.size() - 1).get_record_no() + 1;
            if(next_record_no >= newest_record.get_record_no())
            {
               records.add(newest_record);
               state = state_type.complete;
            }
         }
         else
         {
            records.add(newest_record);
            state = state_type.complete;
         }
      }
   } // on_response

   
   /**
    * Holds the newest record
    */
   private Record newest_record;
   
   
   /**
    * The following values are used to describe the state of the transaction
    */
   private enum state_type
   { 
      query_newest,
      collect_holes,
      complete
   }
   
   
   /**
    * Holds the next record number to collect
    */
   private long next_record_no;
   
   
   /**
    * Stores the state for this transaction
    */
   private state_type state;
   
   
   /**
    * Holds the number of records to poll
    */
   private int records_count;
}
