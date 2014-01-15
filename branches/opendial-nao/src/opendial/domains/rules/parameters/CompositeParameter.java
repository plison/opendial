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

package opendial.domains.rules.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;

/**
 * Parameter represented as a combination of several continuous parameters.  
 * The combination may either take the form of a linear combination
 * or a multiplication.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class CompositeParameter implements Parameter {

	// logger
	public static Logger log = new Logger("CompositionParameter", Logger.Level.NORMAL);

	// distributions for the composite parameter, indexed by their variable name
	Set<Parameter> parameters;

	// operator used to combine the sub-parameters
	public static enum Operator { ADD, MULTIPLY }

	Operator operator = Operator.ADD;

	// ===================================
	//  PARAMETER CONSTRUCTION
	// ===================================

	
	
	/**
	 * Creates a new composite parameter 
	 */
	public CompositeParameter(Operator operator) {
		this.parameters = new HashSet<Parameter>();
		this.operator = operator;
	}


	/**
	 * Creates a new composite parameter with the given parameter distributions
	 * 
	 * @param paramIds the collection of distributions
	 */
	public CompositeParameter(Collection<Parameter> paramIds, Operator operator) {
		this(operator);
		this.parameters.addAll(paramIds);
	}


	/**
	 * Adds a new parameter in the composite distribution
	 * 
	 * @param param the parameter to add
	 */
	public void addParameter(Parameter param) {
		if (param instanceof CompositeParameter && ((CompositeParameter)param).getOperator() == operator) {
			parameters.addAll(((CompositeParameter)param).getParameters());
		}
		else {
			parameters.add(param);
		}
	}


	/**
	 * Sums the parameter with the other parameter and returns the result.
	 * 
	 * @param the other parameter 
	 * @return the parameter resulting from the addition of the two parameters
	 */
	@Override
	public Parameter sumParameter(Parameter otherParam) {
		if (operator == Operator.ADD && otherParam instanceof CompositeParameter) {
			List<Parameter> paramList = new ArrayList<Parameter>();
			paramList.addAll(parameters);
			paramList.addAll(((CompositeParameter)otherParam).getParameters());
			return new CompositeParameter(paramList, Operator.ADD);
		}
		else {
			return new CompositeParameter(Arrays.asList(this, otherParam), Operator.ADD);			
		}
	}

	
	/**
	 * Multiplies the parameter with the other parameter and returns the result.
	 * 
	 * @param the other parameter 
	 * @return the parameter resulting from the multiplication of the two parameters
	 */
	@Override
	public Parameter multiplyParameter(Parameter otherParam) {
		if (operator == Operator.MULTIPLY && otherParam instanceof CompositeParameter) {
			List<Parameter> paramList = new ArrayList<Parameter>();
			paramList.addAll(parameters);
			paramList.addAll(((CompositeParameter)otherParam).getParameters());
			return new CompositeParameter(paramList, Operator.MULTIPLY);
		}
		else {
			return new CompositeParameter(Arrays.asList(this, otherParam), Operator.MULTIPLY);			
		}
	}
	
	

	// ===================================
	//  GETTERS
	// ===================================

	
	
	/**
	 * Returns the operator for the parameter.
	 * 
	 * @return the operator
	 */
	private Operator getOperator() {
		return operator;
	}


	/**
	 * Returns the set of parameters included in this composite parameter
	 * 
	 * @return the set of included parameters
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}


	/**
	 * Returns the actual parameter value (as a double) given the particular
	 * value assignment.  The assignment should contain the actual values for 
	 * the sub-parameters.
	 * 
	 * @param input the input assignment
	 * @return the parameter value
	 * @throws DialException if the value could not be retrieved from the assignment
	 */
	public double getParameterValue(Assignment input) throws DialException {
		double totalValue = (operator == Operator.ADD)? 0.0 : 1.0;
		for (Parameter paramVar : parameters) {
			double paramVal = paramVar.getParameterValue(input);
			switch (operator) {
			case ADD: totalValue += paramVal; break;
			case MULTIPLY: totalValue *= paramVal; break;
			}
		}
		return totalValue;
	}


	/**
	 * Returns the collection of elementary parameter identifiers for the composite
	 * parameter
	 *
	 * @return the collection of parameter identifiers
	 */
	public Collection<String> getParameterIds() {
		Set<String> ids = new HashSet<String>();
		for (Parameter param : parameters) {
			ids.addAll(param.getParameterIds());
		}
		return ids;
	}


	// ===================================
	//  UTILITY METHODS
	// ===================================

	
	
	/**
	 * Returns a string representation of the composite parameter
	 */
	public String toString() {
		String str = "";
		for (Parameter param : parameters) {
			switch (operator) {
			case ADD: str += param.toString() + "+"; break;
			case MULTIPLY : str += param.toString() + "*"; break;
			}
		}
		return str.substring(0, str.length()-1);		
	}


	/**
	 * Returns true if the composite parameter is identical to the given
	 * object, and false otherwise
	 *
	 * @param o the object to compare
	 * @return true if identical, false otherwise
	 */
	public boolean equals(Object o) {
		if (o instanceof CompositeParameter) {
			return parameters.equals(((CompositeParameter)o).getParameterIds());
		}
		return false;
	}


	/**
	 * Returns the hashcode for the parameter
	 *
	 * @return the hashcode
	 */
	public int hashCode() {
		return parameters.hashCode() - operator.hashCode();
	}



}
