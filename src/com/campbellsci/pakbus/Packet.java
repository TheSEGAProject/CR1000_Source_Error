/* Packet.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Tuesday 26 September 2006
   Last Change: Saturday 12 April 2008
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;



/** 
 * Defines a PakBus message body and its header data.
 */
public class Packet
{
   // The following fields control the addressing and disposition of this message. 
   public short dest_address;
   public short source_address;
   public short neighbour_dest_address;
   public short neighbour_source_address;
   public byte priority;
   public byte expect_more_code;
   public short protocol_type;
   public short message_type;
   public short tran_no;
   public byte link_state;
   public boolean short_header;
   public byte control_type;
   public byte capabilities;
   public byte sub_protocol;

   // the following fields are used to keep the contents of the message.
   private byte[] storage;
   private int storage_len;
   private int read_index;

   // defines the codes for various subprotocols
   public static final byte sub_control = 0;
   public static final byte sub_link_state = 1;
   public static final byte sub_unquoted = 2;
   public static final byte sub_ack_retry = 3;
   public static final byte sub_devconfig = 4;
   
   
   // the following members define possible values for the expect more code
   public static final byte expect_last = 0;
   public static final byte expect_more = 1;
   public static final byte expect_neutral = 2;
   public static final byte expect_reverse = 3;

   // the following fields define possible values for the priority field
   public static final byte pri_low = 0;
   public static final byte pri_normal = 1;
   public static final byte pri_high = 2;
   public static final byte pri_extra_high = 3;

   // the following fields define possible values for the protocol_type field
   public static final byte protocol_pakctrl = 0;
   public static final byte protocol_bmp5 = 1;

   // the following fields define possible values for the link_state member.
   public static final byte link_off_line = 0x08;
   public static final byte link_ring = 0x09;
   public static final byte link_ready = 0x0A;
   public static final byte link_finished = 0x0B;
   public static final byte link_pause = 0x0C;
   
   // the following fields define possible values for the control packet type codes
   public static final byte control_ring = 0x09;
   public static final byte control_reserved = 0x0D;
   public static final byte control_capabilities = 0x0E;
   
   // the following fields define possible values for PakCtrl message types
   public static final short pakctrl_delivery_failure = 0x81;
   public static final short pakctrl_hello_cmd = 0x09;
   public static final short pakctrl_hello_ack = 0x89;
   public static final short pakctrl_hello_req = 0x0e;
   public static final short pakctrl_bye = 0x0d;
   public static final short pakctrl_reset = 0x0c;
   public static final short pakctrl_clock = 0x02;
   public static final short pakctrl_echo_cmd = 0x05;
   public static final short pakctrl_echo_ack = 0x85;
   public static final short pakctrl_get_settings_cmd = 0x07;
   public static final short pakctrl_get_settings_ack = 0x87;
   
   
   // the following fields define possible values for the BMP5 message types
   public static final short bmp5_please_wait = 0xa1;
   public static final short bmp5_clock_set_cmd = 0x17;
   public static final short bmp5_clock_set_ack = 0x97;
   public static final short bmp5_file_send_cmd = 0x1c;
   public static final short bmp5_file_send_ack = 0x9c;
   public static final short bmp5_file_receive_cmd = 0x1d;
   public static final short bmp5_file_receive_ack = 0x9d;
   public static final short bmp5_file_control_cmd = 0x1e;
   public static final short bmp5_file_control_ack = 0x9e;
   public static final short bmp5_get_program_stats_cmd = 0x18;
   public static final short bmp5_get_program_stats_ack = 0x98;
   public static final short bmp5_collect_data_cmd = 0x09;
   public static final short bmp5_collect_data_ack = 0x89;
   public static final short bmp5_one_way_table_def = 0x20;
   public static final short bmp5_one_way_data = 0x14;
   public static final short bmp5_get_values_cmd = 0x1a;
   public static final short bmp5_get_values_ack = 0x9a;
   public static final short bmp5_set_values_cmd = 0x1b;
   public static final short bmp5_set_values_ack = 0x9b;
   public static final short bmp5_user_io_cmd = 0x0b;
   public static final short bmp5_user_io_ack = 0x8b;
   
   
   /**
    * Defines the default constructor
    */
   public Packet()
   {
      source_address = 0;
      dest_address = 0;
      neighbour_source_address = 0;
      neighbour_dest_address = 0;
      protocol_type = protocol_bmp5;
      message_type = 0;
      tran_no = 0;
      read_index = storage_len = 0;
      short_header = false;
      sub_protocol = sub_link_state;
   }


