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
import java.util.List;
import java.util.Map;

import opendial.arch.DialConstants.ModelGroup;
import opendial.domains.types.GenericType;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * Representation of a dialogue domain for openDial, complete with: <ul>
 * <li> (optional) a domain name;
 * <li> a list of variable types;
 * <li> an initial state;
 * <li> and a set of rule-based probabilistic models. </ul>
 *
 * @see Model
 * @see GenericType
 * @see Rule
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
	Map<String,GenericType> types;
	
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

		types = new HashMap<String,GenericType>();
		
		initialState = new DialogueState();
		models = new HashMap<ModelGroup, Model>();
	}
	
	
	/**
	 * Adds the initial state to the dialogue domain
	 * 
	 * @param initialState the initial state
	 */
	public void addInitialState(DialogueState initialState) {
		this.initialState = initialState;
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
	 */
	public void addTypes(Collection<GenericType> types) {
		for (GenericType type : types) {
			addType(type);
		}
	}

	
	/**
	 * Adds a single declared type to the domain
	 * 
	 * @param type the type to add
	 */
	private void addType(GenericType type) {
		types.put(type.getName(), type);		
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
	public GenericType getType(String typeName) {
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
	public List<GenericType> getTypes() {
		return new ArrayList<GenericType>(types.values());
	}






}
