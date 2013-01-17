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

package opendial.bn.distribs.datastructs;



/**
 * Estimate of a (typically utility) value, defined by the averaged estimate itself,
 * and the number of values that have contributed to it (in order to 
 * correctly compute the average)
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Estimate {

	// averaged estimate for the utility
	double averagedEstimate = 0.0;

	// number of values used for the average
	int nbValues = 0;

	/**
	 * Creates a new utility estimate, with a first value
	 * 
	 * @param firstValue the first value
	 */
	public Estimate(double firstValue) {
		updateEstimate(firstValue);
	}

	
	/**
	 * Updates the current estimate with a new value
	 * 
	 * @param newValue the new value
	 */
	public void updateEstimate(double newValue) {
		double prevUtil = averagedEstimate;
		nbValues++;
		averagedEstimate = prevUtil + (newValue - prevUtil) / (nbValues);
	}


	/**
	 * Returns the current (averaged) estimate for the utility
	 * 
	 * @return the estimate
	 */
	public double getValue() {
		return averagedEstimate;
	}


	public int getNbCounts() {
		return nbValues;
	}
	
	public String toString() {
		return "" + averagedEstimate;
	}
}


