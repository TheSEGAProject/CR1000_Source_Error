/* HopMetric.java

   Copyright (C) 2006, Campbell Scientific, Inc

   Written by: Jon Trauntvein
   Date Begun: Tuesday 03 October 2006
   Last Change: Tuesday 03 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


/**
 * Represents a value that can be converted between a response time (in
 * milli-seconds) to an encoded hop metric.  Provides methods that run the
 * encoding both ways.
 */
final class HopMetric
{
   /**
    * Constructor from coded value
    *
    * @param coded_value_  Specifies the coded time value.
    */
   public HopMetric(byte coded_value_)
   { coded_value = coded_value_; }


   /**
    * Constructor from time interval in milli-seconds
    *
    * @param time_msec  Specifies the time interval in milli-seconds.
    */
   public HopMetric(int time_msec)
   { set_response_time_msec(time_msec); }


   /**
    * @return The coded value
    */
   byte get_coded_value()
   { return coded_value; }


   /**
    * @return The response interval in milli-seconds
    */
   int get_response_time_msec()
   {
      int rtn;
      switch(coded_value)
      {
      case 0:
         rtn = 200;
         break;
         
      case 1:
         rtn = 1000;
         break;
         
      case 2:
         rtn = 5000;
         break;
         
      case 3:
         rtn = 10000;
         break;

      case 4:
         rtn = 20000;
         break;
         
      case 5:
         rtn = 60000;
         break;
         
      case 6:
         rtn = 300000;
         break;
         
      default:
         rtn = 1800000;
         break;
      }
      return rtn;
   } // get_response_time_msec


   /**
    * Sets the response time in milli-seconds.
    *
    * @param response_time_msec  Specifies the response time in milli-seconds.
    */
   void set_response_time_msec(int response_time_msec)
   {
      if(response_time_msec <= 200)
         coded_value = 0;
      else if(response_time_msec <= 1000)
         coded_value = 1;
      else if(response_time_msec <= 5000)
         coded_value = 2;
      else if(response_time_msec <= 10000)
         coded_value = 3;
      else if(response_time_msec <= 20000)
         coded_value = 4;
      else if(response_time_msec <= 60000)
         coded_value = 5;
      else if(response_time_msec <= 300000)
         coded_value = 6;
      else
         coded_value = 7;
   } // set_response_time_msec
   

   /**
    * Holds the encoded interval
    */
   byte coded_value;
};

