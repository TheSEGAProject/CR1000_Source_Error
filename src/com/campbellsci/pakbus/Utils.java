/* Utils.java

   Copyright (C) 2006, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Friday 22 September 2006
   Last Change: Monday 25 June 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Defines static methods of general usefulness.  
 * 
 * @author jon
 *
 */
final public class Utils 
{
   /**
    * Calculates the signature of an arbitrary array of bytes.  This version
    * can continue the calculation previously started
    *
    * @param buff   Reference to the byte buffer
    * @param len    Specifies the number of bytes to consider
    * @param seed   Specifies the result of previous calculations.  Should be
    *               0xAAAA for a new seqeunce.
    * @return       The signature of the sequence.  This will be in the range of 0 - 65535
    */
   static public int calc_sig(
       byte[] buff,
       int len,
       int seed)
   {
      int i, j;
      int rtn = seed;
      for(i = 0; i < len && i < buff.length; ++i)
      {
         j = rtn;
         rtn = (rtn << 1) & 0x01FF;
         if(rtn >= 0x100)
            ++rtn;
         rtn = ((rtn + (j >> 8) + buff[i]) & 0xFF) | (j << 8); 
      }
      return rtn & 0x0000FFFF;
   } // calc_sig


   /**
    * Calculates the signature for a new sequence of bytes.
    *
    * @param buff  Reference to the byte buffer
    * @param buff_len  The number of bytes to consider
    * @return the sequence signature
    */
   static public int calc_sig(
      byte[] buff,
      int buff_len)
   { return Utils.calc_sig(buff,buff_len,(char)0xAAAA); }
      
   
   
