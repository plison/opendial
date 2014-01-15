// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.bn.values;


import java.util.Collection;
import java.util.Vector;

import opendial.arch.Logger;
import opendial.utils.StringUtils;

/**
 * Representation of an array of doubles.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class ArrayVal implements Value {

	// logger
	public static Logger log = new Logger("DoubleVectorVal",
			Logger.Level.DEBUG);
	
	// the array of doubles
	Double[] array;
	
	/**
	 * Creates a new array of doubles
	 * 
	 * @param values the array
	 */
	public ArrayVal(Double[] values) {
		this.array = new Double[values.length];
		for (int i = 0 ; i < array.length ; i++) {
			array[i] = values[i];
		}
	}

	/**
	 * Creates a new array of doubles
	 * 
	 * @param values the array (as a collection)
	 */
	public ArrayVal(Collection<Double> values) {
		array = values.toArray(new Double[values.size()]);
	}

	
	/**
	 * Compares to another value.  
	 */
	@Override
	public int compareTo(Value arg0) {
		if (arg0 instanceof ArrayVal) {
			Double[] otherVector = ((ArrayVal)arg0).getArray();
			if (array.length != otherVector.length) {
				return array.length - otherVector.length;
			}
			else {
				for (int i = 0 ; i < array.length ; i++) {
					double val1 = array[i];
					double val2 = otherVector[i];
					
					// if the difference is very small, assume 0
					if (Math.abs(val1 - val2) > 0.0001) {
						return (new Double(val1).compareTo(new Double(val2)));
					}
				}
				return 0;
			}
		}
		return hashCode() - arg0.hashCode();
	}

	
	/**
	 * Copies the array
	 */
	@Override
	public Value copy() {
		return new ArrayVal(array);
	}
	
	
	/**
	 * Returns a vector with the elements of the array
	 * 
	 * @return the vector
	 */
	public Vector<Double> getVector() {
		Vector<Double> vector = new Vector<Double>(array.length);
		for (int i = 0 ; i < array.length ; i++) {
			vector.add(array[i]);
		}
		return vector;
	}
	
	/**
	 * Checks for equality
	 */
	public boolean equals(Object o) {
		if (o instanceof ArrayVal) {
			return ((ArrayVal)o).getVector().equals(getVector());
		}
		return false;
	}
	
	/**
	 * Returns the array
	 * 
	 * @return the array
	 */
	public Double[] getArray() {
		return array;
	}
	
	/**
	 * Returns the hashcode for the array
	 */
	public int hashCode() {
		return 2* getVector().hashCode();
	}
	
	
	/**
	 * Returns a string representation of the array
	 */
	@Override
	public String toString() {
		String s = "[";
		for (Double d : getVector()) {
			s += StringUtils.getShortForm(d) + ",";
		}
		return s.substring(0, s.length() -1) + "]";
	}

	
	/**
	 * Returns false.
	 */
	@Override
	public boolean contains(Value filledValue) {
		return false;
	}

}

