/* GetValuesTran.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Thursday 07 December 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.IOException;



/**
 * Defines a transaction that can drive the BMP5 get values transaction.  This
 * differs from {@link DataCollectTran data collection} in that this
 * transaction can be done without requiring table definitions (the datalogger
 * will convert the data to the specified type),  only one field can be queried
 * (that field can be an array), and no record number or time stamp will be
 * returned.
 */
public class GetValuesTran extends TransactionBase
{
   /**
    * Specifies that the values should be returned as strings (arrays of
    * characters).  When this type code is used, the value of swath should be
    * the expected length of the string.
    */
   public static final int type_string = ColumnDef.type_ascii;

   /**
    * Specifies that the value(s) should be returned as type float.
    */
   public static final int type_float = ColumnDef.type_ieee4;

   /**
    * Specifies that the value(s) should be returned as type int.
    */
   public static final int type_int = ColumnDef.type_int4;

   
   /** 
    * Specifies that this transaction has succeeded
    */
   public static final int outcome_success = 1;
   
   /**
    * Specifies that this transaction failed due to a general communications failure
    */
   public static final int outcome_comm_failure = 2;

   /**
    * Specifies that the transaction failed because the PakBus link was lost
    */
   public static final int outcome_link_failure = 3;
   
   /**
    * Specifies that the transaction failed because low level I/O failed.
    */
   public static final int outcome_port_failure = 4;
   
   /**
    * Specifies that the transaction failed because no response was received event after retries. 
    */
   public static final int outcome_failure_timeout = 5;
   
   /**
    * Specifies that the transaction failed because the command message could not be routed.
    */
   public static final int outcome_failure_unroutable = 6;
   
   /**
    * Specifies that the transaction failed because our security code is invalid
    */
   public static final int outcome_permission_denied = 7;
   
   /**
    * Specifies that this transaction is not supported by the datalogger.
    */
   public static final int outcome_failure_unsupported = 8;

   /**
    * Specifies that the datalogger does not have the named table or the named
    * field  for the table.
    */
   public static final int outcome_invalid_table_or_field = 9;

   /**
    * Specifies that the datalogger is unable to convert from the data type for the
    * given field to the data type that was requested for this transaction.
    */
   public static final int outcome_conversion_not_supported = 10;

   /**
    * Specifies that the given swath and starting location will go beyond the memory
    * bounds of the requested variable.
    */
   public static final int outcome_memory_bound_error = 11;
   
   
   /**
    * @param client_  Specifies the object that will receive a notification when
    * this transaction is complete.
    * @param table_name_  Specifies the name of the table
    * @param field_name_  Specifies the name of the field to be queried
    * @param swath_  Specifies the number of values of an array to be returned.
    * This must be set to one for scalar types or if only one value is to be
    * returned.
    * @param type_  Specifies the data type that will be expected.  Must be one
    * of the following values:
    * <ul>
    * <li>{@link GetValuesTran#type_string}
    * <li>{@link GetValuesTran#type_float}
    * <li>{@link GetValuesTran#type_int}
    * </ul>
    */
   public GetValuesTran(
      GetValuesClient client_,
      String table_name_,
      String field_name_,
      short swath_,
      int type_)
   {
      client = client_;
      table_name = table_name_;
      field_name = field_name_;
      swath = swath_;
      type = type_;
   } // constructor

   
   @Override
   public String get_name()
   { return "GetValues(" + table_name + ", " + field_name + ", " + swath + ")"; }
   
   
   @Override
   public void on_focus_start() throws Exception
   {
      try
      {
         // form and send the command packet
         Packet command = new Packet();
         command.protocol_type = Packet.protocol_bmp5;
         command.message_type = Packet.bmp5_get_values_cmd;
         command.add_uint2(station.get_security_code());
         command.add_string(table_name);
         command.add_byte((byte)type);
         command.add_string(field_name);
         command.add_uint2(swath);
         post_message(command);
      }
      catch(IOException e)
      { throw e; }
      catch(Exception e)
      { on_complete(outcome_comm_failure); }
   } // on_focus_start
   

   @Override
   public void on_message(Packet message) throws Exception
   {
      if(message.protocol_type == Packet.protocol_bmp5 &&
         message.message_type == Packet.bmp5_get_values_ack)
      {
         try
         {
            int response = message.read_byte();
            if(response == 0)
            {
               // the record to hold the values needs to be created.  In order to do this, we need generate
               // the requisite table definitions.
               TableDef table_def = new TableDef(table_name,station);
               ColumnDef col_def = new ColumnDef(field_name,(byte)type);
               table_def.columns.add(col_def);
               col_def.piece_size = swath;
               col_def.dims.add_dimension(swath);
               col_def.begin_index = 1;
               
               // we can now create the record
               record = table_def.make_record();
               record.read(0, new LoggerDate(), message);
               on_complete(outcome_success);
            }  
            else
            {
               int outcome = outcome_comm_failure;
               switch(response)
               {
               case 1:
                  outcome = outcome_permission_denied;
                  break;
                  
               case 16:
                  outcome = outcome_invalid_table_or_field;
                  break;
                  
               case 17:
                  outcome = outcome_conversion_not_supported;
                  break;
                  
               case 18:
                  outcome = outcome_memory_bound_error;
                  break;
               }
               on_complete(outcome);
            }
         }
         catch(Exception e)
         { on_complete(outcome_comm_failure); }
      }
   } // on_message
   
   
   /**
    * Called when this transaction is complete
    */
   private void on_complete(int outcome) throws Exception
   {
      close();
      if(outcome != outcome_success)
         record = null;
      if(client != null)
      {
         client.on_complete(this, outcome, record);
         client = null;
      }
   } // on_complete
   

   private GetValuesClient client;
   private String table_name;
   private String field_name;
   private int type;
   private int swath;
   private Record record;
}