   /**
    * Calculates the two bytes that, if appended to a buffer whose signature is
    * calculated as sig, will cause the signature of the buffer to be zero.
    *
    * @param  sig  The signature to be nullified
    * @return the signature nullifier
    */
   static public int calc_sig_nullifier(
      int sig)
   {
      // calculate the value for the most significant byte. Then run this value
      // through the signature algorithm using the specified signature as
      // seed. The calculation is designed to cause the least significant byte
      // in the signature to become zero.
      int new_seed = (sig << 1) & 0x1ff;
      byte[] null1 = new byte[1];
      int new_sig;

      if(new_seed >= 0x0100)
         new_seed++;
      null1[0] = (byte)(0x0100 - (new_seed + (sig >> 8)));
      new_sig = calc_sig(null1,1,sig);

      // now perform the same calculation for the most significant byte in the
      // signature. This time we will use the signature that was calculated
      // using the first null byte
      int null2;

      new_seed = (new_sig << 1) & 0x01FF;
      if(new_seed >= 0x0100)
         new_seed++;
      null2 = 0x0100 - (new_seed + (new_sig >> 8));

      // now form the return value placing null1 one in the most signicant byte
      // location
      int rtn = null1[0] & 0xff;
      rtn <<= 8;
      rtn += (null2 & 0xff);
      return rtn;
   } // calc_sig_nullifier
   
   
   static final long neg_nan_min = 0xFFF0000000000001L;
   static final long neg_nan_max = 0xFFFFFFFFFFFFFFFFL;
   static final long pos_nan_min = 0x7FF0000000000001L;
   static final long pos_nan_max = 0x7FFFFFFFFFFFFFFFL;
   static final long pos_inf_val = 0x7FF0000000000000L;
   static final long neg_inf_val = 0xFFF0000000000000L;
   
   
   /**
    * Formats a floating point number according to CSI specifications.
    * Very large or very small numbers will be formatted with scientific
    * notation.  +Inf, -Inf, and Nan values will be formatted with "INF", 
    * "-INF", and "NAN" respectively.  All other values will be formatted
    * as fixed point with the specified precision although trailing zeroes
    * (and the decimal point, if applicable) will be cut off.
    * 
    * @param  val   the value to be formatted
    * @param  precision  Specifies the number of significant digits (the max for type
    * float is seven).
    * @param  specials_in_quotes   Specifies that special values should be enclosed
    * in quotation marks.
    * @param  specials_as_numbers  Specifies the the "special" values, INF, -INF, and
    * NAN should be formatted as "7999", "-7999", or "7999" respectively.
    * @return The formatted string
    */
   static public String csi_float_to_string(
      double val,
      int precision,
      boolean specials_in_quotes,
      boolean specials_as_numbers)
   {
      StringBuilder rtn = new StringBuilder();
      long bits = Double.doubleToLongBits(val);
      if((bits >= neg_nan_min && bits <= neg_nan_max) ||
         (bits >= pos_nan_min && bits <= pos_nan_max))
      {
         if(!specials_as_numbers)
         {
            if(specials_in_quotes)
               rtn.append('\"');
            rtn.append("NAN");
            if(specials_in_quotes)
               rtn.append('\"');
         }
         else
            rtn.append("7999");
      }
      else if(bits == pos_inf_val)
      {
         if(!specials_as_numbers)
         {
            if(specials_in_quotes)
               rtn.append('\"');
            rtn.append("INF");
            if(specials_in_quotes)
               rtn.append('\"');
         }
         else
            rtn.append("7999");
      }
      else if(bits == neg_inf_val)
      {
         if(!specials_as_numbers)
         {
            if(specials_in_quotes)
               rtn.append('\"');
            rtn.append("-INF");
            if(specials_in_quotes)
               rtn.append('\"');
         }
         else
            rtn.append("-7999");
      }
      else
      {
         String format_spec = "%1$." + precision + "G";
         rtn.append(String.format(format_spec, val));   
      }
      return rtn.toString();
   } // csi_float_to_string
   
   
   /**
    * Converts a string formatted by the conventions of csi_string_to_float()
    * back to a floating point value.
    * 
    * @param val  Specifies the string to be converted
    * @return the converted value
    */
   static public Double csi_string_to_float(String val)
   {
      double rtn = 0;
      if(val.compareToIgnoreCase("INF") == 0)
         rtn = Double.POSITIVE_INFINITY;
      else if(val.compareToIgnoreCase("-INF") == 0)
         rtn = Double.NEGATIVE_INFINITY;
      else if(val.compareToIgnoreCase("NAN") == 0)
         rtn = Double.NaN;
      else
         rtn = Double.parseDouble(val);
      return rtn;
   } // csi_string_to_float


   /**
    * Implements a "true division" algorithm where the following rules hold:
    *   q*d + r = n
    *   0 <= r < d
    *
    * @param n  specifies the numerator
    * @param d  specifies the denominator
    * @return the quotient and remainder as an array of long
    */
   public static final long[] truediv(
      long n,
      long d)
   {
      long[] rtn = new long[2];
      rtn[0] = n/d;
      rtn[1] = n%d;
      if(rtn[1] < 0)
      {
         rtn[0] += 1;
         rtn[1] += d;
      }
      return rtn;
   } // truediv
   
   
   public static void main(String[] args) throws Exception
   {
      byte[] test_sig1 = 
      { 
         (byte) 0xa0, 0x02, 0x5f, (byte) 0xf0, 0x10, 0x02, 0x0f, (byte) 0xf0, 0x09, (byte) 0xe8, (byte) 0xff, (byte) 0xff, 0x06, 0x00, 0x05,
         (byte) 0xf5, (byte) 0xc6, 0x00, 0x00, 0x66, 0x32, 0x00, 0x00, 0x75, 0x60, 0x00, 0x00
      };
      int signature = calc_sig(test_sig1, test_sig1.length);
      if(signature != 0x6300)
         throw new Exception("invalid signature calculated");
      int signature_null = calc_sig_nullifier(signature);
      if(signature_null != 0x9d00)
         throw new Exception("invalid signature nullifier");
   } // main
}