   /**
    * Appends the contents of the specified buffer to the message buffer.
    */
   public void add_bytes(byte[] buff, int buff_len)
   {
      reserve(storage_len + buff.length);
      for(int i = 0; i < buff_len; ++i)
         storage[storage_len++] = buff[i];
   } // add_bytes


   /**
    * Appends the specified byte to the message contents
    */
   public void add_byte(Byte val)
   {
      reserve(storage_len + 1);
      storage[storage_len++] = val.byteValue();
   }


   /**
    * Appends a boolean value to the message contents
    *
    * @param val  The boolean value to be added
    */
   public void add_bool(boolean val)
   { add_byte(val ? (byte)0xff : (byte)0); }


   /**
    * Appends the two byte signed integer value to the message contents.
    */
   public void add_int2(Short val)
   {
      reserve(storage_len + 2);
      storage[storage_len++] = (byte)((val & 0xFF00) >> 8);
      storage[storage_len++] = (byte)(val & 0x00FF);
   } // add_short

   
   /**
    * Appends a two byte unsigned integer to the message
    */
   public void add_uint2(Integer val)
   {
      reserve(storage_len + 2);
      storage[storage_len++] = (byte)((val & 0xFF00) >> 8);
      storage[storage_len++] = (byte)(val & 0x00FF);
   }
   
   
   /**
    * Appends the two byte signed integer to the message contents least significant byte
    * first.
    *
    * @param val  Specifies the value to append
    */
   public void add_int2_lsf(Short val)
   {
      reserve(storage_len + 2);
      storage[storage_len++] = (byte)(val & 0x00FF);
      storage[storage_len++] = (byte)((val & 0xFF00) >> 8);
   } // add_short_lsf

   
   /**
    * Appends a two byte unsigned integer in little endian format (least significant byte first) to the message.
    */
   public void add_uint2_lsf(Integer val)
   {
      reserve(storage_len + 1);
      storage[storage_len++] = (byte)(val & 0x00FF);
      storage[storage_len++] = (byte)((val & 0xFF00) >> 8);
   }
   
   
   /**
    * Appends a four byte signed integer to the message contents
    */
   public void add_int4(Integer val)
   {
      reserve(storage_len + 4);
      storage[storage_len++] = (byte)((val & 0xFF000000) >> 24);
      storage[storage_len++] = (byte)((val & 0x00FF0000) >> 16);
      storage[storage_len++] = (byte)((val & 0x0000FF00) >> 8);
      storage[storage_len++] = (byte) (val & 0x000000FF);
   } // add_int
   
   
   /**
    * Appends a four byte unsigned integer to the message in big endian format
    */
   public void add_uint4(Long val)
   {
      reserve(storage_len + 4);
      storage[storage_len++] = (byte)((val & 0xFF000000) >> 24);
      storage[storage_len++] = (byte)((val & 0x00FF0000) >> 16);
      storage[storage_len++] = (byte)((val & 0x0000FF00) >> 8);
      storage[storage_len++] = (byte) (val & 0x000000FF);
   }
   
   
   /**
    * Appends a four byte signed integer to the message contents with the least significant byte written first.
    * 
    *  @param val  Specifies the value to append
    */
   public void add_int4_lsf(Integer val)
   {
      reserve(storage_len + 4);
      storage[storage_len++] = (byte) (val & 0x000000FF);
      storage[storage_len++] = (byte)((val & 0x0000FF00) >> 8);
      storage[storage_len++] = (byte)((val & 0x00FF0000) >> 16);
      storage[storage_len++] = (byte)((val & 0xFF000000) >> 24);
   } // add_int_lsf

   
   /**
    * Appends a four byte unsigned integer to the message in little endian format (least significant byte first)
    */
   public void add_uint4_lsf(Long val)
   {
      reserve(storage_len + 4);
      storage[storage_len++] = (byte) (val & 0x000000FF);
      storage[storage_len++] = (byte)((val & 0x0000FF00) >> 8);
      storage[storage_len++] = (byte)((val & 0x00FF0000) >> 16);
      storage[storage_len++] = (byte)((val & 0xFF000000) >> 24);
   }
   
   
   /**
    * Adds the contents of the string as a null terminated UTF-8 array
    */
   public void add_string(String val) throws Exception
   {
      if(val.length() > 0)
      {
         byte[] temp = val.getBytes("UTF8");
         add_bytes(temp,temp.length);
         if(temp[temp.length - 1] != 0)
            add_byte((byte)0);
      }
      else
         add_byte((byte)0);
   } // add_string
   
   
   /**
    * Appends a four byte floating point value to the message
    */
   public void add_float(Float val)
   { add_int4(Float.floatToIntBits(val.floatValue())); }


