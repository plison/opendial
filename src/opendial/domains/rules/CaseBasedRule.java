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

package opendial.domains.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.values.DoubleVal;
import opendial.domains.datastructs.Output;
import opendial.domains.datastructs.OutputTable;
import opendial.domains.datastructs.Template;
import opendial.domains.rules.conditions.BasicCondition;
import opendial.domains.rules.conditions.Condition;
import opendial.domains.rules.conditions.VoidCondition;
import opendial.domains.rules.effects.Effect;
import opendial.domains.rules.effects.VoidEffect;
import opendial.domains.rules.parameters.FixedParameter;
import opendial.domains.rules.parameters.Parameter;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.domains.rules.quantification.LabelPredicate;
import opendial.domains.rules.quantification.UnboundPredicate;
import opendial.state.rules.Rule;

/**
 * Generic representation of a case-based rule, with an identifier and an ordered list 
 * of cases.  The class is abstract and is further refined as a decision, 
 * prediction or update rule.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public abstract class CaseBasedRule implements Rule {

	static Logger log = new Logger("Rule", Logger.Level.DEBUG);
	
	// identifier for the rule
	protected String id = "";
	
	// counter for rule increment 
	// (if rule identifier is not directly given)
	public static int idCounter = 0;
		
	// ordered list of cases
	protected List<Case> cases;
	
	// quantifiers;
	protected Set<String> quantifiers;

	// ===================================
	//  RULE CONSTRUCTION
	// ===================================
	
	
	/**
	 * Creates a new rule, with the identifier "r"+idCounter, and an
	 * empty list of cases
	 * 
	 */
	public CaseBasedRule() {
		cases = new LinkedList<Case>();
		id = "r" + idCounter;
		idCounter++;
		quantifiers = new HashSet<String>();
	}
	
	
	/**
	 * Creates a new rule, with the given identifier and an empty list
	 * of cases
	 * 
	 * @param id the identifier
	 */
	public CaseBasedRule(String id) {
		this();
		this.id = id;
	}
	
	/**
	 * Changes the rule identifier
	 * 
	 * @param id the new identifier
	 */
	public void setRuleId(String id) {
		this.id = id;
	}
	

	/**
	 * Adds a new case to the abstract rule
	 * 
	 * @param newCase the new case to add
	 */
	public void addCase(Case newCase) {	
		if (!cases.isEmpty() && cases.get(cases.size()-1).getCondition() 
				instanceof VoidCondition) {
			log.info("new case for rule " + id + 
					" is unreachable (previous case is trivially true)");
		}
		cases.add(newCase);
	}
	
	/**
	 * Adds a new quantifier to the rule
	 * 
	 * @param quantifier the quantifier to add
	 */
	public void addQuantifier(String quantifier) {
		quantifiers.add(quantifier);
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
	 * Returns the default case for the rule (if no explicit
	 * condition applies).  This method is abstract and must
	 * be implemented by the concrete classes
	 * 
	 * @return the default case for the rule
	 */
	// protected abstract Case getDefaultCase() ;
	
	
	
	/**
	 * Returns the input variables (possibly underspecified, with slots 
	 * to fill) for the rule
	 * 
	 * @return the set of labels for the input variables
	 */
	public Set<Template> getInputVariables() {
		Set<Template> variables = new HashSet<Template>();
		for (Case thecase : cases) {
			variables.addAll(thecase.getInputVariables());
		}
		return new HashSet<Template>(variables);
	}
	
	
	/**
	 * Returns the rule outputs generated for the particular input assignment provided
	 * as argument.  The rule outputs are associated with a double value, representing
	 * the output probability or utility.
	 * 
	 * @param input the input assignment
	 * @return the outputs for the effect
	 */
	public OutputTable getEffectOutputs (Assignment input) {
				
		// search for the matching case
		Case matchingCase = getMatchingCase(input);
		
		return matchingCase.getEffectOutputs(input);
	}
	

	/**
	 * Returns the set of unbound predicates in the rule that include at least
	 * one quantifier (as defined as such in the rule specification).
	 * 
	 * @return the set of unbound predicates
	 */
	public Set<UnboundPredicate> getUnboundPredicates() {
		Set<UnboundPredicate> predicates = new HashSet<UnboundPredicate>();
		for (Case c : cases) {
			Set<UnboundPredicate> initPredicates = c.getCondition().getUnboundPredicates();
			for (UnboundPredicate pred : initPredicates) {
				for (String quantifier : quantifiers) {
					if (pred.getVariables().contains(quantifier)) {
						predicates.add(pred);
					}
				}
			}		
		}
		return predicates;
	}
	
	
	// ===================================
	//  UTILITY METHODS
	// ===================================
	
	
	/**
	 * Returns a string representation for the rule
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		String str = id +": ";
		for (Case theCase : cases) {
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
	public int hashCode() {
		return this.getClass().hashCode() - id.hashCode() + cases.hashCode();
	}


	/**
	protected List<Effect> getEffectsForCondition(Condition condition) {
		for (Case ruleCase : cases) {
			if (ruleCase.getCondition().equals(condition)) {
				return ruleCase.getEffects();
			}
		}
		Effect voidEffect = new VoidEffect();
		return Arrays.asList(voidEffect);
	}

	protected Parameter getParameterForEffect(Condition condition, Effect effect) {
		for (Case ruleCase : cases) {
			if (ruleCase.getCondition().equals(condition)) {
				return ruleCase.getParameter(effect);
			}
		}
		if (condition instanceof VoidCondition && effect instanceof VoidEffect) {
			return new FixedParameter(1.0);
		}
		return new FixedParameter(0.0);	
	} */

	
	

	// ===================================
	//  PRIVATE METHODS
	// ===================================
	

	/**
	 * Returns the first case matched by the specific assignment of values given 
	 * as argument.  If no case is matched, returns the default case.
	 * 
	 * @param inputValue the value assignment
	 * @return the first case matched by the assignment, or the default case otherwise
	 */
	private Case getMatchingCase(Assignment inputValue) {
		for (Case ruleCase : cases) {
			if (ruleCase.getCondition().isSatisfiedBy(inputValue)) {
				return ruleCase;
			}
		}
		return new Case();
	}
	

	
}
