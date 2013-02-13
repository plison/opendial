// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.DoubleVal;
import opendial.bn.values.Value;
import opendial.bn.values.VectorVal;
import opendial.inference.datastructs.WeightedSample;

public class MathUtils {

	// logger
	public static Logger log = new Logger("MathUtils", Logger.Level.DEBUG);

	public static final double MIN_PROXIMITY_DISTANCE = 0.1;


	/**
	 * Returns the closest row for the given value.  This method only works
	 * if the head values are defined as doubles.
	 * 
	 * @param head the head assignment
	 * @return the closest row, if any.  Else, an empty assignment
	 */
	public static Assignment getClosestElement(Set<Assignment> elements, Assignment head) {

		double minDistance = MIN_PROXIMITY_DISTANCE;
		Assignment closest = new Assignment();

		outer: 
			for (Assignment element : elements) {
			if (element.size() != head.size() || !element.getVariables().equals(head.getVariables())) {
				log.debug("searching for closest element on non-comparable assignments: " + head + " and " + element);
				continue outer;
			}
			double totalDistance = 0;
			for (String var : head.getVariables()) {
				Value headVal = head.getValue(var);
				Value elVal = element.getValue(var);
				if (headVal instanceof DoubleVal && elVal instanceof DoubleVal) {
					totalDistance += Math.abs(((DoubleVal)headVal).getDouble() - ((DoubleVal)elVal).getDouble()) ;
				}
				else if (headVal instanceof VectorVal && elVal instanceof VectorVal) {
					totalDistance += MathUtils.getDistance(((VectorVal)headVal).getArray(), ((VectorVal)elVal).getArray());
				}
				else if (!headVal.equals(elVal)) {
					continue outer;
				}
			}
			if (totalDistance < minDistance) {
				closest = element;
				minDistance = totalDistance;
			}
		}
		return closest;
	}
	
	
	public static Map<Assignment,Double> getClosestElements (List<Assignment> elements, Assignment head) {
		
		Map<Assignment,Double> values = new HashMap<Assignment,Double>();
		
		int incr = 0;
		Collections.shuffle(elements);
		
		double minDistance = MIN_PROXIMITY_DISTANCE*5.0;
		outer: 
			for (Assignment element : elements) {
			if (!element.getVariables().containsAll(head.getVariables())) {
				log.debug("searching for closest elements on non-comparable assignments: " + head + " and " + element);
				continue outer;
			}
			double totalDistance = 0;
			for (String var : head.getVariables()) {
				Value headVal = head.getValue(var);
				Value elVal = element.getValue(var);
				if (headVal instanceof DoubleVal && elVal instanceof DoubleVal) {
					totalDistance += Math.abs(((DoubleVal)headVal).getDouble() - ((DoubleVal)elVal).getDouble()) ;
				}
				else if (headVal instanceof VectorVal && elVal instanceof VectorVal) {
					totalDistance += MathUtils.getDistance(((VectorVal)headVal).getArray(), ((VectorVal)elVal).getArray());
				}
				else if (!headVal.equals(elVal)) {
					continue outer;
				}
			}
			if (totalDistance < minDistance) {
				incr++;
				Assignment value = element.getTrimmedInverse(head.getVariables());
				double weight = minDistance - totalDistance/2.0;
				if (values.containsKey(value)) {
					values.put(value, values.get(value) + weight);
				}
				else {
					values.put(value, weight);
				}
			}
		}
		
		if (values.isEmpty()) {
	//		log.warning("no closer element found for assignment " + head);
		}
		// log.debug("number of relevant elements: " + incr + " on a total of " + elements.size());
		return values;
	}
		
		

	public static double getMaxDistance(Collection<Double> points) {
		double maxDistance = Double.MAX_VALUE;
		Iterator<Double> it = points.iterator();
		if (it.hasNext()) {
			double prev = it.next();
			while (it.hasNext()) {
				double cur = it.next();
				double dist = Math.abs(cur-prev);
				if (dist > maxDistance) {
					maxDistance = dist;
				}
				prev = cur;
			}
		}
		return maxDistance;
	}
	

	public static double getMinDistance(Collection<Double> points) {
		double minDistance = Double.MAX_VALUE;
		Iterator<Double> it = points.iterator();
		if (it.hasNext()) {
			double prev = it.next();
			while (it.hasNext()) {
				double cur = it.next();
				double dist = Math.abs(cur-prev);
				if (dist < minDistance) {
					minDistance = dist;
				}
				prev = cur;
			}
		}
		return minDistance;
	}
	

	public static double getDistance(double[] point1, double[] point2) {
		double dist = 0;
		for (int i = 0 ; i < point1.length ; i++) {
			dist += Math.pow(point1[i]-point2[i], 2);
		}
		return Math.sqrt(dist);
	}
	
	public static double getMinManhattanDistance(Collection<double[]> points) {
		double minDistance = Double.MAX_VALUE;
		Iterator<double[]> it = points.iterator();
		if (it.hasNext()) {
			double[] prev = it.next();
			while (it.hasNext()) {
				double[] cur = it.next();
				double dist =getDistance(prev, cur);
				if (dist < minDistance) {
					minDistance = dist;
				}
				prev = cur;
			}
		}
		return minDistance;
	}
	
	

	public static double getAverageDistance(Collection<double[]> points) {
		Iterator<double[]> it = points.iterator();
		double dist = 0;
		if (it.hasNext()) {
			double[] prev = it.next();
			while (it.hasNext()) {
				double[] cur = it.next();
				dist += getDistance(prev, cur);
				prev = cur;
			}
		}
		return dist / points.size();
	}
	
	
	public static double getMaxManhattanDistance(Collection<double[]> points) {
		double maxDistance = Double.MAX_VALUE;
		Iterator<double[]> it = points.iterator();
		if (it.hasNext()) {
			double[] prev = it.next();
			while (it.hasNext()) {
				double[] cur = it.next();
				double dist =getDistance(prev, cur);
				if (dist > maxDistance) {
					maxDistance = dist;
				}
				prev = cur;
			}
		}
		return maxDistance;
	}
	
	
	
	public static double shorten(double value) {
		return Math.round(value*10000.0)/10000.0;
	}



}

