/* Datalogger.java

   Copyright (C) 2006, 2010 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Friday 29 September 2006
   Last Change: Tuesday 22 June 2010
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.util.*;



/**
 * Defines an object that represents the state of a datalogger in the Java
 * PakBus API.  The state maintained includes the route used (the datalogger
 * PakBus address as well as the neighbour address used to reach the station)
 * as well as that stations meta-data including programming stats and table
 * definitions.
 */
public final class Datalogger implements GetTableDefsClient
{
   /**
    * Constructs the station with the required routing information.
    * @param pakbus_address_  Specifies the PakBus address for the station.
    * This value must match the address setting of that station if
    * communication with the station can succeed.
    * @param neighbour_address_  Specifies the address of the router that should
    * be used to reach the station.  This value should be the same as
    * pakbus_address if the station is expected to be reached directly.  If
    * this value is specified as zero, the value of the pakbus_address will be
    * used. 
    */
   public Datalogger(
      short pakbus_address_,
      short neighbour_address_)
   {
      Random generator = new Random();
      pakbus_address = pakbus_address_;
      if(neighbour_address_ != 0)
         neighbour_address = neighbour_address_;
      else
         neighbour_address = pakbus_address;
      transactions = new HashMap<Short, TransactionBase>();
      defunct_transactions = new LinkedList<Short>();
      last_tran_no = (short)generator.nextInt(255);
      max_packet_size = 998;
      round_trip_time = 5000;
      table_defs = new Vector<TableDef>();
      value_factory = new ValueFactory();
      one_way_data_handlers = new LinkedList<OneWayDataHandler>();
      check_for_shutdown = false;
   } // constructor


   /**
    * A second form of the constructor that takes just the datalogger pakbus.
    * In this case, the neighbour address will be set to the same address.
    * @param pakbus_address_  Specifies both the pakbus and neighbour address
    * for the datalogger.
    */
   public Datalogger(
      short pakbus_address_)
   {
      Random generator = new Random();
      pakbus_address = neighbour_address = pakbus_address_;
      transactions = new HashMap<Short, TransactionBase>();
      defunct_transactions = new LinkedList<Short>();
      last_tran_no = (short)generator.nextInt(255);
      round_trip_time = 5000;
      max_packet_size = 998;
      table_defs = new Vector<TableDef>();
      value_factory = new ValueFactory();
      one_way_data_handlers = new LinkedList<OneWayDataHandler>();
      check_for_shutdown = false;
   } // constructor
      

   /**
    * Called to get the datalogger PakBus address.
    * @return the station PakBus address.
    */
   public short get_pakbus_address()
   { return pakbus_address; }


   /**
    * Returns the neighbour address used to reach this station.
    * @return The neighbour address
    */
   public short get_neighbour_address()
   { return neighbour_address; }


   /**
    * Sets the network reference.  This will be done automatically when the
    * station is first added to the network.
    * @param network_  Specifies the network reference
    */
   public void set_network(Network network_)
   { network = network_; }


   /**
    * Returns the assigned network reference
    * @return the datalogger's assigned network
    */
   public Network get_network()
   { return network; }


   /**
    * @return An estimate of the time required to send a message to the datalogger and for the
    * response for that message to be received.
    */
   public int get_round_trip_time()
   { return round_trip_time; }


