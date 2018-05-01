/* DataCollectAllRecordsMode.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 18 October 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.List;

/**
 * Defines a data collection mode that gets all of the records available
 * in the table.
 */
public class DataCollectModeAllRecords extends DataCollectMode
{
   /**
    * Default Constructor
    */
   public DataCollectModeAllRecords()
   {
      state = state_type.collect_newest;
      next_record_no = 0xFFFFFFFF;
   } // constructor
   
   
   @Override
   public String get_name()
   { return "All Records"; }
   
   
   /**
    * Generates the next logger command based upon the state
    */
   @Override
   public Packet get_next_command()
   {
      Packet rtn = new Packet();
      rtn.protocol_type = Packet.protocol_bmp5;
      rtn.message_type = Packet.bmp5_collect_data_cmd;
      rtn.add_uint2(station.get_security_code());
      if(state == state_type.collect_newest)
      {
         rtn.add_byte((byte)5);
         rtn.add_uint2(table_def.table_no);
         rtn.add_uint2(table_def.def_sig);
         rtn.add_int4(1); // collect most recent record
         table_def.format_column_request(rtn);
      }
      else if(state == state_type.collect_holes && 
              next_record_no != newest_record.get_record_no())
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
    * Handles the response that comes in from the last command
    * 
    * @param records  Holds the list of records that were reported
    */
   @Override
   public void on_response(List<Record> records)
   {
      if(state == state_type.collect_newest)
      {
         // if there were no records or if the table has only one record, we are done.
         // Otherwise, we need to determine what records to collect
         if(!records.isEmpty() && table_def.size > 1)
         {
            // we will pull off the newest record so that it can be reported in sequence
            newest_record = records.get(0);
            records.clear();
            next_record_no = newest_record.get_record_no() - table_def.size;
            state = state_type.collect_holes;
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
               next_record_no = newest_record.get_record_no();
               records.add(newest_record);
            }
         }
         else
         {
            records.add(newest_record);
            next_record_no = newest_record.get_record_no();
         }
      }
   } // on_response
   
   
   /**
    * Defines the current state of this transaction
    */
   private enum state_type
   {
      collect_newest,
      collect_holes,
      complete
   }
   
   
   /**
    * Stores the current state of the transaction
    */
   private state_type state;
   
   
   /**
    * Stores the newest record that was collected
    */
   private Record newest_record;
   
   
   /**
    * Holds the next record number expected to be collected in the
    * collect_holes state.
    */
   private long next_record_no;
}
