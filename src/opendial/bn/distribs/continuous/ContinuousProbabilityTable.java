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

package opendial.bn.distribs.continuous;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.AbstractProbabilityTable;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbDistribution;
import opendial.bn.distribs.discrete.DiscreteProbabilityTable;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.Value;

/**
 * Table mapping conditional assignments to continuous probability distributions.
 * This is the continuous equivalent of the ProbabilityTable.
 * 
 * In other words, the distribution is defined as P(X1,...Xn|Y1,...Yn), where X1,...Xn
 * are variables with a continuous domain, whose probabilities are defined via density 
 * functions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ContinuousProbabilityTable 
	extends AbstractProbabilityTable<FunctionBasedDistribution>
	implements ContinuousProbDistribution {

	// logger
	public static Logger log = new Logger("ContinuousProbabilityTable", Logger.Level.NORMAL);
	

	// ===================================
	//  TABLE CONSTRUCTION
	// ===================================

	
	/**
	 * Creates a new continuous probability table
	 * 
	 */
	public ContinuousProbabilityTable() {
		table = new HashMap<Assignment,FunctionBasedDistribution>();
		conditionalVars = new HashSet<String>();
	}
	
	
	/**
	 * Adds a new continuous probability distribution associated with the given
	 * conditional assignment
	 * 
	 * @param condition the conditional assignment
	 * @param distrib the distribution (in a continuous, function-based representation)
	 */
	public void addDistrib (Assignment condition, FunctionBasedDistribution distrib) {
		table.put(condition, distrib);
		conditionalVars.addAll(condition.getVariables());
	}


	// ===================================
	//  GETTERS
	// ===================================


	/**
	 * Returns the probability density associated with the given conditional and head
	 * assignments.  Else, returns 0.0.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the associated density
	 */
	@Override
	public double getProbDensity(Assignment condition, Assignment head) {
		if (table.containsKey(condition)) {
			return table.get(condition).getProbDensity(new Assignment(), head);
		}
		else {
			return 0.0;
		}
	}

	
	/**
	 * Returns the cumulative probability associated with the given conditional and head
	 * assignments. Else, returns 0.0.
	 * 
	 * @param condition the conditional assignment
	 * @param head the head assignment
	 * @return the cumulative probability
	 */
	@Override
	public double getCumulativeProb(Assignment condition, Assignment head) {
		if (table.containsKey(condition)) {
			return table.get(condition).getCumulativeProb(new Assignment(), head);
		}
		else {
			return 0.0;
		}
	}
	
	
	
	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	public Collection<String> getHeadVariables() {
		Set<String> headVars = new HashSet<String>();
		for (FunctionBasedDistribution subdistrib : table.values()) {
			headVars.addAll(subdistrib.getHeadVariables());
		}
		return headVars;
	}
	
	
	

	// ===================================
	//  UTILITY FUNCTIONS
	// ===================================


	
	/**
	 * Returns a copy of the probability table
	 * 
	 * @return the copy
	 */
	@Override
	public ProbDistribution copy() {
		ContinuousProbabilityTable newTable = new ContinuousProbabilityTable();
		for (Assignment condition : table.keySet()) {
			newTable.addDistrib(condition, table.get(condition));
		}
		return newTable;
	}

	
	/**
	 * Returns a pretty print of the distribution
	 * 
	 * @return the pretty print
	 */
	@Override
	public String prettyPrint() {
		String s = "";
		for (Assignment condition : table.keySet()) {
			String distribString = table.get(condition).prettyPrint();
			Pattern p = Pattern.compile("PDF\\((.)*\\)=");
			Matcher m = p.matcher(distribString);
			while (m.find()) {
				String toreplace = m.group();
				distribString = distribString.replace(toreplace, 
						toreplace.substring(0,toreplace.length()-2) + "|" + condition + ")=");
			}
			s += distribString + "\n";
		}
		return s;
	}

	
	/**
	 * Returns a pretty print of the distribution
	 * 
	 * @return the pretty print
	 */
	public String toString() {
		return prettyPrint();
	}
	

	/**
	 * Returns the discrete equivalent of the distribution
	 * 
	 * @return the discrete equivalent
	 */
	@Override
	public DiscreteProbDistribution toDiscrete() {
		DiscreteProbabilityTable newTable = new DiscreteProbabilityTable();
		for (Assignment condition : table.keySet()) {
			newTable.addRows(condition, table.get(condition).toDiscrete());
		}
		return newTable;
	}

	
	/**
	 * Returns itself
	 * 
	 * @return itself
	 */
	@Override
	public ContinuousProbDistribution toContinuous() throws DialException {
		return this;
	}

	
	
	
}
