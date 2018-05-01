/* ArrayDimensions.java

   Copyright (C) 2006, 2006 Campbell Scientific, Inc.

   Written by: Jon Trauntvein
   Date Begun: Tuesday 10 October 2006
   Last Change: Wednesday 11 October 2006
   Last Commit: $Date: 2015/02/07 01:20:55 $
   Last Changed by: $Author: jdk85 $

*/

package com.campbellsci.pakbus;


import java.util.*;


/**
 * Represents the dimensions of an array as a vector of positive integers.
 * This is used to help deal with array dimensions associated with datalogger
 * values.
 */
public class ArrayDimensions
{
   /**
    * Default constructor
    */
   public ArrayDimensions()
   {
      dims = new Vector<Integer>();
   } // constructor


   /**
    * Copy constructor (from general dims container)
    *
    * @param dims_  The collection of dimensions 
    */
   public ArrayDimensions(List<Integer> dims_)
   {
      dims = new Vector<Integer>(dims_);
   } // copy constructor


   /**
    * Calculates the linear offset of an array element referred to by the index
    * parameter.
    *
    * @param index_  A collection of integers that specify the array index in
    * row major order.  The most significant index should be given first and
    * the number of values in the index should be equal to the number of
    * dimensions in the array.  The value of each component in the index must
    * also be less than or equal to the corresponding array dimension.
    * @param ignore_least_significant  Specifies that the index was generated
    * so that the least significant dimension was omitted.  This is used to add 
    * a dimension to the local index used for the calculation.
    * @return The linear offset into the array assuming row major ordering and
    * one based index.  
    */
   public int to_offset(
         List<Integer> index_,
         boolean ignore_least_significant)
   {
      int rtn = 0;
      Vector<Integer> index = new Vector<Integer>(index_);
      
      if(ignore_least_significant)
         index.add(1);
      if(!index.isEmpty() && index.size() == dims.size())
      {
         int weight = 1;
         for(int i = dims.size(); i > 0; --i)
         {
            int dim = dims.get(i - 1);
            int subscript = index.get(i - 1);
            rtn += (subscript - 1) * weight;
            weight *= dim;
         }
      }
      return rtn + 1;
   } // to_offset
   
   
   /**
    * Creates an array of subscripts for the specified linear index.  This 
    * method is the inverse of to_offset().
    * 
    * @param offset  Specifies the linear offset into the array (assumes one 
    * based, row major ordering).  This value must be less than the total size 
    * of the array.
    * @param ignore_least_significant  Specifies that the least significant
    * dimension is to be left out of the index.  This is needed for the ascii
    * data type because the least significant dimension specifies the string
    * length. 
    * @return a vector of subscripts
    */
   public List<Integer> to_index(
      int offset,
      boolean ignore_least_significant)
   {
      List<Integer> rtn = new Vector<Integer>(dims.size());
      int weight = 1;
      for(int i = dims.size(); i > 0; --i)
      {
         int dim = dims.get(i - 1);
         int subscript = ((offset - 1) / weight) % dim + 1;
         rtn.add(subscript);
         weight *= dim;
      }
      Collections.reverse(rtn);
      if(ignore_least_significant)
         rtn.remove(rtn.size() - 1);
      return rtn;
   } // to_index
   
   
   /**
    * Calculates the maximum number of elements that can be stored in the 
    * array based upon its dimensions.
    * 
    * @return The maximum number of elements
    */
   public int array_size()
   {
      int rtn = 1;
      for(Integer dim: dims)
         rtn *= dim;
      return rtn;
   } // array_size
   
   
   /**
    * Adds a dimension to the array.  This method is generally used while 
    * reading table definitions from the datalogger.
    * 
    *  @param dim the size of the dimension to add
    */
   public void add_dimension(int dim)
   { dims.add(dim); }
   
   
   /**
    * Evaluates whether the dimensions are describing a "scalar" value. 
    * 
    * @param ignore_least_significant  Specifies that the least significant 
    * dimension should be ignored.  This is needed because, with the ascii data 
    * type, the least significant dimension is a measure of string length.
    * @return true if these dimensions indicate a scalar value
    */
   public boolean for_scalar(boolean ignore_least_significant)
   {
      boolean rtn = false;
      int dims_count = dims.size();
      if(ignore_least_significant)
         --dims_count;
      if(dims_count == 0 || dims.get(dims_count - 1) == 1)
         rtn = true;
      return rtn;
   } // for_scalar
   
   
   /**
    * @return the array dimensions
    */
   public List<Integer> values()
   { return dims; }
   
   
   /** 
    * @return the least significant dimension
    */
   public int back()
   { return dims.get(dims.size() - 1); }
   
   
   /**
    * @return the most significant dimension
    */
   public int front()
   { return dims.get(0); }
   
   
   /**
    * Holds the array dimensions
    */
   private Vector<Integer> dims;
};

