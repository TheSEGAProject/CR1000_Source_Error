/* Tabledef.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 11 October 2006
   Last Change: Saturday 12 April 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;



/**
 * Defines an object that holds the meta-data for one table on a Campbell
 * Scientific datalogger.  This information includes the name of the table, its
 * size, interval, and column information.
 */
public class TableDef
{
   /**
    * Specifies the order in which this table was read from the logger table
    * definitions.  This index is one-based.
    */
   public int table_no;


   /**
    * Specifies the name of this table
    */
   public String name;


   /**
    * Specifies the number of records that can be stored in the datalogger
    * table.  This value is useful for calculating what records are in the
    * datalogger table once the newest record has been received.
    */
   public int size;


   /**
    * Specifies the data type that is used to record time stamps for this
    * table.   This value will be one of ColumnDef.type_nsec,
    * ColumnDef.type_nsec_lsf, ColumnDef.type_sec, or ColumnDef.type_usec.
    */
   public byte time_type;


   /**
    * Specifies the output interval for this table in units of nano-seconds.
    * This value will be zero if the table is interval driven.
    */
   public long interval;


   /**
    * Specifies the signature of the table definition as calculated based upon
    * what was sent by the datalogger.
    */
   public int def_sig;


   /**
    * Defines the columns of this table
    */
   public List<ColumnDef> columns;
   
   
   /**
    * Reference to the station to which this table belongs
    */
   public Datalogger station;


   /**
    * This constructor can be called when a table definition is being built 
    * within the program.
    */
   protected TableDef(
      String table_name_,
      Datalogger station_)
   {
      name = table_name_;
      station = station_;
      size = 1;
      time_type = ColumnDef.type_nsec;
      columns = new Vector<ColumnDef>();
      record_cache = new LinkedList<Record>();
      is_subset = false;
   } // default constructor
   
   
   /**
    * This constructor is meant to be called by the datalogger when table
    * definitions are being read.
    *
    * @param table_no_  Specifies the logger assigned number for this table.
    * @param message    A packet that holds the table definitions from this
    * @param station_   Specifies the station to which this table belongs
    * datalogger.
    */
   protected TableDef(
      int table_no_,
      Packet message,
      Datalogger station_) throws Exception
   {
      // we need to process the initial stuff in the table definition
      long interval_sec;
      long interval_nsec;
      int table_start_pos = message.get_read_index();
      
      station = station_;
      table_no = table_no_;
      name = message.read_string();
      size = message.read_int4();
      time_type = message.read_byte();
      message.move_past(8);     // we'll ignore the time into the interval
      interval_sec = message.read_int4();
      interval_nsec = message.read_int4();
      interval = (interval_sec * 1000000000) + interval_nsec;
      is_subset = false;

      // we can now process all of the column definitions
      byte field_type = message.read_byte();
      int column_no = 1;

      columns = new Vector<ColumnDef>();
      while(field_type != 0)
      {
         ColumnDef column = new ColumnDef(column_no++,field_type,message);
         columns.add(column);
         field_type = message.read_byte();
      }

      // create the record cache
      record_cache = new LinkedList<Record>();
      
      // we now can calculate the signature of the table definition.  In order
      // to do this, we need to extract the portion of the message that begins
      // at the starting position.
      byte[] table_contents = message.get_fragment(
         table_start_pos,
         message.get_read_index() - 1);
      def_sig = Utils.calc_sig(
         table_contents,
         table_contents.length); 
   } // constructor
   
   
   /**
    * Creates a new table definition structure that represents a sub-set of the
    * column pieces represented by this table.  
    * 
    *  @param column_names  Specifies the list of column names that will be selected
    *  for the new table.  If this parameter is an empty list, all columns from this
    *  source object will be copied in the new table def.
    *  @return the new table definition object.
    * @throws Exception 
    */
   TableDef make_subset(List<String> column_names) throws Exception
   {
      // copy all of the information that applies to both structures.
      TableDef rtn = new TableDef(name,station);
      rtn.size = size;
      rtn.table_no = table_no;
      rtn.def_sig = def_sig;
      rtn.record_cache = new LinkedList<Record>();
      rtn.time_type = time_type;
      rtn.interval = interval;
      rtn.is_subset = true;
      
      // if the set of column names is empty, we will simply copy the names from this, otherwise,
      // we will generate a new set of columns
      if(column_names == null || column_names.isEmpty())
      {
         rtn.columns = columns;
         rtn.is_subset = true;
      }
      else
      {
         rtn.columns = new Vector<ColumnDef>();
         
         // we now need to select the columns for the new table definition.  In order to avoid 
         // duplicate selections, we will keep track of the column piece numbers that have already
         // been selected.
         Set<Integer> selected_piece_numbers = new HashSet<Integer>();
         for(String column_name: column_names)
         {
            // we need to find the piece, if any that contains the specified column name
            ValueName vname = new ValueName(column_name);
            boolean column_added = false;
            for(ColumnDef column: columns)
            {
               if(column.name.equalsIgnoreCase(vname.get_column_name()))
               {
                  int array_index = column.to_linear_index(vname.get_array_address());
                  if(array_index >= column.begin_index && 
                     array_index <= column.begin_index + column.piece_size)
                  {
                     column_added = true;
                     if(!selected_piece_numbers.contains(column.column_no))
                     {
                        selected_piece_numbers.add(column.column_no);
                        rtn.columns.add(column);
                        break;
                     }
                  }
               }
            }
            if(!column_added)
               throw new Exception("Invalid column name: " + column_name);
         }
      }
      return rtn;
   }
   
   
   /**
    * @return The number of values contained in this record
    */
   public int get_values_count()
   { 
      int rtn = 0;
      for(ColumnDef column: columns)
         rtn += column.get_values_count();
      return rtn;
   } // get_values_count
   
   
   /**
    * @return the number of bytes that will be needed for a record
    * from this table
    */
   public int get_record_size()
   {
      int rtn = 0;
      for(ColumnDef column: columns)
         rtn += column.get_values_size();
      return rtn;
   }
   
   
   /**
    * Adds the specified record to the cache
    * 
    * @param record  The record to be added
    */
   public void cache_record(Record record)
   { record_cache.add(record); }
   
   
   /**
    * Either generates a completely new record or returns a record
    * object from the cache.
    */
   public Record make_record()
   {
      Record rtn = null;
      if(record_cache.isEmpty())
         rtn = new Record(this);
      else
      {
         rtn = record_cache.get(0);
         record_cache.remove(0);
      }
      return rtn;
   } // make_record
   
   
   /**
    * @return the number of bytes occupied by the time stamp
    */
   public int get_time_stamp_size()
   { return ColumnDef.data_type_size(time_type); }
   
   
   /**
    * Calculates the number of bytes that will be needed to store a block
    * of the specified number of records.  This takes into account the space
    * needed for the time stamp as well.
    * 
    * @param records_count  The number of records in the block
    * @return the number of bytes needed for the specified number of records
    */
   public int get_native_block_size(int records_count)
   {
      int rtn = get_record_size() * records_count;
      if(interval == 0)
         rtn += get_time_stamp_size() * records_count;
      else
         rtn += get_time_stamp_size();
      return rtn;
   } // get_native_block_size


