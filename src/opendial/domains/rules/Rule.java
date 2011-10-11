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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.domains.rules.variables.StandardVariable;
import opendial.utils.Logger;

/**
 * Representation of a rule.  A rule is specified with a list of input variables,
 * a list of output variables, and a list of cases (which are condition-effects 
 * mappings defined on the input and output variables).
 * 
 * <p>It is important to note that the list of cases is <>ordered</i>, and that this 
 * ordering matters for the way probabilistic inference is performed on the rule. 
 * The list of cases is indeed defined as an <i>if then else</i> construction:
 * <pre>
 * if (condition1) then ...
 * else if (condition2) then ...
 * else if (condition3) then ...
 * else then ...
 * </pre> 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Rule {

	// logger
	static Logger log = new Logger("Rule", Logger.Level.DEBUG);
	
	// the list of input variables, anchored by their identifier
	Map<String,StandardVariable> inputs;
	
	// the list of output variables, anchored by their identifier
	Map<String,StandardVariable> outputs;
	
	// the list of cases
	List<Case> cases;
	
	/**
	 * Creates a new rule, with empty inputs and outputs, and no cases.
	 */
	public Rule() {
		inputs = new HashMap<String,StandardVariable>();
		outputs = new HashMap<String,StandardVariable>();
		cases = new LinkedList<Case>();
	}
	
	/**
	 * Adds an input variable to the rule
	 * 
	 * @param var the input variable
	 */
	public void addInputVariable(StandardVariable var) {
		if (inputs.containsKey(var.getIdentifier())) {
			log.warning("Identifier " + var.getIdentifier() + 
				" has already been defined in the input variables!");
		}
		inputs.put(var.getIdentifier(), var);
	}
	
	/**
	 * Adds a list of input variables to the rule
	 * 
	 * @param vars the list of input variables
	 */
	public void addInputVariables(List<StandardVariable> vars) {
		for (StandardVariable var: vars) {
			addInputVariable(var);
		}
	}
	
	/**
	 * Adds an output variable to the rule
	 * 
	 * @param var the output variable
	 */
	public void addOutputVariable(StandardVariable var) {
		if (outputs.containsKey(var.getIdentifier())) {
			log.warning("Identifier " + var.getIdentifier() + 
				" has already been defined in the output variables!");
		}
		outputs.put(var.getIdentifier(), var);
	}
	
	
	/**
	 * Adds a list of output variables to the rule
	 * 
	 * @param vars the output variables
	 */
	public void addOutputVariables(List<StandardVariable> vars) {
		for (StandardVariable var: vars) {
			addOutputVariable(var);
		}
	}

	
	/**
	 * Adds a new case to the rule (inserted at the end of the list)
	 * 
	 * @param case1 the case to add
	 */
	public void addCase(Case case1) {
		cases.add(case1);
	}

	/**
	 * Returns true if there is an input variable referenced by the
	 * given identifier, false otherwise
	 * 
	 * @param identifier the id to check
	 * @return false if input variable is found, false otherwise
	 */
	public boolean hasInputVariable(String identifier) {
		return inputs.containsKey(identifier);
	}
	
	/**
	 * Returns the input variable referenced by the identifier, if 
	 * one exists.  Else, returns null.
	 * 
	 * @param identifier the identifier for the variable
	 * @return the variable
	 */
	public StandardVariable getInputVariable(String identifier) {
		return inputs.get(identifier);
	}

	/**
	 * Returns true if there is an output variable referenced by the
	 * given identifier, false otherwise
	 * 
	 * @param identifier the id to check
	 * @return false if output variable is found, false otherwise
	 */
	public boolean hasOutputVariable(String denotation) {
		return outputs.containsKey(denotation);
	}

	
	/**
	 * Returns the output variable referenced by the identifier, if 
	 * one exists.  Else, returns null.
	 * 
	 * @param identifier the identifier for the variable
	 * @return the variable
	 */
	public StandardVariable getOutputVariable(String denotation) {
		return outputs.get(denotation);
	}

	/**
	 * Returns the list of input variables for this rule
	 * 
	 * @return the input variables
	 */
	public List<StandardVariable> getInputVariables() {
		return new ArrayList<StandardVariable>(inputs.values());
	}
	
	/**
	 * Returns the list of output variables for this rule
	 * 
	 * @return the output variables
	 */
	public List<StandardVariable> getOutputVariables() {
		return new ArrayList<StandardVariable>(outputs.values());
	}

	/**
	 * Returns the (ordered) list of cases for the rule
	 * 
	 * @return the cases
	 */
	public List<Case> getCases() {
		return cases;
	}

	/**
	 * Returns true if the rule has an input or output variable
	 * referenced by the identifier
	 * 
	 * @param identifier variable identifier
	 * @return true if a variable has been found, false otherwise
	 */
	public boolean hasVariable(String identifier) {
		if (inputs.containsKey(identifier)) {
			return true;
		}
		else if (outputs.containsKey(identifier)) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the variable which is referenced by the identifier,
	 * if one exists.  Else, returns null.
	 * 
	 * @param identifier the variable identifier
	 * @return the variable if it exists in the rule, else null.
	 */
	public StandardVariable getVariable(String identifier) {
		if (inputs.containsKey(identifier)) {
			return inputs.get(identifier);
		}
		else if (outputs.containsKey(identifier)) {
			return outputs.get(identifier);
		}
		return null;
	}
	
}


