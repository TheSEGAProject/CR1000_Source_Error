/* SetValuesTran.java

   Copyright (C) 2007, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 07 May 2007
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.IOException;


/**
 * Defines a component that can drive the BMP5 set values transaction.  Only
 * one field (one field can be an array) can be set in any one transaction.
 */ 
 public class SetValuesTran extends TransactionBase
 {
    /**
       Set Float scalar constructor

       @param client_   Specifies the object that implements the {@link
       SetValuesClient} interface and that will handle the completion
       notification for this transaction. 
       @param table_name_  Specifies the name of the table (generally "public")
       that contains the value to be set.
       @param column_name_  Specifies column name and, optionally, the array
       subscript for the value to be set.
       @param value_ Specifies the floating point value that should be sent.
    */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       float value_)
    {
       client = client_;
       float_values = new float[1];
       float_values[0] = value_;
       table_name = table_name_;
       column_name = column_name_;
    }


    /**
     * Sets up this transaction to send an array of floating point values.
     *
     * @param client_   Specifies the object that implements the {@link
     * SetValuesClient} interface and that will handle the completion
     * notification for this transaction. 
     * @param table_name_  Specifies the name of the table that contains the
     * values.
     * @param column_name_  Specifies the name of the column and, optionally,
     * the starting array address for the values to be set. 
     * @param values_  Specifies the values to be sent.
     */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       float[] values_)
    {
       client = client_;
       table_name = table_name_;
       column_name = column_name_;
       float_values = values_;
    } 
    
    
    /**
     * Sets up this transaction to send a scalar integer value to the datalogger.
     * 
     * @param client_  Specifies the object that implements the {@link SetValuesClient} 
     * interface and that will handle the completion event from this transaction.
     * @param table_name_  Specifies the name of the table that contains the
     * values.
     * @param column_name_  Specifies the name of the column and, optionally,
     * the starting array address for the values to be set. 
     * @param value_  Specifies the value to be sent.
     */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       int value_)
    {
       client = client_;
       table_name = table_name_;
       column_name = column_name_;
       int_values = new int[1];
       int_values[0] = value_;
    }
    
    
    /**
     * Sets up this transaction to set an array of integer values. 
     * 
     * @param client_  Specifies the object that implements the {@link SetValuesClient} 
     * interface and that will handle the completion event from this transaction.
     * @param table_name_  Specifies the name of the table that contains the
     * values.
     * @param column_name_  Specifies the name of the column and, optionally,
     * the starting array address for the values to be set. 
     * @param values_  Specifies the values to be sent.
     */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       int[] values_)
    {
       client = client_;
       table_name = table_name_;
       column_name = column_name_;
       int_values = values_;
    }
    
    
    /**
     * Sets up this transaction to set a single boolean scalar
     * 
     * @param client_  Specifies the object that implements the {@link SetValuesClient} 
     * interface and that will handle the completion event from this transaction.
     * @param table_name_  Specifies the name of the table that contains the
     * values.
     * @param column_name_  Specifies the name of the column and, optionally,
     * the starting array address for the value to be set. 
     * @param value_  Specifies the value to be sent.
     */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       boolean value_)
    {
       client = client_;
       table_name = table_name_;
       column_name = column_name_;
       bool_values = new boolean[1];
       bool_values[0] = value_;
    }
    
    
    /**
     * Sets up this transaction to set an array of boolean values.
     * 
     * @param client_  Specifies the object that implements the {@link SetValuesClient} 
     * interface and that will handle the completion event from this transaction.
     * @param table_name_  Specifies the name of the table that contains the
     * values.
     * @param column_name_  Specifies the name of the column and, optionally,
     * the starting array address for the values to be set. 
     * @param values_  Specifies the value to be sent.
     */
    public SetValuesTran(
       SetValuesClient client_,
       String table_name_,
       String column_name_,
       boolean[] values_)
    {
       client = client_;
       table_name = table_name_;
       column_name = column_name_;
       bool_values = values_;
    }
    
    @Override
    public void on_focus_start() throws Exception
    {
       try
       {
          Packet command = new Packet();
          command.protocol_type = Packet.protocol_bmp5;
          command.message_type = Packet.bmp5_set_values_cmd;
          command.add_uint2(station.get_security_code());
          command.add_string(table_name);
          if(float_values != null)
          {
             command.add_byte(ColumnDef.type_ieee4);
             command.add_string(column_name);
             command.add_uint2(float_values.length);
             for(float value: float_values)
                command.add_float(value);
          }
          else if(int_values != null)
          {
             command.add_byte(ColumnDef.type_int4);
             command.add_string(column_name);
             command.add_uint2(int_values.length);
             for(int value: int_values)
                command.add_int4(value);
          }
          else if(bool_values != null)
          {
             command.add_byte(ColumnDef.type_bool4);
             command.add_string(column_name);
             command.add_uint2(bool_values.length);
             for(boolean value: bool_values)
                command.add_bool(value);
          }
          else
             command = null;

          if(command != null)
             post_message(command);
          else
             on_complete(outcome_comm_failure);
       }
       catch(IOException e)
       { throw e; }
       catch(Exception e)
       { on_complete(outcome_comm_failure); }
    }

    
    
    /* (non-Javadoc)
    * @see com.campbellsci.pakbus.TransactionBase#on_failure(int)
    */
   @Override
   public void on_failure(int reason) throws Exception
   {
      int outcome = outcome_comm_failure;
      switch(reason)
      {
      case TransactionBase.failure_comms:
         outcome = outcome_comm_failure;
         break;
         
      case TransactionBase.failure_link:
         outcome = outcome_link_failure;
         break;
         
      case TransactionBase.failure_port:
         outcome = outcome_port_failure;
         break;
         
      case TransactionBase.failure_timeout:
         outcome = outcome_failure_timeout;
         break;
         
      case TransactionBase.failure_unroutable:
         outcome = outcome_failure_unroutable;
         break;
         
      case TransactionBase.failure_unsupported:
         outcome = outcome_failure_unsupported;
         break;
      }
      on_complete(outcome);
   }


   @Override
    public void on_message(Packet message) throws Exception
    {
       int outcome = outcome_comm_failure;
       if(message.protocol_type == Packet.protocol_bmp5 &&
          message.message_type == Packet.bmp5_set_values_ack)
       {
          try
          {
             int logger_resp = message.read_byte();
             switch(logger_resp)
             {
             case 0:
                outcome = outcome_success;
                break;
                
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
                
             default:
             outcome = outcome_comm_failure;
             break;
             }
          }
          catch(Exception e)
          { outcome = outcome_comm_failure; }
          on_complete(outcome);
       }
    }

    /**
     * Specifies the code reported when this transaction has successfully completed
     */
    public static int outcome_success = 1;
    
    /**
     * Specifies the code reported when this transaction has failed due to a communication error
     */
    public static int outcome_comm_failure = 2;
    
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
     * Called when this transaction is complete 
     */
    private void on_complete(int outcome) throws Exception
    {
       close();
       if(client != null)
          client.on_complete(this,outcome);
    }
    
    
    /**
     * Specifies the client object that will receive completion notifications
     * from this transaction.
     */
    private SetValuesClient client;

    /**
     * Specifies the name of the table
     */
    private String table_name;

    /**
     * Specifies the name of the column
     */
    private String column_name; 

    /**
     * Holds the floating point values that will be sent.
     */
    private float[] float_values;
    
    /**
     * Holds the integer values that will be sent.
     */
    private int[] int_values;
    
    /**
     * Holds the set of boolean values that will be sent.
     */
    private boolean[] bool_values;
}