   /**
    * Sets the round trip time estimate for this station
    *
    * @param round_trip_time_  Specifies the application estimate of the round trip time required to
    * send a message to the datalogger and receive a response back.  The value is specified in units
    * of milli-seconds.  In order to be accepted, this value must be greater than 5 seconds or less
    * than 30 seconds.
    */
   public void set_round_trip_time(int round_trip_time_)
   { 
      if(round_trip_time_ >= 5000 && round_trip_time_ <= 30000)
      {
         round_trip_time = round_trip_time_;
         network.add_comment("round trip time for " + pakbus_address + " is " + round_trip_time + " msec");
      }
      else
         network.add_comment("attempt to set round trip time to " + round_trip_time + " msec failed");
   }

   
   /**
    * Adds the specified transaction to the map managed by this device.  At the
    * same time, assigns that transaction a unique identifier and assigns its
    * network and station fields.  Finally, focus is requested for the transaction. 
    * 
    * @param transaction  The transaction to be added. 
    */
   public void add_transaction(TransactionBase transaction) throws Exception
   {
      add_transaction_without_focus(transaction);
      transaction.request_focus();
   } // add_transaction
   
   
   /**
    * Adds the specified transaction without requesting focus.  This is most useful
    * when a transaction is being added under the control of another that already 
    * has focus.  
    * 
    * @param transaction  the transaction to be added
    */
   protected void add_transaction_without_focus(TransactionBase transaction) throws Exception
   {
      synchronized(transactions)
      {
         transaction.tran_no = generate_transaction_id();
         transaction.network = network;
         transaction.station = this;
         transactions.put(transaction.tran_no, transaction);
         network.add_comment(
            "Transaction added: \"" + 
            transaction.get_name() +
            "\" Dest: " + pakbus_address + 
            " Number: " + transaction.tran_no);
      }
   } // add_transaction_without_focus
   
   
   /**
    * Prepares to remove the specified transaction by placing its identifier in the defunct_transactions list.
    * 
    * @param  tran_no  Specifies the transaction number to be removed
    */
   public void remove_transaction(short tran_no) throws Exception
   {
      synchronized(transactions)
      {
         // make sure that the target transaction is marked so that it won't do any more retries
         TransactionBase target = transactions.get(tran_no);
         if(target != null)
         {
            target.is_satisfied = true;
            defunct_transactions.add(tran_no);
            if(!target.will_close())
               check_for_shutdown = true;
            network.add_comment(
               "Transaction closed: \"" + 
               target.get_name() +
               "\" Dest: " + pakbus_address + 
               " Number: " + target.tran_no);
         }
      }
   } // remove_transaction

   
   /** 
    * Sets the security code
    * 
    * @param security_code_  Specifies the new security code
    */
   public void set_security_code(int security_code_)
   {
      if(security_code_ < 0 || security_code_ > 65535)
         throw new IllegalArgumentException("the security code is out of range.");
      security_code = security_code_;
   } // set_security_code
   
   
   /**
    * @return the security code
    */
   public int get_security_code()
   { return security_code; }


   /**
    * @return Returns the raw table definitions or a null object if 
    * table definitions have not been assigned or read.
    */
   public Packet get_raw_table_defs()
   { return raw_table_defs; }
   
   
   /**
    * Sets the value of the raw table definitions member and causes the new 
    * table definitions to be parsed into the table_defs structure.  This version 
    * accepts a buffer and length as its parameters.
    * 
    *   @param buff  Specifies the contents of the raw table definitions
    *   @param buff_len Specifies the number of bytes to process from the buffer
    */
   public void set_raw_table_defs(
      byte[] buff,
      int buff_len)
   {
      Packet defs_packet = new Packet();
      defs_packet.add_bytes(buff, buff_len);
      set_raw_table_defs(defs_packet);
   }

   
   /**
    * Sets the value of the raw table defs member and causes the new table 
    * definitions to be parsed into the table_defs structure. 
    * 
    * @param raw_table_defs_  Specifies the packet for the raw table definitions. 
    */
   public void set_raw_table_defs(Packet raw_table_defs_)
   {
      // make the initial assignments
      raw_table_defs = raw_table_defs_;
      table_defs.clear();
      
      try
      {
         // we now need to parse the table defs structure
         if(raw_table_defs != null)
         {
            raw_table_defs.reset();
            byte fsl_version = raw_table_defs.read_byte();
            if(fsl_version == 1)
            {
               int table_no = 1;
               while(raw_table_defs.whats_left() > 0)
               {
                  TableDef table_def = new TableDef(table_no++, raw_table_defs, this);
                  table_defs.add(table_def);
               }
            }
         }
      }
      catch(Exception e)
      {
         raw_table_defs = null;
         table_defs.clear();
      }
      
      // we need to let each transaction know that table definitions may have changed
      synchronized(transactions)
      {
         for(TransactionBase transaction: transactions.values())
            transaction.on_table_defs_changed();
      }
   } // set_raw_table_defs
   
   
   /**
    * Looks up the table definition associated with the specified name.  The
    * name comparison is case insensitive.
    *
    * @param name  Specifies the name for the table
    * @return The table definition associated with the name or null if there is
    * no such table.
    */
   public TableDef get_table(String name)
   {
      TableDef rtn = null;
      for(TableDef table: table_defs)
      {
         if(name != null && table.name.compareToIgnoreCase(name) == 0)
         {
            rtn = table;
            break;
         }
      }
      try
      {
         if(table_defs.isEmpty())
            start_get_table_defs();
      }
      catch(Exception e)
      { }
      return rtn;
   } // get_table
   
   
   /**
    * @return the number of tables
    */
   public int get_tables_count()
   { return table_defs.size(); }
   
   
   /**
    * @param table_no  Specifies the table number
    * @return returns the associated table def or null
    */
   public TableDef get_table(int table_no)
   {
      TableDef rtn = null;
      try
      {
         if(table_defs.isEmpty())
            start_get_table_defs();
         rtn = table_defs.get(table_no - 1);
      }
      catch(Exception e)
      { }
      return rtn;
   }
   
   
   /**
    * Starts a transaction to get new table definitions for this logger.  
    * Nothing will be done if a transaction is already in progress. 
    */
   public void start_get_table_defs() throws Exception
   {
      if(get_table_defs_tran == null)
      {
         get_table_defs_tran = new GetTableDefsTran(this);
         add_transaction(get_table_defs_tran);
      }
   } // start_get_table_defs
   
   
   /**
    * Implements the completion notification for getting table definitions
    */
   public void on_complete(
      GetTableDefsTran transaction, 
      int outcome) throws Exception
   {
      get_table_defs_tran = null;
   }


