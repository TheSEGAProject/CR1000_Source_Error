/* LoggerDate.java

   Copyright (C) 2006, 2008 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Saturday 14 October 2006
   Last Change: Monday 04 February 2008
   Last Commit: $Date: 2015/02/07 01:20:54 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.util.*;




/**
 * <p>Defines an object that represents a date as nano-seconds since Midnight 1
 * January 1990 without regard to any specific time zone.  This object is used
 * in dealing with dates for datalogger related protocols because of its
 * nano-second resolution (the Java Calendar and Date classes deal with, at
 * best, milli-second resolution).  Despite this, this class will provide
 * methods for converting to and from Java standard Calendar objects and will
 * optionally allow the application to specify the time zone when this
 * conversion is made.</p>
 *
 * <p>This class uses concepts described in the following articles:</p>
 * <ul>
 *    <li>Dr Dobbs Journal #80, June 1983.   True Julian dates as used by
 * astronomers take noon, J January 4713 BC as their base.
 *
 *    <li>Collected Algorithms from CACM - Algorithm 199
 *      <ul>
 *        <li>1 March 1900 = Julian Day 2415080 (noon based)
 *        <li>1 January 0000 = Julian Day 1721119
 *        <li>1 January 1970 = Julian Day 2440588
 *        <li>1 January 1980 = Julian day 2444240
 *        <li>1 January 1970 fell on a Thursday
 *        <li>The difference between 1 January 1990 and 1 January 1970 is 631,152,000
 *        seconds or 7,305 days
 *      </ul>
 * </ul>    
 */
public class LoggerDate implements Cloneable, Comparable<LoggerDate>
{
   /**
    * Default constructor sets the date to midnight 1 January 1990
    */
   public LoggerDate()
   { elapsed = 0; }


   /**
    * Construct from raw interval
    *
    * @param elapsed_  Specifies the interval since midnight 1 January 1990 in
    * nano-seconds
    */
   public LoggerDate(long elapsed_)
   { elapsed = elapsed_; }


   // the following conversion factors will be used throughout this
   // implementation and are useful  to the application as well.
   public static final long nsec_per_usec = 1000;
   public static final long nsec_per_msec = nsec_per_usec * 1000;
   public static final long nsec_per_sec = nsec_per_msec * 1000;
   public static final long nsec_per_min = nsec_per_sec * 60;
   public static final long nsec_per_hour = nsec_per_min * 60;
   public static final long nsec_per_day = nsec_per_hour * 24;
   public static final long nsec_per_week = nsec_per_day * 7;
   public static final long msec_per_sec = 1000;
   public static final long msec_per_min = msec_per_sec * 60;
   public static final long msec_per_hour = msec_per_min * 60;
   public static final long msec_per_day = msec_per_hour * 24;
   public static final long msec_per_week = msec_per_day * 7;
   public static final long jul_day_0 = 1721119;
   public static final long jul_day_1970 = 2440588;
   public static final long days_since_1970 = 7305;
   public static final long jul_day_1990 = jul_day_1970 + days_since_1970;
   public static final long days_since_30_dec_1899 = 32874;

   
   /**
    * Construct from a Calendar object
    *
    * @param calendar   Specifies the calendar object
    */
   public LoggerDate(Calendar calendar)
   { from_calendar(calendar); }


   /**
    * Construct from seconds + nanosecs since 1990
    *
    * @param seconds  specifies seconds since midnight 1 jan 1990
    * @param nsec     specifies nano-seconds into the seconds
    */
   public LoggerDate(long seconds, long nsec)
   { elapsed = seconds * nsec_per_sec + nsec; }
   
   
   /**
    * Construct a LoggerDate from another
    * 
    * @param other   the stamp to use for initialisation
    */
   public LoggerDate(LoggerDate other)
   { elapsed = other.elapsed; }
   

   /**
    * Construct a LoggerDate object from a Date object
    *
    * @param date  The date object that specifies the date and time
    */
   public LoggerDate(Date date)
   {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      from_calendar(cal);
   }

   
   /**
    * Generates a LoggerDate class set to the current system time
    * 
    * @return the new date
    */
   public static final LoggerDate system()
   { return new LoggerDate(Calendar.getInstance()); }
   
   /**
    * Generates a LoggerDate object set to the current gmt 
    * 
    * @return the new date
    */
   public static final LoggerDate gmt()
   { 
      LoggerDate rtn = new LoggerDate(
         Calendar.getInstance(
            TimeZone.getTimeZone("GMT")));
      return  rtn;
   }

