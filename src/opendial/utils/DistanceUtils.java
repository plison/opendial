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

package opendial.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ArrayVal;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;

/**
 * Utilities to calculate distance (between doubles and arrays).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class DistanceUtils {

	// logger
	public static Logger log = new Logger("DistanceUtils", Logger.Level.DEBUG);

	public static final double MIN_PROXIMITY_DISTANCE = 0.1;


	/**
	 * Returns true is all elements in the array a have a lower value than
	 * the corresponding elements in the array b
	 * 
	 * @param a the first array
	 * @param b the second array
	 * @return true is a is lower than b in all dimensions, and false otherwise
	 */
	public static boolean isLower(Double[] a, Double[] b) {
		for (int i = 0 ; i < a.length  ; i++) {
			if (a[i] > b[i]) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Find the assignment whose values are closest to the assignment toFind, assuming the values
	 * contained in the assignment are composed of only DoubleVal or ArrayVal.
	 * 
	 * @param rows the assignments in which to search for the closest element
	 * @param toFind the reference assignment
	 * @return the closest assignment, if any is found.
	 * @throws DialException if no closest assignment could be found
	 */
	public static Assignment getClosestElement(Collection<Assignment> rows, 
			Assignment toFind) throws DialException {
			
		Assignment closest = null;
		double minDistance = Double.MAX_VALUE;
		for (Assignment a : rows) {
			double totalDistance = 0;
			for (String var : toFind.getVariables()) {
				Double[] val1 = convertToDouble(toFind.getValue(var));
				Double[] val2 = convertToDouble(a.getValue(var));
				totalDistance += getDistance(val1, val2);
			}	
			if (totalDistance < minDistance) {
				minDistance = totalDistance;
				closest = a;
			}
		}	
		if (closest == null) {
		throw new DialException("could not find closest element");
		}
		return closest;
	}
	

	/**
	 * Returns the Euclidian distance between two double arrays.
	 * 
	 * @param point1 the first double array
	 * @param point2 the second double array
	 * @return the distance beteeen the two points
	 */
	public static double getDistance(Double[] point1, Double[] point2) {
		if (point1 == null || point2 == null || point1.length != point2.length) {
			return Double.MAX_VALUE;
		}
		double dist = 0;
		for (int i = 0 ; i < point1.length ; i++) {
			dist += Math.pow(point1[i]-point2[i], 2);
		}
		return Math.sqrt(dist);
	}
	
	
	/**
	 * Returns the minimal Euclidian distance between any two pairs of points
	 * in the collection of points provided as argument.
	 * 
	 * @param points the collection of points
	 * @return the minimum distance between all possible pairs of points
	 */
	public static double getMinEuclidianDistance(Collection<Double[]> points) {
		double minDistance = Double.MAX_VALUE;
		List<Double[]> l = new ArrayList<Double[]>(points);
		for (int i = 0 ; i < points.size()-1 ; i++) {
			Double[] first = l.get(i);
			for (int j = i+1 ; j < points.size() ; j++) {
				Double[] second = l.get(j);
				double dist = getDistance(first, second);
				if (dist < minDistance) {
					minDistance = dist;
				}
			}
		}
		return minDistance;
	}
	


	/**
	 * Converts the value to a double array, if possible.  Else, returns null
	 * 
	 * @param val the value to convert
	 * @return the converted value or null.
	 */
	private static Double[] convertToDouble(Value val) {
		Double[] a = null;
		if (val instanceof ArrayVal) {
			a = ((ArrayVal)val).getArray();
		}
		else if (val instanceof DoubleVal) {
			a = new Double[]{((DoubleVal)val).getDouble()};
		}
		return a;
	}

	
	

}


	