   /**
    * @return the max allowed packet size
    */
   public int get_max_packet_size()
   { return max_packet_size; }
   
   
   /**
    * @param max_packet_size_  Specifies the new max packet size.  This must be less than 998
    */
   public void set_max_packet_size(int max_packet_size_)
   {
      max_packet_size = max_packet_size_;
      if(max_packet_size > 998)
         max_packet_size = 998;
   } // set_max_packet_size
   
   
   /**
    * @return the station model number
    */
   public String get_model_no()
   { return model_no; }
   
   
   /**
    * @return station name
    */
   public String get_station_name()
   { return station_name; }
   
   
   /**
    * @return The current program name
    */
   public String get_program_name()
   { return program_name; }
   
   
   /**
    * @return The compile result
    */
   public String get_compile_result()
   { return compile_result; }
   
   
   /**
    * Called to create the comm_resource_manager object.  This will keep this 
    * station from sending a shut down packet  because of the presence of the 
    * manager transaction.  This transaction can be cancelled by invoking 
    * stop_manage_comms().  This method will have no effect if there is already 
    * a manage comms transaction present. 
    */
   public void start_manage_comms() throws Exception
   {
      if(comm_resource_manager == null)
      {
         comm_resource_manager = new ManageCommResourceTran();
         add_transaction_without_focus(comm_resource_manager);
      }
   } // start_manage_comms
   
   
   /**
    * Called to stop the effects started when start_manage_comms() was called.
    */
   public void stop_manage_comms() throws Exception
   {
      if(comm_resource_manager != null)
         comm_resource_manager.close();
   } // stop_manage_comms
   
   
   /**
    * Sets the value factory that will be used.  An application can do this if it
    * needs to supply its own value objects for the records.
    * 
    *  @param value_factory_   Specifies the new value factory
    */
   public void set_value_factory(ValueFactory value_factory_)
   { value_factory = value_factory_; }
 
   
   /**
    * Adds a handler to the list of handlers for one way data events
    * 
    * @param handler  The object that will receive future one way 
    * data notifications. 
    */
   public void add_one_way_data_handler(OneWayDataHandler handler)
   { one_way_data_handlers.add(handler); }
   
   
   /**
    * Removes a handler from the list of handlers for one way
    * data events
    * 
    * @param handler  The handler to be removed
    */
   public void remove_one_way_data_handler(OneWayDataHandler handler)
   { one_way_data_handlers.remove(handler); }
   
