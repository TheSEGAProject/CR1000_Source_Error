/* Record.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Saturday 14 October 2006
   Last Change: Monday 16 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;



/**
 *  Defines an object that represents a single record collected from
 *  a datalogger table.  This object will keep a reference to the 
 *  original table definition as well as the record number and time 
 *  stamp.  It will also maintain a collection  of value objects.
 */
public class Record implements Cloneable
{
   /**
    * @param table_def_  Specifies the table definition 
    */
   public Record(TableDef table_def_)
   {
      // initialise the class members
      table_def = table_def_;
      values = new Vector<ValueBase>(table_def.get_values_count());
      storage = new byte[table_def.get_record_size()];
      
      // we must now create the values and allocate them pieces of the storage buffer
      int storage_offset = 0;
      ValueFactory factory = table_def.station.value_factory;
      
      for(ColumnDef column: table_def.columns)
      {
         for(int i = 0; i < column.get_values_count(); ++i)
         {
            ValueBase value = factory.make_value(column, column.begin_index + i);
            storage_offset += value.assign_record_buff(storage, storage_offset);
            values.add(value);
         }
      }
   } // constructor
   
   
   /**
    * @return the table definition
    */
   public TableDef get_table_def()
   { return table_def; }

   
   /**
    * @return the record number
    */
   public long get_record_no()
   { return record_no; }
   
   
   /**
    * @return the record time stamp
    */
   public LoggerDate get_time_stamp()
   { return time_stamp; }

   
   /**
    * Looks up the index of the value given by the name and the array address.
    * 
    * @param column_name  specifies the column name of the value
    * @param array_address  Specifies the address of the desired value.  If this
    * is given as an empty list, the first value associated with the column name 
    * will be returned.
    * @return the index of the matching value or, if no value could be found to match,
    * a negative number.
    */
   public int find_column_index(
      String column_name,
      List<Integer> array_address)
   {
      // we will use much the same algorithm as is used when a linear index
      // is passed in.  The difference is that we must first find a matching
      // column definition so that the array_address parameter can be 
      // converted to a linear address.
      int rtn = -1;
      try
      {
         int passed_count = 0;
         int linear_index = -1;
         for(ColumnDef column: table_def.columns)
         {
            int num_values = column.get_values_count();
            
            if(column.name.compareToIgnoreCase(column_name) == 0)
            {
               if(linear_index == -1)
                  linear_index = column.to_linear_index(array_address);
               if(linear_index >= column.begin_index &&
                  linear_index < column.begin_index + num_values)
               {
                  rtn = passed_count + linear_index - column.begin_index;
                  break;
               }
               else
                  passed_count += num_values;
            }
            else
               passed_count += num_values;
         }
      }
      catch(Exception e)
      { rtn = -1; }
      return rtn;
   } // find_column_index
   
   
   /**
    * Alternate version that searches for a fully qualified value name
    * (the array address would be incorporated into the name).
    * 
    * @param value_name Specifies the value name and, optionally, 
    * the array address.
    * @return the index of the located value, if any, or a negative number
    * if no value could be located.  
    */
   public int find_column_index(String value_name)
   {
      int rtn = -1;
      try
      {
         ValueName parsed = new ValueName(value_name);
         rtn = find_column_index(
            parsed.get_column_name(),
            parsed.get_array_address());
      }
      catch(Exception e)
      { rtn = -1; }
      return rtn;
   } // find_value_name
   
   
   /**
    * An alternate version that searches for the specified value using a column 
    * name and a linear array index (assumes one based offset and row major ordering)
    * 
    * @param column_name  The name of the column
    * @param linear_index  the linear index of the value
    * @return  the index of the located value or a negative number if no value could
    * be located.
    */
   public int find_column_index(
      String column_name,
      int linear_index)
   {
      // we need to locate the column definition that should contain this value.  While 
      // doing so, we will keep track of the number of values that are passed over for
      // each piece
      int rtn = -1;
      int passed_count = 0;
      for(ColumnDef column: table_def.columns)
      {
         int num_values = column.get_values_count();
         if(column.name.compareToIgnoreCase(column_name) == 0 &&
            linear_index >= column.begin_index &&
            linear_index < column.begin_index + num_values)
         {
            rtn = passed_count + linear_index - column.begin_index;
            break;
         }
         else
            passed_count += num_values;
      }
      return rtn;
   } // find_column_index
   
   
   /**
    * Returns the value associated with the specified index or null
    * if there is no such value
    * 
    * @param index  The value index.  This parameter is 0 based
    * @return The value object at the specified index or null
    */
   public ValueBase get_value(int index)
   { return values.get(index); }
   
   
   /**
    * @return the number of values in this record
    */
   public int get_values_count()
   { return values.size(); }
   
   
   /**
    * @return the values contained in this record
    */
   public List<ValueBase> get_values()
   { return values; }
   
   
   /**
    * Sets the record number and time stamp and reads the record data
    * from the provided message
    * 
    * @param record_no_  Specifies the record number for this record
    * @param time_stamp_ Specifies the new time stamp for this record
    * @param message     The message from which this record data should be read
    */
   void read(
      long record_no_,
      LoggerDate time_stamp_,
      Packet message) throws Exception
   {
      record_no = record_no_;
      time_stamp = time_stamp_;
      message.read_bytes(storage,storage.length);
   } // read
   
   
   /**
    * Reference to the table definition that generated this record
    */
   private TableDef table_def;
   
   
   /**
    * Specifies the record number
    */
   private long record_no;
   
   
   /**
    * Specifies the time stamp 
    */
   private LoggerDate time_stamp;
   
   
   /**
    * Specifies the list of values associated with this record
    */
   private List<ValueBase> values;
   
   
   /**
    * Buffer for the values associated with this record.
    */
   private byte[] storage;
}