   /**
    * Handles a one way data fragment for this table.  Any records that result
    * from this fragment will be stored in the records container.
    *
    * @param records  The container that will receive any decoded records
    * @param message  The message to be handled
    */
   protected void on_one_way_data_fragment(
      List<Record> records,
      Packet message) throws Exception
   {
      // we will ignore anything with the wrong signature
      int frag_sig = message.read_uint2();
      if(frag_sig == def_sig)
      {
         // read the header of the message
         long begin_record_no = message.read_uint4();
         int records_count = message.read_uint2();
         boolean partial_fragment = (records_count & 0x8000) != 0;
         int record_offset = 0;

         if(partial_fragment)
         {
            record_offset = ((records_count & 0x7fff) << 16) | message.read_uint2();
            records_count = 1;
         }
         

         // this fragment must line up with previous fragments received
         if(one_way_buffer == null)
            one_way_buffer = new Packet();
         if(begin_record_no != last_one_way_record_no)
         {
            one_way_buffer.clear();
            last_one_way_record_no = begin_record_no;
         }
         if(record_offset == one_way_buffer.whats_left())
         {
            int frag_size = message.whats_left();
            one_way_buffer.add_bytes(
               message.read_bytes(frag_size),
               frag_size);
            if(one_way_buffer.whats_left() >= get_native_block_size(records_count))
            {
               read_records(
                  one_way_buffer,
                  records,
                  begin_record_no,
                  records_count);
               one_way_buffer.clear();
            }
         }
         else
            one_way_buffer.clear();
      }
   } // on_one_way_data_fragment
   

   /**
    * Reads the message buffer into the provided record collection.
    *
    * @param message  Specifies the message from which the record(s) will be
    * read
    * @param records  The list container that will hold the resulting records
    * @param begin_record_no  Specifies the record number for the first record
    * @param count    Specifies the number of records to be read
    * @return the number of records read
    */
   protected int read_records(
      Packet message,
      List<Record> records,
      long begin_record_no,
      int count) throws Exception
   {
      LoggerDate record_stamp = null;
      for(int i = 0; i < count; ++i)
      {
         if(i == 0 || interval == 0)
         {
            switch(time_type)
            {
            case ColumnDef.type_nsec:
               record_stamp = message.read_nsec();
               break;
               
            case ColumnDef.type_nsec_lsf:
               record_stamp = message.read_nsec_lsf();
               break;
               
            case ColumnDef.type_sec:
               record_stamp = message.read_sec();
               break;
               
            default:
               throw new Exception("Unsupported time type");
            }
         }
         
         // we can now create a record to read the data
         Record record = make_record();
         record.read(
            begin_record_no + i, 
            (LoggerDate)record_stamp.clone(), 
            message);
         record_stamp.add_nsec(interval);
         records.add(record);
      }
      return count;
   } // read_records
   
   
   /**
    * Formats the values list as if for a data collection request.  This list will
    * be empty (null terminator only) if the table is not a subset or will specify
    * a list of column numbers otherwise.
    */
   protected void format_column_request(Packet collect_command)
   {
      if(is_subset)
      {
         for(ColumnDef column: columns)
            collect_command.add_uint2(column.column_no);
      }
      collect_command.add_uint2(0);
   }
   
   /**
    * Used to cache allocated records
    */
   private List<Record> record_cache;


   /**
    * Used to cache incomplete records until all expected fragments have been
    * received.
    */
   private Packet one_way_buffer;


   /**
    * Used to keep track of the begin record number for the last one way
    * fragment received.
    */
   private long last_one_way_record_no;
   
   /**
    * Used to specify that this table is a subset of the datalogger table.  
    * This is set to true when make_subset is called to create a new table
    * based upon an existing table. 
    */
   private boolean is_subset;
}

