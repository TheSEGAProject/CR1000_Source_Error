/* LowLevelDecoder.java

   Copyright (C) 2006, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Friday 06 October 2006
   Last Change: Monday 25 June 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.io.*;
import java.util.*;



/**
 * Defines an object that is able to decode a byte stream into various types of
 * PakBus related PakBus packets.  The decode() method can be called to test
 * the stream for the presence of packets and will return as validated packet
 * object when it encounters one.
 */
public class LowLevelDecoder
{
   public LowLevelDecoder()
   {
      state = state_wait_for_synch;
      storage = new byte[1026];
      storage_len = 0;
   } // constructor


   // the following fields define values for quoted bytes
   public static final int synch_byte = 0xbd;
   public static final int quoted_synch_byte = 0xdd;
   public static final int quote_byte = 0xbc;
   public static final int quoted_quote_byte = 0xdc;

   // the following fields define the 
   
   /**
    * Called by the network to decode data from the stream.  This method will
    * only process the data that is currently available on the stream and will
    * not block.  It will return at the end of any valid packet.
    *
    * @param input   Specifies the input stream
    * @param loggers The collection of low level loggers that will receive
    * notification of characters received as well as other comments from the
    * decode process.
    */
   public Packet decode(
      InputStream input,
      List<LowLevelLogger> loggers) throws Exception
   {
      int available = input.available();
      Packet rtn = null;

      for(int i = 0; rtn == null && i < available; ++i)
      {
         int temp = input.read();
         int high_nibble = (temp & 0xf0) >> 4;
         for(LowLevelLogger logger: loggers)
            logger.on_io(temp, false);
         switch(state)
         {
         case state_wait_for_synch:
            if(temp == synch_byte)
               state = state_synch_found;
            break;
            
         case state_synch_found:
            if(temp == synch_byte)
               continue;
            else if(high_nibble == Packet.control_ring ||
               high_nibble == Packet.control_reserved ||
               high_nibble == Packet.control_capabilities)
            {
               state = state_control;
               storage_len = 1;
               storage[0] = (byte)temp;
            }
            else if(high_nibble == Packet.link_off_line ||
                    high_nibble == Packet.link_ready ||
                    high_nibble == Packet.link_finished ||
                    high_nibble == Packet.link_pause)
            {
               state = state_serpkt;
               storage_len = 1;
               storage[0] = (byte)temp;
            }
            else if(temp == 0xF0)
            {
               state = state_unquoted_len;
               storage_len = 0;
            }
            else if(temp == 0xF2)
            {
               state = state_devconfig;
               storage_len = 1;
               storage[0] = (byte)temp;
            }
            else
               state = state_wait_for_synch;
            break;
            
         case state_serpkt:
         case state_control:
         case state_devconfig:
            try
            {
               if(temp == quote_byte)
                  state = state_serpkt_quoted;
               else if(temp == synch_byte)
               {
                  int sig = Utils.calc_sig(storage,storage_len);
                  if(sig == 0)
                     rtn = make_packet();
                  state = state_wait_for_synch;
               }
               else
               {
                  // store the byte as part of the packet
                  if(storage_len >= storage.length)
                     state = state_wait_for_synch;
                  else
                     storage[storage_len++] = (byte)temp;
               }
            }
            catch(Exception e)
            {
               rtn = null;
               state = state_wait_for_synch;
            }
            break;
            
         case state_serpkt_quoted:
         case state_control_quoted:
         case state_devconfig_quoted:
            if(storage_len < storage.length)
            {
               storage[storage_len++] = (byte)(temp - 0x20);
               switch(state)
               {
               case state_serpkt_quoted:
                  state = state_serpkt;
                  break;
                  
               case state_control_quoted:
                  state = state_control;
                  break;
                  
               case state_devconfig_quoted:
                  state = state_devconfig;
                  break;
               }
            }
            else
               state = state_wait_for_synch;
            break;
            
         case state_unquoted_len:
            storage[storage_len++] = (byte)temp;
            if(storage_len == 2)
            {
               state = state_unquoted_body;
               storage_len = 0;
               unquoted_body_len = ((storage[0] & 0xff) << 8) + (storage[1] & 0xff);
               if(unquoted_body_len < 8 || unquoted_body_len > storage.length)
                  state = state_wait_for_synch;
            }
            break;
            
         case state_unquoted_body:
            storage[storage_len++] = (byte)temp;
            if(storage_len == unquoted_body_len)
            {
               rtn = make_packet();
               state = state_wait_for_synch;
            }
            break;
         }
      }
      return rtn;
   } // decode
   
   
   /**
    * Creates a packet and sets its fields to appropriate values depending upon the state.
    */
   private Packet make_packet()
   {
      Packet rtn = new Packet();
      try
      {
         switch(state)
         {
         case state_serpkt:
            rtn.read_serial_packet(storage, storage_len - 2);
            break;
            
         case state_control:
            rtn.read_control_packet(storage,storage_len - 2);
            break;
            
         case state_devconfig:
            rtn.read_devconfig_packet(storage,storage_len - 2);
            break;
            
         case state_unquoted_body:
            rtn.read_unquoted_packet(storage,storage_len);
            break;
         }
         return rtn;
      }
      catch(Exception e)
      { rtn = null; }
      return rtn;
   } // make_serial_packet


   public static final int state_wait_for_synch = 0;     
   public static final int state_synch_found = 1;     
   public static final int state_serpkt = 2;          
   public static final int state_serpkt_quoted = 3;   
   public static final int state_control  = 4;         
   public static final int state_control_quoted = 5;  
   public static final int state_devconfig = 6;       
   public static final int state_devconfig_quoted = 7;
   public static final int state_unquoted_len = 8;    
   public static final int state_unquoted_body = 9;    


   /**
    * Holds the state of this decoder
    */
   private int state;


   /**
    * Holds the bytes accumulated for the sub-protocol packet
    */
   byte[] storage;


   /**
    * Holds the number of bytes that have been accumulated
    */
   int storage_len;


   /**
    * Holds the expected body length for the unquoted sub-protocol
    */
   int unquoted_body_len;
}
