/* TransactionBase.java

   Copyright (C) 2006, 2010 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Wednesday 04 October 2006
   Last Change: Thursday 25 March 2010
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.io.IOException;



/**
 * This class is a base class transactions in the Campbell Scientific Java
 * PakBus API.  The application is expected to extend this class to create
 * transaction objects that will accomplish specific operations.  This base
 * class provides timing and notification services.
 */
public abstract class TransactionBase
{
   /**
    * class constructor
    */
   public TransactionBase()
   {
      priority = Packet.pri_normal;
      tran_no = 0;
      last_message_sent = null;
      watch_dog = null;
      retry_count = 0;
      is_satisfied = false;
      min_time_out = 0;
      total_messages_sent = total_retries = total_failures = 0;
   } // constructor

   
   /**
    * @return the "name" of this transaction
    */
   public String get_name()
   { return getClass().getSimpleName(); }
   
   
   /**
    * Checks the state of this transaction.  This method should be delegated by
    * an overloaded version, if any.  This method will be invoked by the
    * station when its check_state() method is called.  This represents an
    * opportunity to check the transaction watch dog and other timing sensitive
    * members.
    */
   public void check_state() throws Exception
   {
      // check to see if the transaction has timed out
      if(is_satisfied)
         watch_dog = null;
      if(last_message_sent != null && watch_dog != null)
      {
         int elapsed = watch_dog.elapsed();
         int timeout = calc_timeout();
         if(elapsed >= timeout)
         {
            network.add_comment(
               "Transaction Timed Out: \"" +
               get_name() + 
               "\" after " + elapsed + " msec");
            check_retry();
         }
      }
   } // check_state


   /**
    * Requests focus for this transaction.  Once the application has invoked
    * this method, it should wait until on_focus_start() before attempting any
    * communication.
    *
    * The API provides this focus mechanism to help prevent flooding the
    * network with packets.  Using the focus, the application can have several
    * transactions pending but still allow only one transaction to use the
    * network at a time.
    */
   public void request_focus() throws Exception
   { network.request_focus(this); }


   /**
    * Called by the network when focus has been granted to this transaction. 
    */
   public abstract void on_focus_start() throws Exception;


   /**
    * Releases focus from this transaction.  If focus has not been granted, the
    * transaction will be removed from the focus queue.
    */
   public void release_focus() throws Exception
   { network.release_focus(this); }


   /**
    * Prepares to shut down this transaction by releasing focus (if any) and
    * removing this transaction from the station.
    */
   public void close() throws Exception
   {
      release_focus();
      station.remove_transaction(tran_no);
   } // close


   /**
    * Prepares the message for posting by writing the transaction number and
    * passing it to the station.
    *
    * @param message   The message that should be sent.
    */
   public void post_message(Packet message) throws Exception
   {
      message.tran_no = tran_no;
      message.priority = priority;
      station.post_message(message);
   } // post_message


   /**
    * Called by the station when a message has arrived whose transaction number
    * matches this transaction.
    *
    * @param  message   The message that has been recieved.
    */
   public abstract void on_message(Packet message) throws Exception;
   
   
   /**
    * Called by the station when a message has arrived that is identified with
    * this transaction number.  This method will perform some pre-processing
    * on the message including checking for the please wait message before 
    * passing it to the abstract on_message() method.
    * 
    * @param message  the message to be processed
    */
   protected void do_on_message(Packet message) throws Exception
   {
      if(message.protocol_type == Packet.protocol_bmp5 &&
         message.message_type == Packet.bmp5_please_wait)
      {
         try
         {
            // we will only pay attention if this transaction is in a state of waiting for a response
            if(watch_dog != null && last_message_sent != null)
            {
               short command_message_type = message.read_byte();
               int estimated_wait = message.read_uint2();
               if(command_message_type == last_message_sent.message_type)
               {
                  watch_dog.reset();
                  min_time_out = estimated_wait * 1000;
               }
            }
         }
         catch(IOException e1)
         { throw e1; }
         catch(Exception e2)
         { }
      }
      else if(!is_satisfied)
      {
         if(watch_dog != null)
            round_trip_time = watch_dog.elapsed();
         try{
        	 on_message(message); //TODO: should this throw an exception, or do we just wait?
         }catch(Exception e){
        	 throw e; // This handles null pointer exception in case where table doesn't have any records yet
         }
      }
   } // do_on_message
   
   
   /**
    * Called by the station when a message associated with this transaction is being sent.
    * 
    * @param message   the message that is being sent
    */
   public void on_message_being_sent(Packet message)
   {
      watch_dog = new Timer();
      last_message_sent = message;
      ++total_messages_sent;
   } // on_message_being_sent


