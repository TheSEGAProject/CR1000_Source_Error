/* ValueBase.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 13 October 2006
   Last Change: Friday 13 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines a base class for value objects that are parts of the Record class.
 */
public abstract class ValueBase implements Cloneable
{
   /** 
    * This constructor is meant to be invoked by the ValueFactory 
    * class (or one of its descendents).
    * 
    * @param column_def  Specifies the column definition
    * @param array_offset
    */
   protected ValueBase(
      ColumnDef column_def,
      int array_offset)
   {
      this.column_def = column_def;
      this.array_offset = array_offset;
   } // constructor
   
   
   /**
    * @return the column name associated with this value
    */
   public String get_name()
   { return column_def.name; }
   
   
   /**
    * @return the column data type
    */
   public byte get_data_type()
   { return column_def.data_type; }
   
   
   /** 
    * @return the column process strings
    */
   public String get_processing()
   { return column_def.processing; }
   
   
   /**
    * @return the column unit string
    */
   public String get_units()
   { return column_def.units; }
   
   
   /**
    * @return the formatted name with array index
    */
   public String format_name()
   { return column_def.format_name(array_offset); }
   
   
   /**
    * @return the size of this value in the record buffer
    */
   public abstract int raw_size();
   
   
   /**
    * Must be overloaded to format the value as a string.
    */
   public abstract String format();
   
   
   /**
    * Must be overloaded to convert the value to an integer or to 
    * throw an UnsupportedOperationException object if the conversion
    * is not supported
    */
   public abstract int to_int() throws UnsupportedOperationException;


   /**
    * Can be overloaded to convert the value into a long integer.
    */
   public abstract long to_long() throws UnsupportedOperationException;
   
   
   /**
    * Must be overloaded to convert the value to a floating point value
    * or to throw an UnsupportedOperationException object if the 
    * conversion is not supported.
    */
   public abstract float to_float() throws UnsupportedOperationException;
   
   
   /**
    * Converts the value to a double precision floating point.
    * 
    * @return the double value 
    */
   public double to_double() throws UnsupportedOperationException
   { return to_float(); }
   
   
   /**
    * @return the value formatted as a string
    */
   public String toString()
   { return format(); }
   
   
   /**
    * Assigns the record buffer and offset.
    * 
    * @param record_buff  Specifies the record's buffer
    * @param record_buff_offset  Specifies this value's offset
    * into the record buffer
    * @return The number of bytes this value will occupy in the
    * record buffer.
    */
   protected int assign_record_buff(
      byte[] record_buff,
      int record_buff_offset)
   {
      this.record_buff = record_buff;
      this.record_buff_offset = record_buff_offset;
      return raw_size();
   } // assign_record_buff
   
   
   /**
    * Reference to the column definition associated with this value
    */
   protected ColumnDef column_def;
   
   
   /**
    * Specifies the linear offset for the array address of this value. 
    * This index is one based and assumes row major order.
    */
   protected int array_offset;
   
   
   /**
    * Reference to the record's data buffer.  This is assigned when the
    * record calls set_record_buff().
    */
   protected byte[] record_buff;
   
   
   /**
    * Offset into the record buffer where this values raw data can be found
    */
   protected int record_buff_offset;
}