   /**
    * Creates a calendar object from the date assuming a system time zone.
    *
    * @return A gregorian calendar object that represents this date
    */
   public GregorianCalendar to_calendar()
   {
      // create the return object and get its time zone
      GregorianCalendar rtn = new GregorianCalendar();
      TimeZone time_zone = rtn.getTimeZone();
      
      // we now need to adapt our representation to that used by the calendar. 
      // we also need to add the adjustment for the time zone 
      long nsec_since_1970 = elapsed + (days_since_1970 * nsec_per_day);
      long msec_since_1970 = nsec_since_1970 / nsec_per_msec;
      rtn.setTimeInMillis(msec_since_1970 - time_zone.getOffset(msec_since_1970));
      return rtn;
   } // to_calendar


   /**
    * Sets this time from a calendar object
    *
    * @param calendar  The calendar
    */
   public void from_calendar(Calendar calendar)
   {
      // we need to get the raw time from the calendar and add the correction from its time zone
      long msec_since_1970 = calendar.getTimeInMillis();
      TimeZone time_zone = calendar.getTimeZone();
      
      msec_since_1970 += time_zone.getOffset(msec_since_1970);

      // we can now change the epoch and convert the units to nano-seconds
      long msec_since_1990 = msec_since_1970 - (days_since_1970 * msec_per_day);
      elapsed = msec_since_1990 * nsec_per_msec;
   } // from_calendar


   /**
    * Separates the year, month, and day from the time and writes these values
    * to the appropriate parameters.
    *
    * @return the year, month, and day components as an array of long
    */
   public long[] to_date()
   {
      // we need both the days elapsed as well as the positive remainder in nano-seconds
      long year, month, day;
      long days_since_1990 = Utils.truediv(elapsed, nsec_per_day)[0];
      
      // we can now apply julian day calculations
      long clock = jul_day_1990 - jul_day_0 + days_since_1990;
      year = (4 * clock - 1)/146097L;
      clock = 4*clock - 1 - year*146097;

      // we can now strip off the month and day
      long d = clock/4;
      clock = (4*d + 3)/1461;
      d = 4*d + 3 - clock/1461;
      d = (d + 4)/4;
      month = (5*d - 3)/153;
      d = 5*d - 3 - month*153;
      day = (d + 5)/5;
      year = 100*year + clock;
      if(month < 10)
         month += 3;
      else
      {
         month -= 9;
         year++;
      } 
      
      // set up the return values
      long[] rtn = new long[3];
      rtn[0] = year;
      rtn[1] = month;
      rtn[2] = day;
      return rtn;
   } // to_date


   /**
    * Separates out the time elements of the time stamp
    *
    * @return the hours, minutes, seconds, and nsec values in an array of longs
    */
   public long[] to_time()
   {
      long[] temp = Utils.truediv(elapsed, nsec_per_sec);
      long[] rtn = new long[4];
      
      rtn[3] = rtn[1];
      temp = Utils.truediv(temp[0], 60);
      rtn[2] = temp[1];
      temp = Utils.truediv(temp[0], 60);
      rtn[1] = temp[1];
      temp = Utils.truediv(temp[0], 24);
      rtn[0] = temp[1];
      return rtn;
   } // to_time


   /**
    * @return the year
    */
   public long get_year()
   {
      long[] components = to_date();
      return components[0];
   } // get_year
     

   /**
    * @return the month
    */
   public long get_month()
   {
      long[] components = to_date();
      return components[1];
   } // get_month


   /**
    * @return the day
    */
   public long get_day()
   {
      long[] components = to_date();
      return components[2];
   } // get_day


   /**
    * @return the hour
    */
   public long get_hour()
   { return to_time()[0]; }


   /**
    * @return the minute
    */
   public long get_minute()
   { return to_time()[1]; }


   /**
    * @return the second
    */
   public long get_second()
   { return to_time()[2]; }


   /**
    * @return the nanoseconds into the second
    */
   public long get_nsec()
   { return to_time()[3]; }


   /**
    * @return day of week (sunday = 1)
    */
   public long get_day_of_week()
   {
      long days = elapsed/nsec_per_day + jul_day_1990 - jul_day_0;
      return ((days + 2) % 7) + 1;
   } // get_day_of_week


   /**
    * @return the day of the year (1 jan = 1)
    */
   public long day_of_year()
   {
      LoggerDate year_start = new LoggerDate();
      year_start.set_date(get_year(),1L,1L);
      return ((elapsed - year_start.elapsed) / nsec_per_day) + 1;
   } // day_of_year