   /**
    * Responsible for calculating the timeout for this transaction.  This
    * version will take into effect the timeout specified by the station and, as such, should be used to add to any other timeout
    * calculated in an overriden version.
    */
   public int calc_timeout()
   { 
      int rtn = station.get_round_trip_time();
      if(min_time_out > rtn)
         rtn = min_time_out;
      return station.get_round_trip_time(); 
   } // calc_timeout


   /**
    * Implements the retry policy for this transaction.  This version will
    * check to see if the retry count has exceeded three attempts and, if not,
    * will resend the last message (as well as reset the timer).  Otherwise,
    * on_failure() will be invoked.  This method can be overloaded to implement
    * a different policy if needed by an operation.
    */
   public void check_retry() throws Exception
   {
      if(++retry_count > 3 && last_message_sent != null)
      {
         ++total_failures;
         on_failure(failure_timeout);
      }
      else
      {
         Packet retry_message = last_message_sent;
         last_message_sent = null;
         watch_dog = null;
         post_message(retry_message);
         ++total_retries;
      }
   } // check_retry
   
   
   /**
    * @return the address of the station's neighbour
    */
   public short get_neighbour_address()
   { return station.get_neighbour_address(); }
   
   
   /**
    * @return the station
    */
   public Datalogger get_station()
   { return station; }
      
   
   /** 
    * Resets the transaction timer and retry counts
    */
   public void reset_watchdog()
   {
      retry_count = 0;
      watch_dog = null;
      last_message_sent = null;
   } // reset_watchdog
   
   
   /**
    * Returns the time from when the last command was sent and a response
    * message sent to on_message().
    * 
    * @return the round trip time for this transaction in milli-seconds.
    */
   public int get_round_trip_time()
   { return round_trip_time; }
   
   
   // the following codes define possible failure types.
   /**
    * Identifies when a link failure has occurred
    */
   public static final int failure_link = 1;

   /**
    * Identifies when a port failure has occurred (the netowkr could no longer
    * read or write to its streams, for instance.  This is more general than a
    * link failure which may only effect a neighbour address.
    */
   public static final int failure_port = 2;

   /**
    * Identifies when a timeout has occurred for this transction.
    */
   public static final int failure_timeout = 3;

   /**
    * Identifies when the address for this transaction cannot be reached (the
    * router sent us a delivery failure.
    */
   public static final int failure_unroutable = 4;
   
   
   /** 
    * Identifies when a general communications failure has occurred.  Generally, this results from messages not being formed as expected. 
    */
   public static final int failure_comms = 5;
   /**
    * Identifies when the logger cannot support a message we sent.
    */
   public static final int failure_unsupported = 6;

   
   /**
    * Called when a failure effecting this transaction has occurred.  The
    * failure will be one of the failure_xxx methods defined above.  The
    * default behaviour of this method is to close the transaction.  If more
    * behaviour needs to be added or the default behaviour is unacceptable,
    * this method should be overloaded. 
    *
    * @param reason  Specifies the cause of the failure.  This will be one of
    * the failure_xxx codes defined above: 
    * <ul>
    * <li>{@link TransactionBase#failure_comms}
    * <li>{@link TransactionBase#failure_link}
    * <li>{@link TransactionBase#failure_port}
    * <li>{@link TransactionBase#failure_timeout}
    * <li>{@link TransactionBase#failure_unroutable}
    * <li>{@link TransactionBase#failure_unsupported}
    * </ul>
    */
   public void on_failure(int reason) throws Exception
   { close(); }

   
   /**
    * This method queries the state of the is_satisified member.  It can be
    * optionally overloaded should that be needed by a derived class.  It is
    * used by the datalogger object to determine the state of the expect more
    * flag for outgoing messages.
    *  
    *  @return Should return true if this transaction has no more activity to
    *  sponsor.
    */
   public boolean get_is_satisfied()
   { return is_satisfied; }
   
