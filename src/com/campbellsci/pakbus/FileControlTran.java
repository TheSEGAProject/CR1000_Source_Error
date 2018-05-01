/* FileControlTran.java

   Copyright (C) 2006, 2009 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Saturday 02 December 2006
   Last Change: Thursday 04 June 2009
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines a transaction that executes the BMP5 file control transaction.  This
 * transaction can be used to perform various file related operations on the
 * datalogger including the specification of the currently running program.
 */
public class FileControlTran extends TransactionBase
{
   // the following constants define the command codes that can be executed 
   
   /**
    * (1) Compile and run the specified program making it the program to run on
    * power up
    */
   public static final int command_compile_power_up = 1;

   /**
    * (2) Set or clear the run on power up attribute (an empty file name clears the
    * attribute).
    */
   public static final int command_set_power_up = 2;

   /**
    * (3) Mark the specified file as "hidden".  A file thus marked cannot be
    * received or listed in the directory although it can be deleted or
    * overwritten.
    */
   public static final int command_hide = 3;

   /**
    * (4) Delete the specified file.
    */
   public static final int command_delete = 4;

   /**
    * (5) Format the specified device.
    */
   public static final int command_format = 5;

   /**
    * (6) Compile and run the specified program.  Do not reset data tables if that
    * can be avoided.
    */
   public static final int command_compile_keep_data = 6;

   /**
    * (7) Stop the currently running program (no name is required).
    */
   public static final int command_stop_program = 7;

   /**
    * (8) Stop the current program and delete any associated data files.
    */
   public static final int command_stop_program_delete_files = 8;

   /**
    * (9) Evaluate the specified file as an operating system (the file must have a
    * <tt>.obj</tt> extension.
    */
   public static final int command_make_os = 9;

   /**
    * (10) Compile and run the specified program but leave the run on power up
    * attribute untouched.
    */
   public static final int command_compile_no_power_up = 10;

   /**
    * (11) Pause the currently running program (stop its execution but leave all
    * data intact)
    */
   public static final int command_pause_program = 11;

   /**
    * (12) Resume a program that was previously paused.
    */
   public static final int command_resume_program = 12;

   /**
    * (13) Stop the currently running program, delete its associated data files,
    * compile the specified program, and mark it as the program to run on
    * power up.
    */
   public static final int command_stop_delete_compile_power_up = 13;

   /**
    * (14) Stop the currently running program, delete its associated data files,
    * compile the specified program, leave the run on power up attribute
    * untouched.
    */
   public static final int command_stop_delete_compile  = 14;
   
   /**
    * (15) Move the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.
    */
   public static final int command_move_file = 15;
   
   /**
    * (16) Move the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.  When this is complete, perform the same
    * operation as {@link #command_stop_delete_compile_power_up} on the name
    * given by <tt>file_spec</tt>.
    */
   public static final int command_move_stop_delete_compile_power_up = 16;
   
   /**
    * (17) Move the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.  When this is complete, perform the same
    * operation as {@link #command_stop_delete_compile} on the name given
    * by <tt>file_spec</tt>.
    */
   public static final int command_move_stop_delete_compile = 17;
   
   /**
    * (18) Copy the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.
    */
   public static final int command_copy_file = 18;
   
   /**
    * (19) Copy the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.  When this is complete, perform the same
    * operation as {@link #command_stop_delete_compile_power_up} on the name
    * given by <tt>file_spec</tt>.
    */
   public static final int command_copy_stop_delete_compile_power_up = 19;
   
   /**
    * (20) Copy the file specified by <tt>source_name</tt> to the device and name
    * given by <tt>file_spec</tt>.  When this is complete, perform the same
    * operation as {@link #command_stop_delete_compile} on the name
    * given by <tt>file_spec</tt>.
    */
   public static final int command_copy_stop_delete_compile = 20;
   
   
   // the following constants define the possible outcomes for this transaction
   
   /** 
    * Specifies that the transaction succeeded
    */
   public static final int outcome_success = 1;
   
   /**
    * Specifies the transaction failed due to a general failure in communications.
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
    * Specifies that the transaction failed because no response was received even after retries. 
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
    * Specifies that the transaction failed because the value of the file spec parameter was invalid
    */
   public static final int outcome_invalid_file_spec = 8;
   
   /** 
    * Specifies that the transaction failed because the file command code is not supported by the datalogger.
    */
   public static final int outcome_invalid_command_code = 9;
   