   /**
    * @return the number of seconds elapsed since midnight 1 jan 1990
    */
   public long get_secs_since_1990()
   { return elapsed / nsec_per_sec; }

   
   /**
    * Sets the date information in this time stamp but leaves the time alone
    * 
    * @param year  specifies the year
    * @param month specifies the month of the year (january == 1)
    * @param day   specifies the day into the month (starts at 1)
    */
   public void set_date(
      long year,
      long month,
      long day)
   {
      // normalise the time info
      if(year < 1)
         year = 1;
      if(month < 1)
         month = 1;
      if(day < 1)
         day = 1;
      
      // preserve the time
      long[] time = to_time();
      
      // we need to separate the century and year 
      long century, year_of_century;
      long jm, jy;
      
      if(month >= 3)
      {
         jm = month - 3;
         jy = year;
      }
      else
      {
         jm = month + 9;
         jy = year - 1;
      }
      century = jy / 100;
      year_of_century = jy % 100;
      
      // calculate the number of days since 1 jan 0000
      long days = 
         ((146097 * century) / 4) + 
         ((1461 * year_of_century) / 4) +
         ((153 * jm + 2) / 5) +
         day;
      
      // we can now bring everything back together
      days -= jul_day_1990 - jul_day_0;
      elapsed = 
         days * nsec_per_day +
         time[0] * nsec_per_hour +
         time[1] * nsec_per_min +
         time[2] * nsec_per_sec +
         time[3];
   } // set_date
   
   
   /**
    * Sets the time component without touching the date.
    * 
    * @param hour  Specifies hours into the day (0 - 23)      
    * @param minute Specifies minutes into the hour (0 - 59)
    * @param second Specifies seconds into the minute (0 - 59)
    * @param nsec   Specifies nsec in the second (0 - 1000000000)
    */
   public void set_time(
      long hour,
      long minute,
      long second,
      long nsec)
   {
      // we first strip off the current time
      elapsed -= Utils.truediv(elapsed, nsec_per_day)[1];
      
      // we now add the components above
      elapsed +=
         hour * nsec_per_hour +
         minute * nsec_per_min +
         second * nsec_per_sec +
         nsec;
   } // set_time
   
   
   /**
    * Formats the date using an strftime() like format string.  The term,
    * "like" is used here because some codes are different.  The following
    * codes will be recognised:
    *
    * <table>
    *   <tr>
    *     <td><tt>%a</tt></td> 
    *     <td>abbreviated weekday name according to locale</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%A</tt></td> 
    *     <td>full weekday name according to locale</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%b</tt></td>
    *     <td>abbreviated month name according to locale</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%B</tt></td>
    *     <td>full month name according to locale</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%c</tt></td>
    *     <td>local date and time representation  (short version)</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%#c</tt></td>
    *     <td>local date and time representation (long version)</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%d</tt></td>
    *     <td>day of month, two spaces, rights justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%H</tt></td>
    *     <td>hours into the day, two spaces right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%I</tt></td>
    *     <td>Hour with 12 hour clock, two spaces right justified, padded with zero</td>
    *   <tr>
    *   <tr>
    *     <td><tt>%j</tt></td>
    *     <td>Day of year, three spaces right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%m</tt></td>
    *     <td>numeric month, two spaces right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%M</tt></td>
    *     <td>minutes into the hour, two spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%p</tt></td>
    *     <td>local equivalent of "AM" or "PM" specifier</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%S</tt></td>
    *     <td>seconds into the minute, two spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%w</tt></td>
    *     <td>day of week as an integer, one space</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%y</tt></td>
    *     <td>years into century, two spaces, rights justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%Y</tt></td>
    *     <td>year as an integer</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%1</tt></td>
    *     <td>tenths of seconds, one space</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%2</tt></td>
    *     <td>hundredths of seconds, two spaces, rights justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%3</tt></td>
    *     <td>thousands of seconds, three spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%4</tt></td>
    *     <td>1/10000 of second, four spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%5</tt></td>
    *     <td>1/100000 of second, five spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%6</tt></td>
    *     <td>micro-seconds, six spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%7</tt></td>
    *     <td>1/10000000 of second, seven spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%8</tt></td>
    *     <td>1/100000000 of seconds, eight spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%9</tt></td>
    *     <td>nano-seconds, nine spaces, right justified, padded with zero</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%x</tt></td>
    *     <td>prints the sub-second resolution of the stamp with a preceding period with no
    * padding</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%X</tt></td>
    *     <td>local date representation</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%n</tt></td>
    *     <td>local time representation (%x conflicts with previous usage)</td>
    *   </tr>
    *   <tr>
    *     <td><tt>%%</tt></td>
    *     <td>Prints the '%' character</td>
    *   </tr>
    * </table>
    */
   public String format(String spec)
   {
      StringBuilder rtn = new StringBuilder();
      char last_ch = 0;
      Calendar calendar = to_calendar();
      int exp, divisor;
      long[] time = to_time();
      
      for(int i = 0; i < spec.length(); ++i)
      {
         if(last_ch == '%' && spec.charAt(i) == '%')
            rtn.append('%');
         else if(last_ch == '%')
         {
            boolean flagged = spec.charAt(i) == '#';
            if(flagged && i + 1 < spec.length())
               ++i;
            switch(spec.charAt(i))
            {
            case 'a':
            case 'A':
            case 'b':
            case 'B':
            case 'c':
            case 'd':
            case 'H':
            case 'I':
            case 'j':
            case 'm':
            case 'M':
            case 'n':
            case 'p':
            case 'S':
            case 'U':
            case 'w':
            case 'X':
            case 'y':
            case 'Y':
            case 'Z':
            {
               StringBuilder format_buff = new StringBuilder();
               format_buff.append("%1$t");
               if(flagged)
                  format_buff.append('#');
               format_buff.append(spec.charAt(i) == 'n' ? 'z' : spec.charAt(i));
               rtn.append(
                  String.format(format_buff.toString(), calendar));
               break;
            }
            
            case '1':              // tenths of seconds
            case '2':              // hundredths of seconds
            case '3':              // thousandths of seconds
            case '4':              // 1/10000 of seconds
            case '5':              // 1/100000 of seconds
            case '6':              // micro-seconds
            case '7':              // 1/10000000 of seconds
            case '8':              // 1/100000000 of seconds
            case '9':              // nano-seconds
               exp = 9 - (spec.charAt(i) - '0');
               divisor = 1;
               for(int j = 0; j< exp; ++j)
                  divisor *= 10;
               rtn.append(
                  String.format(
                     "%1$02d",
                     time[3] / divisor));
               break;

            case 'x':
               if(time[3] > 0)
               {
                  StringBuilder temp = new StringBuilder();
                  temp.append(
                     String.format(
                        ".%1$09u",
                        time[3]));
                  for(int digit = 8; digit >= 0 && rtn.charAt(digit) == '0'; --digit)
                     temp.deleteCharAt(digit);
                  rtn.append(temp);
               }
               break;

            case '%':
               rtn.append('%');
               break;
            }
         }
         else if(spec.charAt(i) != '%')
            rtn.append(spec.charAt(i));
         last_ch = spec.charAt(i); 
      }
      return rtn.toString();
   } // format

   
   @Override
   public Object clone()
   { return new LoggerDate(elapsed); }
     
   
   /**
    * Overrides Object.hashCode so that seconds since 1990 is used
    */
   @Override
   public int hashCode()
   { return (int)(elapsed / nsec_per_sec); }


   @Override
   public String toString()
   { return format("%c"); }
      
   
   /**
    * Overrides the base version to compare the time elapsed.
    *
    * @param obj  assumed to be another LoggerDate object
    */
   @Override
   public boolean equals(Object obj)
   {
      LoggerDate other = (LoggerDate)obj;
      return elapsed == other.elapsed;
   } // equals


   /**
    * Implements the Comparable Interface method
    *
    * @param o another instance of LoggerDate
    */ 
   public int compareTo(LoggerDate o)
   {
      int rtn;
      if(elapsed > o.elapsed)
         rtn = 1;
      else if(elapsed < o.elapsed)
         rtn = -1;
      else
         rtn = 0;
      return rtn;
   } // compareTo
   
   
   /**
    * Adds the specified number of nano-seconds to the current time
    * 
    * @param nsec  Specifies the number of nano-seconds to add
    */
   public void add_nsec(long nsec)
   { elapsed += nsec; }

   
   /**
    * @return The nano-seconds elapsed since midnight 1 January 1990
    */
   public long get_elapsed()
   { return elapsed; }
   
   
   /**
    * Holds the time elapsed since midnight 1 Jan 1990 in nano-seconds
    */
   private long elapsed;
}