   /**
    * Get the network transaction that currently has focus
    * @return The network transaction object that currently has focus.
    */
   public TransactionBase get_focus()
   { return network.get_focus(); }

   
   /**
    * Changes the identity of the specified transaction
    * 
    * @param transaction  specifies the transaction that should be changed. 
    */
   protected void change_transaction_id(TransactionBase transaction)
   {
      synchronized(transactions)
      {
         short old_tran_no = transaction.tran_no;
         transaction.tran_no = generate_transaction_id();
         transactions.put(transaction.tran_no, transaction);
         transactions.remove(old_tran_no);
         network.add_comment(
            "New Transaction ID: \"" +
            transaction.get_name() +
            "\" old: " + old_tran_no +
            " new: " + transaction.tran_no);
      }
   } // change_transaction_id
   
   
   /**
    * Called by the network when a message is ready to be sent.
    * 
    * @param message Specifies the message that is being sent
    */
   protected void on_message_being_sent(Packet message)
   {
      synchronized(transactions)
      {
         // find the transaction, if any, associated with this message
         TransactionBase transaction = transactions.get(message.tran_no);
         if(transaction != null)
         {
            transaction.on_message_being_sent(message);
            network.add_comment(
               "Sending message for \"" + transaction.get_name() + "\" number " + transaction.tran_no);
            if(transaction.get_is_satisfied())
               network.add_comment("  This transaction is satisfied");
            if(transaction.will_close())
               network.add_comment("  This transaction will close the link");
         }
         
         // we need to specify the expect more state of this message.  We will do so based upon the
         // state of all of the transactions associated with this station.
         byte expect_more_code = Packet.expect_last;
         for(TransactionBase tran: transactions.values())
         {
            if(!tran.get_is_satisfied() && !tran.will_close())
            {
               expect_more_code = Packet.expect_more;
               break;
            }
         }
         message.expect_more_code = expect_more_code;
      }
   } // on_message_being_sent
   
   
   /**
    * Called when a message has been received for this station.
    * 
    * @param message  Specifies the received message
    */
   protected void on_message_received(Packet message) throws Exception
   {
      // we will not send one waqy messages to transactions
      if(message.protocol_type == Packet.protocol_bmp5 &&
         (message.message_type == Packet.bmp5_one_way_data ||
          message.message_type == Packet.bmp5_one_way_table_def))
      {
         if(message.message_type == Packet.bmp5_one_way_data)
            on_one_way_data(message);
      }
      else
      {
         synchronized(transactions)
         {
            // find the transaction, if any associated with this message
            TransactionBase transaction = transactions.get(message.tran_no);
            if(transaction != null)
            {
               network.add_comment(
                  "Message received for \"" + transaction.get_name() + "\" number " + message.tran_no);
               transaction.do_on_message(message);
            }
            else
               network.add_comment(
                  "Message received for transaction " + message.tran_no + " which is not valid.");
         }
      }
   } // on_message_received


   /**
    * Called to pass the message to the router.  First sets the addresses of
    * the message.
    *
    * @param message  The message that is to be posted.
    */
   protected void post_message(Packet message) throws Exception
   {
      message.dest_address = pakbus_address;
      message.neighbour_dest_address = neighbour_address;
      network.post_message(message);
   } // post_message
   
   
   /**
    * Called by the network's check_state() method.  This method should be used
    * to check the state of all time sensitive operations including pending
    * transactions.
    */
   protected void check_state() throws Exception
   {
      synchronized(transactions)
      {
         // check the state of all transactions
         List<TransactionBase> temp_transactions = new LinkedList<TransactionBase>(transactions.values());
         for(TransactionBase transaction: temp_transactions)
            transaction.check_state();
         
         // if there are no active transactions, but there have been, we will need to post a shut down transaction.
         if(check_for_shutdown)
         {
            boolean post_shut_down = true;
            check_for_shutdown = false;
            for(TransactionBase tran: transactions.values())
            {
               if(!tran.is_satisfied || tran.will_close())
               {
                  post_shut_down = false;
                  break;
               }
            }
            if(post_shut_down)
               add_transaction(new ShutDownTran());
         }
      
      
         // our final act will be to close all defunct transactions
         for(Short tran_no: defunct_transactions)
         {
            TransactionBase tran = transactions.get(tran_no);
            transactions.remove(tran_no);
            if(tran != null)
               network.on_transaction_close(tran);
         }
         defunct_transactions.clear();
      }
   } // check_state
   
   
   /**
    * Called when a link failure has occurred that will effect this station
    * 
    * @param neighbour_address Specifies the neighbour address.  Will be zero
    * if all links are effected.
    */
   protected void on_link_failure(short neighbour_address) throws Exception
   {
      synchronized(transactions)
      {
         int failure_reason = TransactionBase.failure_link;
         if(neighbour_address == 0)
            failure_reason = TransactionBase.failure_port;
         for(TransactionBase transaction: transactions.values())
         {
            ++transaction.total_failures;
            transaction.on_failure(failure_reason);
         }
      }
   } // on_link_failure


