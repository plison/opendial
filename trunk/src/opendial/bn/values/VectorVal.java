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
	
	double[] array;
	Vector<Double> values;
	
	public VectorVal(double[] values) {
		this.array = values;
		this.values = new Vector<Double>(values.length);
		for (int i = 0 ; i < values.length ; i++) {
			this.values.add(values[i]);
		}
	}
	
	public VectorVal(Collection<Double> values) {
		this.values = new Vector<Double>(values);
		array = new double[values.size()];
		for (int i = 0 ; i < array.length ; i++) {
			array[i] = this.values.get(i);
		}
	}

	@Override
	public int compareTo(Value arg0) {
		if (arg0 instanceof VectorVal) {
			Vector<Double> otherVector = ((VectorVal)arg0).getVector();
			if (values.size() != otherVector.size()) {
				return values.size() - otherVector.size();
			}
			else {
				for (int i = 0 ; i < values.size() ; i++) {
					double val1 = values.get(i);
					double val2 = otherVector.get(i);
					if (val1 != val2) {
						return (new Double(val1).compareTo(new Double(val2)));
					}
				}
			}
		}
		return hashCode() - arg0.hashCode();
	}

	@Override
	public Value copy() {
		return new VectorVal(values);
	}
	
	public Vector<Double>  getVector() {
		return values;
	}
	
	public boolean equals(Object o) {
		if (o instanceof VectorVal) {
			return ((VectorVal)o).getVector().equals(values);
		}
		return false;
	}
	
	public double[] getArray() {
		return array;
	}
	
	public int hashCode() {
		return values.hashCode();
	}
	
	@Override
	public String toString() {
		String s = "(";
		for (Double d : values) {
			s += DistanceUtils.shorten(d) + ",";
		}
		return s.substring(0, s.length() -1) + ")";
	}

}

