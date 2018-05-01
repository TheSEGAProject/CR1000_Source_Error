/* LowLevelFile.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Friday 06 October 2006
   Last Change: Friday 06 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.io.*;
import java.util.*;



/**
 * Implements the low level logger to format the comments and I/O data to an
 * application provided stream.
 */
public class LowLevelFile implements LowLevelLogger
{
   /**
    * Public constructor
    *
    * @param output_ Stream that specifies the log output
    */
   public LowLevelFile(OutputStream output_)
   {
      buffer = new byte[16];
      buffer_len = 0;
      buffer_age = new Timer();
      output = output_;
   } // constructor

   
   /**
    * Overloads the base class to write a comment to the file.  
    */
   public void on_comment(String comment)
   {
      try
      {
         if(comment.length() > 0)
         {
            flush();
            output.write(comment.getBytes("utf8"));
            output.write('\n');
         }
         output.flush();
      }        
      catch(Exception e)
      { }
   } // on_comment


   /**
    * Overloads the interface declaration to handle formatting of low level
    * I/O.
    *
    * @param value  a byte being processed
    * @param transmitted   true if the byte is being sent, false if the byte is
    * being received. 
    */ 
   public void on_io(int value, boolean transmitted)
   {
      if(buffer_age.elapsed() >= 300 || transmitted != written)
         flush();
      if(buffer_len == 0)
      {
         buffer_age.reset();
         time_stamp = new GregorianCalendar();
         written = transmitted;
      }
      buffer[buffer_len++] = (byte)value;
      if(buffer_len >= buffer.length)
         flush();
   } // on_io
   
   
   /**
    * Overloads the interface version to check the state of the buffer
    */
   public void check_state()
   {
      if(buffer_age.elapsed() >= 300)
         flush();
   } // check_state
   

   /**
    * Called to flush the existing bytes in the buffer to the output
    */
   private void flush()
   {
      if(buffer_len > 0 && time_stamp != null)
      {
         // format the record
         Integer hour = time_stamp.get(Calendar.HOUR_OF_DAY);
         Integer minute = time_stamp.get(Calendar.MINUTE);
         Integer second = time_stamp.get(Calendar.SECOND);
         Integer msec = time_stamp.get(Calendar.MILLISECOND);
         StringBuffer line = new StringBuffer();
         Formatter formatter = new Formatter(line);
         
         formatter.format(
            "%1$02d:%2$02d:%3$02d.%4$03d %5$c ",
            hour,
            minute,
            second,
            msec,
            written ? 'T' : 'R');
         for(int i = 0; i < buffer_len; ++i)
            formatter.format("%1$02x ",new Integer(buffer[i] & 0xff));
         for(int i = buffer_len; i < buffer.length; ++i)
            line.append("   ");
         for(int i = 0; i < buffer_len; ++i)
         {
            if(Character.isLetterOrDigit((char)buffer[i]))
               line.append((char)buffer[i]);
            else
               line.append('.');
         }
         line.append('\n');
   
         // now we can output the line that we formatted
         try 
         {
            output.write(line.toString().getBytes());
         }
         catch(Exception e)
         { }
         
         // we can now reset the state of the buffer so that more can be written
         buffer_len = 0;
      }
   } // flush
   

   /**
    * reference to the output stream for this logger
    */
   private OutputStream output;

   /**
    * Used to buffer low level bytes until the buffer is complete or the
    * direction changes.
    */
   private byte[] buffer;

   /**
    * specifies the number of bytes that have been written to the buffer
    */
   private int buffer_len;
   
   /**
    * Specifies the direction, true = written, false = read.
    */
   private boolean written;

   /**
    * Used to measure the time since the first byte was added to the buffer.
    */
   private Timer buffer_age;

   /**
    * The calendar time when the first byte was added to the buffer
    */
   private Calendar time_stamp;
}
