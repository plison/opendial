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

import opendial.utils.MathUtils;


/**
 * Representation of a double value
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */


public final class DoubleVal implements Value {

	// the double
	double d;

	/**
	 * Creates the double value
	 * (protected, use the ValueFactory instead)
	 * 
	 * @param d the double
	 */
	protected DoubleVal(double d) { this.d = d; };

	/**
	 * Returns the hashcode for the double
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return new Double(d).hashCode(); }

	/**
	 * Returns true if the objects are similar, false otherwise
	 *
	 * @param o the object to compare
	 * @return true if similar, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		boolean result = (o instanceof DoubleVal && 
				Math.abs(((DoubleVal)o).getDouble() - getDouble()) < 0.000001);
		return result;
	}

	/**
	 * Returns the double value
	 * 
	 * @return
	 */
	public Double getDouble() {return d; }

	/**
	 * Returns a copy of the double value
	 *
	 * @return the copy
	 */
	@Override
	public DoubleVal copy() { return new DoubleVal(d); }

	/**
	 * Returns a string representation of the double
	 *
	 * @return
	 */
	@Override
	public String toString() { 
		return "" + MathUtils.shorten(d);
	}


	/**
	 * Compares the double value to another value
	 * 
	 * @return usual ordering, or hashcode difference if the value is not a double
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof DoubleVal) {
			return (new Double(d)).compareTo(((DoubleVal)o).getDouble());
		}
		else {
			return hashCode() - o.hashCode();
		}
	}
	
}