   /**
    * Specifies that the datalogger does not have enough space to perform the desired operation
    */
   public static final int outcome_not_enough_storage = 10;

   /**
    * Specifies that the datalogger does not have room in the device root directory needed to carry
    * out the command.
    */
   public static final int outcome_root_dir_full = 11;
   
   /**
    * Constructor for commands that require two arguments (move and copy operations).
    * 
    * @param client_ Specifies the object that will receive the event
    * notification when this transaction has been completed.  
    * @param command_code_ Specifies the type of operation that should be
    * performed.  The following codes are supported:
    * <ul>
    *   <li>{@link FileControlTran#command_compile_power_up}
    *   <li>{@link FileControlTran#command_set_power_up}
    *   <li>{@link FileControlTran#command_hide}
    *   <li>{@link FileControlTran#command_delete}
    *   <li>{@link FileControlTran#command_format}
    *   <li>{@link FileControlTran#command_compile_keep_data}
    *   <li>{@link FileControlTran#command_stop_program}
    *   <li>{@link FileControlTran#command_stop_program_delete_files}
    *   <li>{@link FileControlTran#command_make_os}
    *   <li>{@link FileControlTran#command_compile_no_power_up}
    *   <li>{@link FileControlTran#command_pause_program}
    *   <li>{@link FileControlTran#command_resume_program}
    *   <li>{@link FileControlTran#command_stop_delete_compile_power_up}
    *   <li>{@link FileControlTran#command_stop_delete_compile}
    *   <li>{@link FileControlTran#command_move_file}
    *   <li>{@link FileControlTran#command_move_stop_delete_compile}
    *   <li>{@link FileControlTran#command_move_stop_delete_compile_power_up}
    *   <li>{@link FileControlTran#command_copy_file}
    *   <li>{@link FileControlTran#command_copy_stop_delete_compile}
    *   <li>{@link FileControlTran#command_copy_stop_delete_compile_power_up}
    * </ul>
    * <p>Not all of these commands are supported in all versions of datalogger operating
    * systems.  In the case where a datalogger does not support a given command code,
    * the transaction will fail with an outcome code of {@link
    * FileControlTran#outcome_invalid_command_code}.</p> 
    * @param file_spec_ Specifies the file or device on which the operation is
    * to be performed.  Some commands, such as pause or stop, do not require a
    * file name.  In these cases, an empty string is acceptable.  If the
    * operation is to be performed on a file, the name should specify both the
    * device and the file name using the following syntax:
    *
    * <blockquote>
    * <pre>
    * file_spec   := device-name ":" file-name.
    * device-name := "CPU" |    ; refers to the flash memory 
    *                "USR" |    ; refers to a battery backed ram disc
    *                "CRD".     ; refers to card storage
    * </pre>
    * </blockquote>
    * 
    * @param source_name_  Specifies the device and name of the file that is to be 
    * moved or copied.  This parameter will be ignored if the command code does not
    * require it.  The syntax of this name is the same as that given for 
    * <tt>file_spec_</tt>.
    */
   public FileControlTran(
      FileControlClient client_,
      int command_code_,
      String file_spec_,
      String source_name_)
   {
      client = client_;
      command_code = command_code_;
      file_spec = file_spec_;
      source_name = source_name_;
   } // constructor
   
   
   /**
    * Transaction constructor
    *
    * @param client_ Specifies the object that will receive the event
    * notification when this transaction has been completed.  
    * @param command_code_ Specifies the type of operation that should be
    * performed.  The following codes are supported:
    * <ul>
    *   <li>{@link FileControlTran#command_compile_power_up}
    *   <li>{@link FileControlTran#command_set_power_up}
    *   <li>{@link FileControlTran#command_hide}
    *   <li>{@link FileControlTran#command_delete}
    *   <li>{@link FileControlTran#command_format}
    *   <li>{@link FileControlTran#command_compile_keep_data}
    *   <li>{@link FileControlTran#command_stop_program}
    *   <li>{@link FileControlTran#command_stop_program_delete_files}
    *   <li>{@link FileControlTran#command_make_os}
    *   <li>{@link FileControlTran#command_compile_no_power_up}
    *   <li>{@link FileControlTran#command_pause_program}
    *   <li>{@link FileControlTran#command_resume_program}
    *   <li>{@link FileControlTran#command_stop_delete_compile_power_up}
    *   <li>{@link FileControlTran#command_stop_delete_compile}
    * </ul>
    * <p>Not all of these commands are supported in all versions of datalogger operating
    * systems.  In the case where a datalogger does not support a given command code,
    * the transaction will fail with an outcome code of {@link
    * FileControlTran#outcome_invalid_command_code}.</p>     
    * @param file_spec_ Specifies the file or device on which the operation is
    * to be performed.  Some commands, such as pause or stop, do not require a
    * file name.  In these cases, an empty string is acceptable.  If the
    * operation is to be performed on a file, the name should specify both the
    * device and the file name using the following syntax:
    *
    * <blockquote>
    * <pre>
    * file_spec   := device-name ":" file-name.
    * device-name := "CPU" |    ; refers to the flash memory 
    *                "USR" |    ; refers to a battery backed ram disc
    *                "CRD".     ; refers to card storage
    * </pre>
    * </blockquote>
    */
   public FileControlTran(
      FileControlClient client_,
      int command_code_,
      String file_spec_) throws Exception
   {
      client = client_;
      command_code = command_code_;
      file_spec = file_spec_;
      switch(command_code)
      {
      case command_move_file:
      case command_move_stop_delete_compile_power_up:
      case command_move_stop_delete_compile:
      case command_copy_file:
      case command_copy_stop_delete_compile_power_up:
      case command_copy_stop_delete_compile:
         throw new IllegalArgumentException("move or copy requires two arguments");
      }
   } // constructor
   
   
   @Override
   public String get_name()
   { return "FileControl(" + command_code + ", " + file_spec + ")"; }
   

