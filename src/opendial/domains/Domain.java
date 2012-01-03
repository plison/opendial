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

package opendial.domains;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.arch.DialConstants.ModelGroup;
import opendial.arch.DialException;
import opendial.domains.values.ObservationValue;
import opendial.state.DialogueState;
import opendial.state.Fluent;
import opendial.utils.Logger;

/**
 * Representation of a dialogue domain for openDial, complete with: <ul>
 * <li> (optional) a domain name;
 * <li> a list of variable types;
 * <li> an initial state;
 * <li> and a set of rule-based probabilistic models. </ul>
 *
 * @see Model
 * @see Type
 * @see opendial.domains.rules.Rule
 * @see DialogueState
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Domain {

	// logger
	static Logger log = new Logger("Domain", Logger.Level.DEBUG);
	
	// name of dialogue domain
	String domainName;
	
	// types declarations
	Map<String,Type> types;
	
	List<String> typesWithObservations;

	List<String> typesWithActions;

	// initial state of the domain
	DialogueState initialState;
	
	// the specified models (list of rules)
	Map<ModelGroup,Model> models;
	
	
	/**
	 * Create a (empty) dialogue domain
	 * 
	 * @param domainName name of dialogue domain (can be empty string)
	 */
	public Domain(String domainName) {
		this.domainName = domainName;

		types = new HashMap<String,Type>();
		typesWithObservations = new LinkedList<String>();
		typesWithActions = new LinkedList<String>();
		
		initialState = new DialogueState();
		models = new HashMap<ModelGroup, Model>();
	}
	
	
	/**
	 * Adds the initial state to the dialogue domain
	 * 
	 * @param initialState the initial state
	 */
	public void addInitialState(DialogueState initialState) {
		for (Fluent f: initialState.getFluents()) {
			this.initialState.addFluent(f);
		}
	}
	
	
	/**
	 * Adds a rule-based probabilistic model to the domain
	 * 
	 * @param model the model to add
	 */
	public void addModel(Model model) {
		models.put(model.getGroup(), model);
	}
	
	
	/**
	 * Sets the name of the domain
	 * 
	 * @param domainName domain name
	 */
	public void setName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 * Returns the name of the domain
	 * 
	 * @return the domain name
	 */
	public String getName() {
		return domainName;
	}

	
	/**
	 * Adds a collection of declared types to the domain
	 * 
	 * @param types the types to add
	 * @throws DialException 
	 */
	public void addTypes(Collection<Type> types) throws DialException {
		for (Type type : types) {
			addType(type);
		}
	}

	
	/**
	 * Adds a single declared type to the domain
	 * 
	 * @param type the type to add
	 * @throws DialException 
	 */
	private void addType(Type type) throws DialException {
		types.put(type.getName(), type);
		if (!type.getAllValues().isEmpty() && type.getAllValues().get(0) instanceof ObservationValue) {
			typesWithObservations.add(type.getName());
		}
		if (type.isFixed() && !(type.getAllValues().get(0) instanceof ObservationValue)) {
			if (initialState.getFluent(type.getName()) == null) {
			Fluent newFluent = new Fluent(type);
			newFluent.addValue("None", 1.0f);
			initialState.addFluent(newFluent);
			}
		}
	}



	/**
	 * Returns true if the domain contained a type with the
	 * given identifier, false otherwise
	 * 
	 * @param typeName the type name
	 * @return true if domain contains the type, false otherwise
	 */
	public boolean hasType(String typeName) {
		return types.containsKey(typeName);
	}


	/**
	 * Returns the type associated with the name, if one
	 * exists in the domain.  Else, returns null.
	 * 
	 * @param typeName the type name
	 * @return the associated type
	 */
	public Type getType(String typeName) {
		return types.get(typeName);
	}

	
	/**
	 * Returns the initial state for the domain
	 * 
	 * @return the initial state
	 */
	public DialogueState getInitialState() {
		return initialState;
	}


	/**
	 * Returns the model for the given group, if one is defined.
	 * Else, returns null.
	 * 
	 * @param group the model group
	 * @return the associated model
	 */
	public Model getModel(ModelGroup group) {
		return models.get(group);
	}



	/**
	 * Returns the types declared in the model
	 * 
	 * @return all types
	 */
	public List<Type> getTypes() {
		return new ArrayList<Type>(types.values());
	}



	public List<Type> getTypesWithAttachedObservations() {
		List<Type> typesWithObs = new LinkedList<Type>();
		for (String type : typesWithObservations) {
			typesWithObs.add(types.get(type));
		}
		return typesWithObs;
	}


}