   /**
    * Adds the specified time stamp to the message as a nsec (seconds + nsec
    * past 1990).
    *
    * @param time  Specifies the calendar time to add
    */
   public void add_nsec(LoggerDate time)
   {
      add_int4((int)time.get_secs_since_1990());
      add_int4((int)time.get_nsec());
   } // add_nsed


   /**
    * Adds the specified time stamp to the message as nsec_lsf (seconds + nsec
    * past 1990 represented least significant bytes first).
    *
    * @param time  Specifies the time stamp to add.
    */
   public void add_nsec_lsf(LoggerDate time)
   {
      add_int4_lsf((int)time.get_secs_since_1990());
      add_int4_lsf((int)time.get_nsec());
   } // add_nsec_lsf


   /**
    * Adds the specified time stamp to the message as seconds (seconds since
    * 1990)
    *
    * @param time  Specifies the time to add
    */
   public void add_sec(LoggerDate time)
   { add_int4((int)time.get_secs_since_1990()); }

   
   /** 
    * Attempts to read the specified number of bytes
    */
   public byte[] read_bytes(int len) throws Exception
   {
      if(read_index + len > storage_len)
         throw new Exception("Attempt to read past the message end");
      byte[] rtn = new byte[len];
      for(int i = 0; i < len; ++i)
         rtn[i] = storage[read_index++];
      return rtn;
   } // read_bytes
   
   
   /**
    * Attempts to read the specified number of bytes into the provided
    * buffer.  Returns the actual number of bytes read
    */
   public int read_bytes(byte[] buff, int buff_len) throws Exception
   {
      int rtn = buff_len;
      if(buff.length < buff_len)
         rtn = buff.length;
      if(read_index + rtn > storage_len)
         throw new Exception("Attempt to read past the message end");
      for(int i = 0; i < rtn; ++i)
         buff[i] = storage[read_index + i];
      read_index += rtn;
      return rtn;
   }
   
   
   /**
    * Reads a single byte from the message content
    */
   public byte read_byte() throws Exception
   { 
      if(read_index + 1 > storage_len)
         throw new Exception("Attempt to read past the message end");
      return storage[read_index++];
   } // read_byte


