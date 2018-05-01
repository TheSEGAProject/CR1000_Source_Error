/* ValueName.java

   Copyright (C) 2006, 2007 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Saturday 14 October 2006
   Last Change: Wednesday 11 April 2007
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;

import java.util.*;


/**
 * Defines an object that is able to take a fully qualified value name and
 * break it into its component pieces, station name, table name, value name,
 * and array address.
 */
public class ValueName
{
   /**
    * Default constructor
    */
   public ValueName()
   {
      station_name = new String();
      table_name = new String();
      column_name = new String();
      array_address = new Vector<Integer>(3);
   } // default constructor
   
   
   /**
    * Construct from a value name
    * 
    * @param value_name  Specifies the fully qualified value name
    */
   public ValueName(String value_name) throws Exception
   {
      station_name = new String();
      table_name = new String();
      column_name = new String();
      array_address = new Vector<Integer>(3);
      parse(value_name);
   } // value_name based constructor
   
   
   private enum state_type
   {
      before_read_name,
      in_name,
      before_subscript,
      in_subscript,
      after_subscript,
      complete
   }
   
   
   /**
    * Responsible for parsing the fully qualified value name
    *
    * @param value_name  Specifies the fully qualified value name
    * 
    */
   public void parse(String value_name) throws Exception
   {
      StringBuilder temp = new StringBuilder();
      state_type state = state_type.before_read_name;
      int i = 0;
      boolean advance_i = true;
      
      station_name = "";
      table_name = "";
      column_name = "";
      array_address.clear();
      
      while(state != state_type.complete && i < value_name.length())
      {
         char ch = value_name.charAt(i);
         advance_i = true;
         if(state == state_type.before_read_name)
         {
            if(!Character.isSpaceChar(ch))
            {
               state = state_type.in_name;
               advance_i = false;
            }
         }
         else if(state == state_type.in_name)
         {
            switch(ch)
            {
            case '.':
               if(temp.length() == 0)
                  throw new Exception("value name syntax error");
               if(column_name.length() == 0)
                  column_name = temp.toString();
               else if(table_name.length() == 0)
               {
                  table_name = column_name;
                  column_name = temp.toString();
               }
               else if(station_name.length() == 0)
               {
                  station_name = table_name;
                  table_name = column_name;
                  column_name = temp.toString();
               }
               else
                  throw new Exception("invalid value name syntax");
               temp.delete(0,temp.length());
               break;

            case '(':
               state = state_type.before_subscript;
               break;

            default:
               temp.append(ch);
               break;
            }
         }
         else if(state == state_type.in_subscript ||
                 state == state_type.after_subscript ||
                 state == state_type.before_subscript)
         {
            switch(ch)
            {
            case '0':
            case '1':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
               if(state == state_type.before_subscript || state == state_type.in_subscript)
               {
                  state = state_type.in_subscript;
                  temp.append(ch);
               }
               else
                  throw new Exception("Invalid value name syntax");
               break;

            case ' ':
               if(state == state_type.in_subscript)
                  state = state_type.after_subscript;
               break;

            case ',':
               if(temp.length() > 0)
               {
                  array_address.add(
                     Integer.decode(temp.toString()));
               }
               else
                  throw new Exception("Invalid value name syntax");
               temp.delete(0,temp.length());
               break;

            case ')':
               if(temp.length() > 0)
               {
                  state = state_type.complete;
                  array_address.add(
                     Integer.decode(temp.toString())); 
               }
               else
                  throw new Exception("Invalid value name syntax");
               break;

            default:
               throw new Exception("Invalid value name syntax");
            }
         }
         if(advance_i)
            ++i;
      }
      if (temp.length() > 0) 
      {
         if (column_name.length() == 0)
            column_name = temp.toString();
         else if (table_name.length() == 0) 
         {
            table_name = column_name;
            column_name = temp.toString();
         } 
         else if (station_name.length() == 0) 
         {
            station_name = table_name;
            table_name = column_name;
            column_name = temp.toString();
         } 
         else
            throw new Exception("invalid value name syntax");
      }
   } // parse
   
   
   /**
    * @return The station name or an empty string if none was parsed
    */
   public String get_station_name()
   { return station_name; }
   
   
   /**
    * @return the table name or an empty string if none was parsed
    */
   public String get_table_name()
   { return table_name; }
   
   
   /**
    * @return the column name
    */
   public String get_column_name()
   { return column_name; }
   
   
   /**
    * @return the array address
    */
   public List<Integer> get_array_address()
   { return array_address; }
   
   
   /**
    * Stores the station name (empty if no station name was parsed)
    */
   private String station_name;


   /**
    * stores the table name (empty if no table name parsed)
    */
   private String table_name;


   /**
    * stores the column name
    */
   private String column_name;


   /**
    * Stores the array address
    */
   private List<Integer> array_address;
};

