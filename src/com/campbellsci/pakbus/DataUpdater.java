/* DataUpdater.java

   Copyright (C) 2007, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 04 April 2007
   Last Change: Monday 04 February 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.LinkedList;
import java.util.List;


/**
 * Defines an object that will perform data collection on a specified table and
 * using the specified interval.  Data collection will begin using conditions
 * specified by the constructor and thereafter will poll for newly stored
 * records on the interval specified by the constructor.  Status updates will
 * be sent to the client object.
 */
public class DataUpdater extends TransactionBase implements DataCollectClient
{
   /**
    * Constructor used to make this updater start with the newest record
    * @param client_  The object that will receive status notifications
    * @param table_name_ Specifies the name of the table to be polled.
    * @param poll_interval_ Specifies the minimum interval at which polling
    * should occur.
    */
   public DataUpdater(
      DataUpdaterClient client_,
      String table_name_,
      long poll_interval_)
   {
      client = client_;
      start_at_newest = true;
      relative_to_newest = false;
      poll_interval = poll_interval_;
      newest_record_no = 0;
      waiting_for_first = true;
      table_name = table_name_;
   }

   /**
    * Constructor used to make this updater start at a record with the
    * specified time stamp or a specified time interval before the newest table
    * record.
    *
    * @param client_ The object that will receive the status notifications
    * @param table_name_ The name of the table to be polled
    * @param poll_interval_ Specifies the minimum interval at which the table
    * should be polled for new records.
    * @param start_time_ The time stamp for the beginning record, or if
    * <tt>relative_to_newest</tt> is true, the interval behind the newest
    * record to begin.
    * @param relative_to_newest_  Specifies that the start_time_ parameter
    * should be treated as an interval relative to the newest record in the
    * table. 
    */
   public DataUpdater(
      DataUpdaterClient client_,
      String table_name_,
      long poll_interval_,
      LoggerDate start_time_,
      boolean relative_to_newest_)
   {
      client = client_;
      start_at_newest = false;
      relative_to_newest = relative_to_newest_;
      start_time = start_time_;
      poll_interval = poll_interval_;
      newest_record_no = 0;
      waiting_for_first = true;
      table_name = table_name_;
   }

   /**
    * Use this function to add a specific value that will be collected.  By default, all values in the table
    * will be collected.  If the application calls this to add specific values, only those pieces that are
    * associated with the column names will be collected.
    */
   public void add_column_name(String column_name)
   {
      if(column_names == null)
         column_names = new LinkedList<String>();
      column_names.add(column_name);
   }
   
   
   /**
    * Use this method to remove all column names that were previously added with add_column_name().
    */
   public void clear_column_names()
   { column_names = null; }
   
   
   @Override
   public void on_focus_start() throws Exception
   {
      // we will set up the timer so that future attempts will retry even if polling does not work here
      time_since_last = new Timer();
      try
      {
         // we will create the poller now that will drive the first collection.
         check_table_def();
         if(start_at_newest || relative_to_newest)
         {
            poller = new DataCollectTran(
               table_def,   
               this,
               new DataCollectModeMostRecent(1));
         }
         else
         {
            poller = new DataCollectTran(
               table_def,
               this,
               new DataCollectModeDateToNewest(start_time));
         }
         station.add_transaction(poller);
      }
      catch(Exception e)
      {
         station.get_network().add_comment("Data updater start failed: " + e.toString());
         poller = null;
      }
      
      // this transaction uses others so it does not want to keep focus. 
      if(station.get_focus() == this)
         release_focus();
   }

   @Override
   public void on_message(Packet message) throws Exception
   { }


