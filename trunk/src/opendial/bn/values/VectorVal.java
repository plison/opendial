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

package opendial.bn.values;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import opendial.arch.Logger;
import opendial.utils.DistanceUtils;

public class VectorVal implements Value {

	// logger
	public static Logger log = new Logger("DoubleVectorVal",
			Logger.Level.DEBUG);
	
	Double[] array;
	
	public VectorVal(Double[] values) {
		this.array = new Double[values.length];
		for (int i = 0 ; i < array.length ; i++) {
			array[i] = values[i];
		}
	}
	
	public VectorVal(Collection<Double> values) {
		array = new Double[values.size()];
		int incr = 0;
		for (Double value : values) {
			array[incr] = value.doubleValue();
			incr++;
		}
	}

	@Override
	public int compareTo(Value arg0) {
		if (arg0 instanceof VectorVal) {
			Double[] otherVector = ((VectorVal)arg0).getArray();
			if (array.length != otherVector.length) {
				return array.length - otherVector.length;
			}
			else {
				for (int i = 0 ; i < array.length ; i++) {
					double val1 = array[i];
					double val2 = otherVector[i];
					if (Math.abs(val1 - val2) > 0.0001) {
						return (new Double(val1).compareTo(new Double(val2)));
					}
				}
				return 0;
			}
		}
		return hashCode() - arg0.hashCode();
	}

	@Override
	public Value copy() {
		return new VectorVal(array);
	}
	
	public Vector<Double> getVector() {
		Vector<Double> vector = new Vector<Double>(array.length);
		for (int i = 0 ; i < array.length ; i++) {
			vector.add(array[i]);
		}
		return vector;
	}
	
	public boolean equals(Object o) {
		if (o instanceof VectorVal) {
			return ((VectorVal)o).getVector().equals(getVector());
		}
		return false;
	}
	
	public Double[] getArray() {
		return array;
	}
	
	public int hashCode() {
		return 2* getVector().hashCode();
	}
	
	
	@Override
	public String toString() {
		String s = "(";
		for (Double d : getVector()) {
			s += DistanceUtils.shorten(d) + ",";
		}
		return s.substring(0, s.length() -1) + ")";
	}

}