   /**
    * @return The boolean value at the read index
    */
   public boolean read_bool() throws Exception
   {
      byte bval = read_byte();
      boolean rtn = false;
      if(bval != 0)
         rtn = true;
      return rtn;
   } // read_bool

      
   /**
    * Reads a two byte integer from the message body
    */
   public short read_int2() throws Exception
   {
      if(read_index + 2 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int b1 = storage[read_index];
      int b2 = storage[read_index + 1];
      short rtn = (short)(
         ((short)b1 & 0xff) << 8 |
         ((short)b2 & 0xff));
      read_index += 2;
      return rtn; 
   } // read_short

   
   /**
    * Reads a two byte unsigned integer from the message content in big endian order
    */
   public int read_uint2() throws Exception
   {
      if(read_index + 2 > storage_len)
         throw new Exception("Attempt to read past message end");
      int b1 = storage[read_index];
      int b2 = storage[read_index + 1];
      int rtn = ((b1 & 0xff) << 8) + (b2 & 0xff);
      read_index += 2;
      return rtn;
   }

   
   /**
    * Reads a two byte signed integer least significant byte first from the message
    * body.
    *
    * @return The value read
    */
   public short read_int2_lsf() throws Exception
   {
      if(read_index + 2 > storage_len)
         throw new Exception("Attempt to read past the message end");
      short rtn = (short)(
         ((short)storage[read_index + 1] & 0xff) << 8 |
         ((short)storage[read_index] & 0xff));
      read_index += 2;
      return rtn;
   } // read_short_lsf
   
   
   /**
    * Reads a two byte unsigned integer least significant first from the message
    */
   public int read_uint2_lsf() throws Exception
   {
      if(read_index + 2 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int b1 = storage[read_index + 1];
      int b2 = storage[read_index];
      int rtn = ((b1 & 0xff) << 8) + (b2 & 0xff);
      read_index += 2;
      return rtn;
   }


   /**
    * Reads a four byte signed integer from the message body.
    */
   public int read_int4() throws Exception
   {
      if(read_index + 4 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int b1 = storage[read_index];
      int b2 = storage[read_index + 1];
      int b3 = storage[read_index + 2];
      int b4 = storage[read_index + 3];
      int rtn =
         ((b1 & 0xff) << 24) |
         ((b2 & 0xff) << 16) |
         ((b3 & 0xff) << 8) |
         ((b4 & 0xff));
      read_index += 4;
      return rtn;
   } // read_int

   
   /**
    * Reads a four byte unsigned integer from the message body with the most significant byte first.
    */
   public long read_uint4() throws Exception
   {
      if(read_index + 4 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int b1 = storage[read_index];
      int b2 = storage[read_index + 1];
      int b3 = storage[read_index + 2];
      int b4 = storage[read_index + 3];
      long rtn =
         ((b1 & 0xff) << 24) |
         ((b2 & 0xff) << 16) |
         ((b3 & 0xff) << 8) |
         (b4 & 0xff);
      read_index += 4;
      return rtn;
   } // read_uint4
   
   
   /**
    * Reads a four byte signed integer from the message body as least significant byte
    * first.
    *
    * @return The value read
    */
   public int read_int4_lsf() throws Exception
   {
      if(read_index + 4 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int rtn = (((int)storage[read_index + 3] & 0xff) << 24) |
            (((int)storage[read_index + 2] & 0xff) << 16) |
            (((int)storage[read_index + 1] & 0xff) << 8) |
            ((int)storage[read_index] & 0xff);
      read_index += 4;
      return rtn;
   } // read_int_lsf


   /**
    * Reads a four byte unsigned integer from the message body as least significant byte first.
    *
    * @return The integer value as a long
    */
   public long read_uint4_lsf() throws Exception
   {
      if(read_index + 4 > storage_len)
         throw new Exception("Attempt to read past the message end");
      int b1 = storage[storage_len + 3];
      int b2 = storage[storage_len + 2];
      int b3 = storage[storage_len + 1];
      int b4 = storage[storage_len];
      long rtn =
         ((b1 & 0xff) << 24) |
         ((b2 & 0xff) << 16) |
         ((b3 & 0xff) << 8) |
         (b4 & 0xff);
      read_index += 4;
      return rtn;
   } // read_uint4_lsf


   /**
    * Reads a four byte float from the message body
    */
   public float read_float() throws Exception
   {
      int int_val = read_int4();
      return Float.intBitsToFloat(int_val);
   } // read_float


   /**
    * Reads a four byte float least significant byte first from the message
    * body.
    *
    * @return the value read
    */
   public float read_float_lsf() throws Exception
   {
      int int_val = read_int4_lsf();
      return Float.intBitsToFloat(int_val);
   } // read_float_lsf


   /**
    * Reads a null terminated string from the message body
    */
   public String read_string()
   {
      String rtn = new String();
      while(storage[read_index] != 0 && read_index < storage_len)
         rtn += (char)storage[read_index++];
      if(read_index < storage_len && storage[read_index] == 0)
         ++read_index;
      return rtn;
   } // read_string
   

   /**
    * Reads a time stamp from the message body encoded as seconds + nanosecs
    * since 1 jan 1990.
    *
    * @return The LoggerDate object that was read
    */
   public LoggerDate read_nsec() throws Exception
   { return new LoggerDate((long)read_int4(),(long)read_int4()); }
   
   
   /**
    * Reads a time stamp from the message body encoded as seconds + nanosecs
    * since 1 jan 1990 with least significant byte first.
    * 
    * @return The date object that was read
    */
   public LoggerDate read_nsec_lsf() throws Exception
   { return new LoggerDate((long)read_int4_lsf(),(long)read_int4_lsf()); }
   
   
   /**
    * Reads a time stamp from the message body encoded as seconds since
    * 1 jan 1990.
    * 
    * @return The date object that was read
    */
   public LoggerDate read_sec() throws Exception
   { return new LoggerDate((long)read_int4(),(long)0); }

   
   /**
    * Moves the read pointer the specified number of bytes (or past the end of
    * the message).
    */
   public void move_past(int len)
   {
      if(read_index + len >= storage_len)
         read_index = storage_len;
      else
         read_index += len;
   } // move_past


   /**
    * Resets the read index to the beginning of the message content.
    */
   public void reset()
   { read_index = 0; }


   /**
    * Returns the number of bytes remaining in the message body that can be
    * read.
    */
   public int whats_left()
   { return storage_len - read_index; }


   /**
    *  Clears the message for both reading and writing.
    */
   public void clear()
   { storage_len = read_index = 0; }


   /**
    * Returns the current read index
    *
    * @return The current value of read_index
    */
   public int get_read_index()
   { return read_index; }


   /**
    * Returns a fragment from the message body.  starting_offset must be less
    * than ending_offset and both must be less than the message length.
    *
    * @param  start_pos  Specifies the starting offset
    * @param  end_pos    Specifies the ending offset
    * @return  The fragment bounded by [start_pos, end_pos)
    */
   public byte[] get_fragment(int start_pos, int end_pos) throws Exception
   {
      if(start_pos > end_pos ||
         start_pos >= storage_len ||
         end_pos >= storage_len ||
         start_pos < 0 ||
         end_pos < 0)
         throw new Exception("invalid fragment position pointers");
      byte[] rtn = new byte[end_pos - start_pos + 1];
      for(int i = start_pos; i < end_pos; ++i)
         rtn[i - start_pos] = storage[i];
      return rtn;
   } // get_fragment


   /**
    * Initialises the contents of the message assuming a SerialPacket low level
    * protocol structure.  An exception will be thrown if the provided buffer
    * is too small or if another error occurrs.
    */
   public void read_serial_packet(
      byte[] buff,
      int buff_len) throws Exception
   {
      // we assume that the framing (signature and synch bytes have already
      // been stripped and that the dequoting process has already been
      // applied.
      if(buff.length < buff_len)
         throw new Exception("Invalid buffer size");
      if(buff_len >= 8)
         short_header = false;
      else if(buff_len >= 4)
         short_header = true;
      else
         throw new Exception("Invalid message length");

      // we will now decode the header fields based upon serial packet positions
      sub_protocol = sub_link_state;
      link_state = (byte)((buff[0] & 0xf0) >> 4);
      neighbour_dest_address = (short)(((buff[0] & 0x0f) << 8) | (buff[1] & 0xff));
      expect_more_code = (byte)((buff[2] & 0xc0) >> 6);
      priority = (byte)((buff[2] & 0x30) >> 4);
      neighbour_source_address = (short)(((buff[2] & 0x0f) << 8) | (buff[3] & 0xff));
      if(!short_header)
      {
         // read the rest of the header parameters (we will ignore hop count)
         protocol_type = (byte)((buff[4] & 0xf0) >> 4);
         dest_address = (short)(((buff[4] & 0x0f) << 8) | (buff[5] & 0xff));
         source_address = (short)(((buff[6] & 0x0f) << 8) | (buff[7] & 0xff));
         
         // if there are enough bytes left, we will initialise the transaction
         // number, message types and message body fields.
         if(buff_len >= 10)
         {
            message_type = (short)(buff[8] & 0xff);
            tran_no = (short)(buff[9] & 0xff);
            storage_len = buff_len - 10;
            if(storage == null || storage.length < storage_len)
               storage = new byte[storage_len];
            for(int i = 10; i < buff_len; ++i)
               storage[i - 10] = buff[i];
         }
         else
         {
            message_type = tran_no = 0;
            storage_len = 0;
            read_index = 0;
         }
      }
      else
      {
         storage_len = 0;
         read_index = 0;
         dest_address = neighbour_dest_address;
         source_address = neighbour_source_address;
         priority = pri_normal;
         protocol_type = protocol_pakctrl; 
      }
   } // read_serial_packet


   /**
    * Reads a control message format
    *
    * @param buff   Should specify the packet contents after being unquoted.
    * Should begin with the first byte following the synch byte.
    * @param buff_len  Should specify the number of bytes available and should
    * exclude the signature nullifier.
    */
   public void read_control_packet(
      byte[] buff,
      int buff_len) throws Exception
   {
      // this format is mostly similar to that used for the serial packet.  the major difference is
      // in the interpretation of the control_type and the capabilities field.
      read_serial_packet(buff,buff_len);
      sub_protocol = sub_control;
      control_type = (byte)((buff[0] & 0xf0) >> 4);
      capabilities = (byte)((buff[2] & 0xf0) >> 4);
   } // read_control_packet


   /**
    * Reads a message of the devconfig format (in this case, all of the
    * addressing fields should be ignored).
    *
    * @param buff  Should specify the beginning of the packet following the
    * synch byte 
    * @param buff_len  Should specify he number of bytes available.  This count
    * should exclude the signature nullifier bytes at the end.
    */
   public void read_devconfig_packet(
      byte[] buff,
      int buff_len) throws Exception
   {
      if(buff_len < 3)
         throw new Exception("Content is too short for a devconfig packet");
      sub_protocol = sub_devconfig;
      message_type = buff[1];
      tran_no = buff[2];
      storage_len = buff_len - 3;
      for(int i = 0; i < storage_len; ++i)
         storage[i] = buff[i + 3];
   } // read_devconfig_packet


   /**
    * Reads a message of the unquoted format.
    *
    * @param buff   Specifies the buffer to be read.  This buffer should begin
    * following the msgDataLen field which should have been consumed in the
    * decode state machine.
    *
    * @param buff_len  Specifies the number of bytes that are available. 
    */
   public void read_unquoted_packet(
      byte[] buff,
      int buff_len) throws Exception
   {
      // it turns out that this format is almost exactly the same as the serial
      // packet format with the exception that the link state parameter is
      // ignored.
      if(buff_len < 8)
         throw new Exception("Unquoted buffer is too small");
      
      sub_protocol = sub_unquoted;
      link_state = link_ready;
      neighbour_dest_address = (short)(((buff[0] & 0x0f) << 8) | (buff[1] & 0xff));
      expect_more_code = (byte)((buff[2] & 0xc0) >> 6);
      priority = (byte)((buff[2] & 0x30) >> 4);
      neighbour_source_address = (short)(((buff[2] & 0x0f) << 8) | (buff[3] & 0xff));
      protocol_type = (byte)((buff[4] & 0xf0) >> 4);
      dest_address = (short)(((buff[4] & 0x0f) << 8) | (buff[5] & 0xff));
      source_address = (short)(((buff[6] & 0x0f) << 8) | (buff[7] & 0xff));
      if(buff_len >= 10)
      {
         message_type = (short)(buff[8] & 0xff);
         tran_no = (short)(buff[9] & 0xff);
         storage_len = buff_len - 10;
         if(storage == null || storage_len < storage_len)
            storage = new byte[storage_len];
         for(int i = 10; i < buff_len; ++i)
            storage[i - 10] = buff[i];
      }
      else
      {
         message_type = tran_no = 0;
         read_index = 0;
         storage_len = 0;
      }
   } // read_unquoted_format
   


   /**
    * Produces a packet laid out according to the serial packet protocol
    * specifications.
    */
   public byte[] write_serial_packet()
   {
      byte[] rtn = null;
      if(!short_header)
      {
         if(message_type != 0)
            rtn = new byte[storage_len + 10];
         else
            rtn = new byte[8];
      }
      else
         rtn = new byte[4];
      rtn[0] = (byte)((link_state << 4) |
                      ((neighbour_dest_address & 0x0F00) >> 8));
      rtn[1] = (byte)(neighbour_dest_address & 0x00FF);
      if(link_state != link_ring)
      {
         rtn[2] = (byte)((expect_more_code << 6) |
                         (priority << 4) |
                         ((neighbour_source_address & 0x0F00) >> 8));
      }
      else
      {
         rtn[2] = (byte)((capabilities << 4) |
                         ((neighbour_source_address & 0x0f00) >> 8));
      }
      rtn[3] = (byte)(neighbour_source_address & 0x00FF);
      if(!short_header)
      {
         rtn[4] = (byte)((protocol_type << 4) |
                         ((dest_address & 0x0F00) >> 8));
         rtn[5] = (byte)(dest_address & 0x00FF);
         rtn[6] = (byte)((source_address & 0x0F00) >> 8);
         rtn[7] = (byte)(source_address & 0x00FF);
         if(message_type != 0)
         {
            rtn[8] = (byte)(message_type & 0xff);
            rtn[9] = (byte)(tran_no & 0xff);
            for(int i = 0; i < storage_len; ++i)
               rtn[10 + i] = storage[i];
         }
      }
      return rtn;
   } // write_serial_packet


   /**
    * Writes the contents of this packet as an unquoted packet.  This will include the packet
    * length.
    *
    * @return The packet with its length
    */
   public byte[] write_unquoted_packet()
   {
      byte[] rtn = null;
      if(short_header)
      {
         rtn = new byte[6];
         rtn[0] = 0;
         rtn[1] = 4;
      }
      else
      {
         int packet_len = 10 + storage_len;

         if(message_type == 0)
            packet_len = 8;
         rtn = new byte[packet_len + 2];
         rtn[0] = (byte)((packet_len & 0xff00) >> 8);
         rtn[1] = (byte)(packet_len & 0x00ff);
      }
      rtn[2] = (byte)((neighbour_dest_address & 0x0f00) >> 8);
      rtn[3] = (byte)(neighbour_dest_address & 0x00ff);
      rtn[4] = (byte)((expect_more_code << 6) |
                      (byte)(priority << 4) |
                      (byte)((neighbour_source_address & 0x0f00) >> 8));
      rtn[5] = (byte)(neighbour_source_address & 0x00ff);
      if(!short_header)
      {
         rtn[6] = (byte)((protocol_type << 4) |
                         (byte)((dest_address & 0x0F00) >> 8));
         rtn[7] = (byte)(dest_address & 0x00FF);
         rtn[8] = (byte)((source_address & 0x0F00) >> 8);
         rtn[9] = (byte)(source_address & 0x00FF);
         if(message_type != 0)
         {
            rtn[10] = (byte)(message_type & 0xff);
            rtn[11] = (byte)(tran_no & 0xff);
            for(int i = 0; i < storage_len; ++i)
               rtn[12 + i] = storage[i];
         }
      }
      return rtn;
   } // write_unquoted_packet


   /**
    * Evaluates whether this message is a control packet and whether the capabilities
    * bits indicate support for the unquoted sub-protocol.
    *
    * @return true if this is a control packet and the capabilities indicate
    * support for the unquoted sub-protocol.
    */
   public boolean supports_unquoted()
   {
      boolean rtn = false;
      if(sub_protocol == sub_control && (capabilities & 0x01) != 0)
         rtn = true;
      return rtn;
   } // supports_unquoted


   /**
    * Evaluates whether this message is a control packet and whether the capabilities
    * bits indicate support for the retry sub-protocol
    *
    * @return true if this is a control packet and support for the ack-retry
    * sub-protocol is supported.
    */
   public boolean supports_ack_retry()
   {
      boolean rtn = false;
      if(sub_protocol == sub_control && (capabilities & 0x02) != 0)
         rtn = true;
      return rtn;
   } // supports_ack_retry
    

   /**
    * Ensures that the storage buffer for this packet is at least as large
    * as the specified length.
    *
    * @param len  Specifies the new minimum length for the storage buffer
    */
   protected void reserve(int len)
   {
      // we need to check to make sure that the buffer has the capacity for the specified
      // length.  If it does not, we will re-allocate it so that it does.
      if(storage == null)
         storage = new byte[len];
      else if(storage.length < len)
      {
         // we don't have enough capacity.  To prevent re-allocation each time, we will double the
         // requested amount.
         byte[] temp = new byte[len * 2];
         for(int i = 0; i < storage.length; ++i)
            temp[i] = storage[i];
         storage = temp;
      }
   } // reserve 
};

    