   /**
    * Handles the event when the poller has completed
    */
   public void on_complete(
      DataCollectTran transaction,
      int outcome) throws Exception
   {
      boolean first_poll_complete = true;
      poller = null;
      if(outcome == DataCollectTran.outcome_success)
      {
         if(waiting_for_first && relative_to_newest)
         {
            poller = new DataCollectTran(
               table_def,
               this,
               new DataCollectModeDateToNewest(start_time));
            station.add_transaction(poller);
            first_poll_complete = false;
         }
         waiting_for_first = false;
      }
      else if(outcome == DataCollectTran.outcome_invalid_table_defs ||
              outcome == DataCollectTran.outcome_invalid_table_name)
      {
         station.start_get_table_defs();
         first_poll_complete = false;
      }
      if(first_poll_complete)
         client.on_poll_complete(this,outcome);
   }

   
   /**
    * Handles the incoming records from the data poller by forwarding these to
    * the client
    */
   public boolean on_records(
      DataCollectTran transaction,
      List<Record> records)
   {
      boolean rtn = true;
      if(client != null && poller != null)
      {
         if(waiting_for_first && !start_at_newest && relative_to_newest && !records.isEmpty())
         {
            Record first = records.get(0);
            LoggerDate new_start_time = new LoggerDate(first.get_time_stamp());
            new_start_time.add_nsec(-1 * start_time.get_elapsed());
            start_time = new_start_time;
         }
         else if(!records.isEmpty())
         {
            Record last = records.get(records.size() - 1);
            if(last.get_record_no() != newest_record_no)
            {
               newest_record_no = last.get_record_no();
               client.on_records(this, records);
            }
         }
      }
      else
         rtn = false;
      return rtn;
   }

   
   /** 
    * Overrides the base version to check to see if the polling interval is up
    */
   @Override
   public void check_state() throws Exception
   {
      super.check_state();
      try
      {
         if(time_since_last != null &&
            time_since_last.elapsed() > poll_interval &&
            poller == null)
         {
            check_table_def();
            station.get_network().add_comment(
               "Data Updater starting poll: " + station.station_name + "." + table_name);
            if(!waiting_for_first)
            {
               poller = new DataCollectTran(
                  table_def,
                  this,
                  new DataCollectModeRecordNoToNewest(newest_record_no + 1));
            }
            else
            {
               poller = new DataCollectTran(
                  table_def,
                  this,
                  new DataCollectModeMostRecent(1));
            }
            time_since_last.reset();
            station.add_transaction(poller);
         }
      }
      catch(Exception e)
      {
         station.get_network().add_comment("Data updater poll failed: " + e.toString());
         poller = null;
      }
   }
   
   
   @Override
   public void close() throws Exception
   {
      if(poller != null)
      {
         poller.close();
         poller = null;
      }
      super.close();
   }

   
   @Override
   protected void on_table_defs_changed()
   {
      table_def = null;
      poller = null;
      try
      {
         check_state();
      }
      catch (Exception e)
      { 
         poller = null;
         table_def = null;
      }
   }
   
   
   /**
    * Looks up the table def from the datalogger and makes a subset if needed 
    */
   private void check_table_def() throws Exception
   {
      if(table_def == null)
      {
         TableDef temp = station.get_table(table_name);
         if(temp != null)
            table_def = temp.make_subset(column_names);
         else
         {
            station.start_get_table_defs();
            throw new Exception("Table " + table_name + " does not exist");
         }
      }
   }
   

   /**
    * Keeps the reference to the client object for this updater
    */
   private DataUpdaterClient client;
   
   /**
    * Performs the polling operations for this updater
    */
   private DataCollectTran poller;
   
   /**
    * Specifies the name of the table to be polled
    */
   private String table_name;
   
   /**
    * Specifies the minimum interval at which polling will take place
    */
   private long poll_interval;
   
   /**
    * Set to true if this updater should start polling at the newest record.
    */
   private boolean start_at_newest;
   
   /**
    * Set to true if the start_time should be interpreted as an interval relative
    * to the timestamp of the newest record.  If false, start_time will be interpreted
    * as the timestamp for the first record.
    */
   private boolean relative_to_newest;
   
   /**
    * Specifies the starting date for the first record if relative_to_newest is false 
    * or the time interval before the newest record if relative_to_newest is true. 
    */
   private LoggerDate start_time;
   
   /**
    * Keeps track of the time since the last poll was started 
    */
   private Timer time_since_last;
   
   /**
    * Tracks the last record number received.  Used to determine what records should
    * be polled next
    */
   private long newest_record_no;
   
   /**
    * Specifies that this object has not yet received its first record.
    */
   private boolean waiting_for_first;
   
   /**
    * Used to keep track of the column names that the application wants to monitor.  
    * An empty list (the default) signifies that the entire record should be returned.
    */
   private List<String> column_names;
   
   /**
    * Used to track the table that will be requested.  By caching this object here, we
    * can save the effort of looking up table defs on each poll and we can also keep any 
    * restricted structures between polls. 
    */
   private TableDef table_def;
}
