/* DataCollectMode.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Monday 16 October 2006
   Last Change: Wednesday 14 May 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;


/**
 * Defines a base class for an object that controls the data collect
 * transaction.  A subclass must be provided in the constructor for the
 * DataCollectTran class.   This subclass will control what data is collected
 * by the transaction and act as a filter from the transaction. 
 */
public abstract class DataCollectMode
{
   /**
    * @return a "name" for this collect mode 
    */
   String get_name()
   { return getClass().getSimpleName(); }
   
   
   /**
    * Called to form the next command packet for the transaction.  If the
    * state of the transaction is such that the transaction should end, this 
    * method will return a null pointer.
    * 
    * @return The next command message that should be sent for the data 
    * collect transaction.  Will return null if there are no further
    * messages to send. 
 * @throws Exception 
    */
   public abstract Packet get_next_command() throws Exception;
   
   
   /**
    * Called to handle the response from the last command.  This assumes that
    * the response code came back indicating success so the response will be
    * a collection of records.  
    * 
    * @param records  The collection of records in the response
    */
   public abstract void on_response(List<Record> records);
   
   
   /**
    * Reference to the data collect transaction
    */
   protected DataCollectTran transaction;
   
   
   /**
    * Reference to the table definition
    */
   protected TableDef table_def;
   
   
   /**
    * Reference to the station object
    */
   protected Datalogger station;
}
