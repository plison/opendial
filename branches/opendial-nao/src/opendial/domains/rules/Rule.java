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

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.datastructs.ValueRange;
import opendial.domains.rules.conditions.VoidCondition;


/**
 * Generic representation of a probabilistic rule, with an identifier and an ordered 
 * list of cases. The rule can be either a probability or a utility rule.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Rule {

	static Logger log = new Logger("Rule", Logger.Level.DEBUG);
	
	// the rule identifier
	String id;
		
	// ordered list of cases
	List<RuleCase> cases;
	
	public enum RuleType {PROB, UTIL}
	
	RuleType ruleType;
		
	// ===================================
	//  RULE CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new rule, with the given identifier and type,
	 * and an empty list of cases
	 * 
	 * @param id the identifier
	 * @param ruleType the rule type
	 */
	public Rule(String id, RuleType ruleType) {
		this.id = id;
		this.ruleType = ruleType;
		cases = new ArrayList<RuleCase>();	
	}

	

	/**
	 * Adds a new case to the abstract rule
	 * 
	 * @param newCase the new case to add
	 */
	public void addCase(RuleCase newCase) {	
		if (!cases.isEmpty() && cases.get(cases.size()-1).getCondition() 
				instanceof VoidCondition) {
			log.info("new case for rule " + id + 
					" is unreachable (previous case is trivially true)");
		}
		cases.add(newCase);
	}
	
	
	
	// ===================================
	//  GETTERS
	// ===================================
	
	
	/**
	 * Returns the rule identifier
	 * 
	 * @return the rule identifier
	 */
	public String getRuleId() {
		return id;
	}
	
	
	
	/**
	 * Returns the input variables (possibly underspecified, with slots 
	 * to fill) for the rule
	 * 
	 * @return the set of labels for the input variables
	 */
	public Set<Template> getInputVariables() {
		Set<Template> variables = new HashSet<Template>();
		for (RuleCase thecase : cases) {
			variables.addAll(thecase.getInputVariables());
		}
		return new HashSet<Template>(variables);
	}
	
	
	/**
	 * Returns the first case whose condition matches the input assignment
	 * provided as argument.  The case contains the grounded list of effects
	 * associated with the satisfied condition.
	 * 
	 * @param input the input assignment
	 * @return the matched rule case.
	 */
	public RuleCase getMatchingCase (Assignment input) {

		for (RuleCase ruleCase : cases) {
			if (ruleCase.getCondition().isSatisfiedBy(input)) {
				return ruleCase.ground(input);
			}
		}
		return new RuleCase();
	}
	
	
	/**
	 * Returns the rule type
	 * 
	 * @return the rule type
	 */
	public RuleType getRuleType() {
		return ruleType;
	}
	

	/**
	 * Returns the set of groundings that can be derived from the rule and the
	 * specific input assignment.
	 * 
	 * @param input the input assignment
	 * @return the possible groundings for the rule
	 */
	public Set<Assignment> getGroundings(Assignment input) {
		ValueRange groundings = new ValueRange();
		for (RuleCase thecase :cases) {
			groundings.addRange(thecase.getGroundings(input));
		}
		return groundings.linearise();
	}

	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns a string representation for the rule
	 */
	@Override
	public String toString() {
		String str = id +": ";
		for (RuleCase theCase : cases) {
			if (!theCase.equals(cases.get(0))) {
				str += "\telse ";
			}
			str += theCase.toString() + "\n";
		}
		if (!cases.isEmpty()) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}
	
	/**
	 * Returns the hashcode for the rule
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() {
		return this.getClass().hashCode() - id.hashCode() + cases.hashCode();
	}
	
	
	/**
	 * Returns true if o is a rule that has the same identifier, rule type and list of cases
	 * than the current rule.
	 * 
	 * @param o the object to compare
	 * @ return true if the object is an identical rule, false otherwise.
	 */
	@Override
	public boolean equals (Object o) {
		if (o instanceof Rule) {
			return id.equals(((Rule)o).getRuleId()) 
					&& ruleType.equals(((Rule)o).getRuleType()) 
					&& cases.equals(((Rule)o).cases);
		}
		return false;
	}


	
}
