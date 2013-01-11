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

package opendial.inference.datastructs;

import opendial.arch.Logger;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.bn.distribs.utility.UtilityDistribution;

/**
 * Couple composed of a probability distribution and an utility distribution
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DistributionCouple {

	// logger
	public static Logger log = new Logger("DistributionCouple", Logger.Level.NORMAL);
	
	// probability distribution
	ProbDistribution probDistrib;
	
	// utility distribution
	UtilityDistribution utilityDistrib;

	/**
	 * Creates a new couple of probability / utility distributions
	 * 
	 * @param probDistrib the probability distribution
	 * @param valueDistrib the utility distribution
	 */
	public DistributionCouple(ProbDistribution probDistrib, UtilityDistribution valueDistrib) {
		this.probDistrib = probDistrib;
		this.utilityDistrib = valueDistrib;
	}

	
	/**
	 * Creates a new couple of probability / utility distributions given a double
	 * factor
	 * 
	 * @param finalProduct the double factor
	 */
	public DistributionCouple(DoubleFactor finalProduct) {
		probDistrib = new SimpleTable(finalProduct.getProbMatrix());
		utilityDistrib = new UtilityTable(finalProduct.getUtilityMatrix());
	}

	/**
	 * Returns the probability distribution for the couple
	 * 
	 * @return the probability distribution
	 */
	public ProbDistribution getProbDistrib() {
		return probDistrib;
	}

	/**
	 * Returns the utility distribution for the couple
	 * 
	 * @return the utility distribution
	 */
	public UtilityDistribution getUtilityDistrib() {
		return utilityDistrib;
	}
}