   /**
    * @return This method returns the current number allocated to identify this transaction.
    */
   public short get_tran_no()
   { return tran_no; }
   

   /**
    * Called when a delivery failure message has been received
    *
    * @param reason  Specifies the reason for the failure as given in the message
    * @param protocol_type   the protocol type for the rejected message
    * @param message_type    the message type of the rejected message
    */
   protected void on_delivery_failure(
      int reason,
      byte protocol_type,
      byte message_type) throws Exception
   {
      if(last_message_sent != null &&
         protocol_type == last_message_sent.protocol_type &&
         message_type == last_message_sent.message_type)
      {
         switch(reason)
         {
         case 1:
            on_failure(failure_unroutable);
            ++total_failures;
            break;

         case 3:
            // we'll ignore queue overflow unless we've already retried too many times. 
            if(retry_count + 1 > 3)
               on_failure(failure_timeout);
            else
               ++total_failures;
            break;
            
         case 4:
            on_failure(failure_unsupported);
            ++total_failures;
            break;

         case 6:
            on_failure(failure_link);
            if(total_messages_sent == 0)
               ++total_messages_sent;
            ++total_failures;
            break;

         default:
            on_failure(failure_timeout);
            if(total_messages_sent == 0)
               ++total_messages_sent;
            ++total_failures;
            break;
         }
      }
   } // on_delivery_failure
   
   
   /**
    * Called by the datalogger when its table definitions have been updated. 
    */
   protected void on_table_defs_changed()
   { }


   /**
    * Should be overloaded by the transaction object that is designed to send the shut down packet.
    */
   protected boolean will_close()
   { return false; }
   
   
   /**
    * reference to the network on which this transaction will operate.  This
    * value will be set when the transaction is "added" to the station.
    */
   protected Network network;


   /**
    * Reference to the station on which this transaction will operate.  This
    * value will also be set when the transction is added to the station.
    */
   protected Datalogger station;


   /**
    * Identifies this transaction uniquely.  This value will be assigned by the
    * station when the transaction is added and can be changed by calling
    * change_tran_no() which will negotiate a new unique transaction number
    * with the station.
    */
   protected short tran_no;


   /**
    * Specifies the priority that messages sent by the transaction will be
    * assigned.  This also governs the order in which the router will choose
    * this transaction for focus.
    */
   protected byte priority;


   /**
    * Keeps track of the last message that was sent.
    */
   protected Packet last_message_sent;

   
   /**
    * Keeps track of the time elapsed since the last message was sent.   This
    * timer will be checked against the transaction timeout when the state is
    * checked to determine if a retry needs to be sent.
    */
   protected Timer watch_dog;


   /**
    *  Keeps track of the number of times that the last message has been
    * resent.  This value will be used to determine whether a new retry should
    * be sent following a timeout event.
    */
   protected int retry_count;
   
   
   /**
    * Specifies whether this transaction has been satisfied.  It should be
    * false if there is a perceived need to send more commands.  This value can
    * be queried by get_is_satisfied() and will be used by the datalogger to
    * set the state of the expect more bits of outgoing messages.
    */
   protected boolean is_satisfied;
   
   
   /**
    * Specifies the minimum timeout that should be used for this transaction.  
    * This value is initialised at zero but can be set if a please wait message is
    * handled.
    */
   protected int min_time_out;
   
   
   /**
    * Holds the total number of messages that have been sent by this transaction
    */
   protected int total_messages_sent;
   
   
   /**
    * Holds the number of the total number of messages sent that were retries. 
    */
   protected int total_retries;
   
   /**
    * Holds the number of failures encountered by this transaction.
    */
   protected int total_failures;
   
   /**
    * Holds the amount of time between the last command sent and the receipt of
    * the last message in units of milli-seconds.
    */
   protected int round_trip_time;
}