   /**
    * Called when a delivery failure message has been received
    *
    * @param reason  Specifies the reason code reported
    * @param protocol  Specifies the protocol of the rejected message
    * @param message_type  Specifies the message type of the rejected message
    * @param tran_no   Specifies the transaction number of the rejected message
    */
   protected void on_delivery_failure(
      int reason,
      byte protocol,
      byte message_type,
      byte tran_no) throws Exception
   {
      synchronized(transactions)
      {
         TransactionBase transaction = transactions.get(tran_no);
         if(transaction != null)
            transaction.on_delivery_failure(reason,protocol,message_type);
      }
   } // on_delivery_failure
   
   
   /**
    * Generates a new, unique transaction identifier.
    * 
    * @return a unique transaction number
    */
   private short generate_transaction_id()
   {
      short rtn = last_tran_no;
      synchronized(transactions)
      {
         ++rtn;
         if(rtn > 0xff || rtn == 0)
            rtn = 1;
         while(transactions.containsKey(rtn))
         {
            ++rtn;
            if(rtn > 0xff || rtn == 0)
               rtn = 1;
         }
         last_tran_no = rtn;
      }
      return last_tran_no;
   } // generate_transaction_id
   
   
   /**
    * Processes an incoming one way data message
    * 
    * @param message  The message body
    */
   private void on_one_way_data(Packet message)
   {
      if(one_way_data_handlers.isEmpty())
         return;
      try
      {
         int table_no = message.read_uint2();
         for(TableDef table: table_defs)
         {
            if(table.table_no == table_no)
            {
               List<Record> records = new LinkedList<Record>();
               table.on_one_way_data_fragment(records, message);
               for(Record record: records)
               {
                  for(OneWayDataHandler handler: one_way_data_handlers)
                     handler.on_one_way_record(this, record);
               }
               break;
            }
         }
      }
      catch(Exception e)
      { }
   } // on_one_way_data
   

   /**
    * Holds the program compile results last read from the datalogger
    */
   protected String compile_result;
   
   
   /**
    * Holds the compile state last read from the datalogger
    */
   protected byte compile_state;
   
   
   /**
    * Holds the time of last compile 
    */
   protected LoggerDate compile_time;
   
   
   /**
    * Holds the signature of the logger OS
    */
   protected int os_signature;
   
   
   /**
    * Holds the operating system version in the logger
    */
   protected String os_version;
   
   
   /**
    * Holds the power up program for the datalogger
    */
   protected String power_up_program;
   
   
   /**
    * Holds the program name for the logger
    */
   protected String program_name;
   
   
   /**
    * Holds the signature of the current datalogger program
    */
   protected int program_signature;
   
   
   /**
    * Holds the datalogger serial number
    */
   protected String serial_no;
   
   
   /**
    * Holds the setting assigned station name
    */
   protected String station_name;
   
   
   /**
    * Holds the reported station model number
    */
   protected String model_no;
  
   
   /**
    * Reference to the object that manages the comms link.  Will be null if there is no such object
    */
   protected ManageCommResourceTran comm_resource_manager;
   
   
   /**
    * Reference to the value factory that will be used to help create records
    */
   protected ValueFactory value_factory;
   
   
   /**
    * Holds the station pakbus address.
    */
   private short pakbus_address;


   /**
    * Holds the neighbour address
    */
   private short neighbour_address;


   /**
    * Specifies the network to which this station belongs.  This value will be
    * set when the station is added to the network.
    */
   private Network network;


   /**
    * Maintains the set of currently active transactions keyed by their
    * transaction number.
    */
   protected Map<Short, TransactionBase> transactions;
   
   /**
    * Specifies the last transaction number that was allocated.
    */
   private short last_tran_no;
   
   
   /**
    * Specifies the list of transactions that should be deleted at the end of the state check cycle.
    */
   private List<Short> defunct_transactions;


   /**
    * Keeps an estimate of the round trip time required to send a command message to the station and
    * to receive a response message back.  This value is set to a default of 2500 msec in the
    * constructor but can be changed by calling set_round_trip_time().
    */
   private int round_trip_time;
   
   
   /**
    * Stores the security code for this station.  Initialised to zero but can be set by calling set_security_code().
    */
   private int security_code;


   /**
    * Stores the parsed table definitions for this datalogger.
    */
   private List<TableDef> table_defs;


   /**
    * Stores the raw information for the table definitions
    */
   private Packet raw_table_defs;
   
   
   /**
    * Specifies the maximum packet size that can be used for messages sent to this datalogger. 
    */
   private int max_packet_size;
   
   
   /**
    * Container for the list of handlers for one way data events
    */
   private List<OneWayDataHandler> one_way_data_handlers;
   
   /**
    * Keeps track of the current transaction to get table defs
    */
   private GetTableDefsTran get_table_defs_tran;
   
   
   /**
    * Used to flag that the check_state()  method should check to see if a shut down transaction should be posted.
    */
   boolean check_for_shutdown;
};