   /**
    * @return The command code
    */
   public int get_command_code()
   { return command_code; }
   
   
   /**
    * @return The file specification
    */
   public String get_file_spec()
   { return file_spec; }
   
   
   /**
    * @return The source file name
    */
   public String get_source_name()
   { return source_name; }
   
   
   @Override
   public void on_focus_start() throws Exception
   {
      try
      {
         Packet command = new Packet();
         command.protocol_type = Packet.protocol_bmp5;
         command.message_type = Packet.bmp5_file_control_cmd;
         command.add_uint2(station.get_security_code());
         command.add_string(file_spec);
         command.add_byte((byte)command_code);
         if(source_name != null)
            command.add_string(source_name);
         post_message(command);
      }
      catch(Exception e)
      { }
   } // on_focus_start

   
   @Override
   public void on_message(Packet message) throws Exception
   {
      if(message.protocol_type == Packet.protocol_bmp5 &&
         message.message_type == Packet.bmp5_file_control_ack)
      {
         try
         {
            int response = message.read_byte();
            int outcome = outcome_comm_failure;
            switch(response)
            {
            case 0:
               outcome = outcome_success;
               break;
               
            case 1:
               outcome = outcome_permission_denied;
               break;
               
            case 2:
               outcome = outcome_not_enough_storage;
               break;
               
            case 13:
               outcome = outcome_invalid_file_spec;
               break;
               
            case 19:
               outcome = outcome_invalid_command_code;
               break;

            case 20:
               outcome = outcome_root_dir_full;
               break;
            }  
            on_complete(outcome, message.read_int2());
         }
         catch(Exception e)
         { on_complete(outcome_comm_failure, 0); }
      }
   } // on_message
   

   @Override
   public void on_failure(int reason) throws Exception
   {
      int outcome = FileControlTran.outcome_comm_failure;
      switch(reason)
      {
      case failure_comms:
         outcome = FileControlTran.outcome_comm_failure;
         break;
         
      case failure_link:
         outcome = FileControlTran.outcome_link_failure;
         break;
         
      case failure_port:
         outcome = FileControlTran.outcome_port_failure;
         break;
         
      case failure_timeout:
         outcome = FileControlTran.outcome_failure_timeout;
         break;
         
      case failure_unroutable:
         outcome = FileControlTran.outcome_failure_unroutable;
         break;
         
      case failure_unsupported:
         outcome = FileControlTran.outcome_invalid_command_code;
         break;
      }
      on_complete(outcome, 0);
   } // on_failure


   /**
    * Called when this transaction is complete
    */
   private void on_complete(
      int outcome,
      int hold_off) throws Exception
   {
      close();
      if(client != null)
         client.on_complete(this,outcome,hold_off);
   } // on_complete
   
   
   /**
    * Reference to the client object.
    */
   private FileControlClient client;
   
   
   /**
    * Specifies the operation to perform.
    */
   private int command_code;
   
   /**
    * Specifies the parameter for the command.
    */
   private String file_spec;
   
   /**
    * Specifies the source file name for move and copy operations
    */
   private String source_name;
}
