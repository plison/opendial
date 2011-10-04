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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opendial.domains.Model.Type;
import opendial.domains.types.ActionType;
import opendial.domains.types.EntityType;
import opendial.domains.types.FeatureType;
import opendial.domains.types.FixedVariableType;
import opendial.domains.types.StandardType;
import opendial.domains.types.ObservationType;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * Representation of a dialogue domain, complete with entities and variables,
 * rule-based probabilistic models, observations and actions.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Domain {

	static Logger log = new Logger("DialogueDomain", Logger.Level.DEBUG);
	
	// name of dialogue domain
	String domainName;
	
	// types declarations
	Map<String,StandardType> allTypes;
	
	Map<String,EntityType> entityTypes;
	Map<String,FixedVariableType> fixedVariableTypes;
	Map<String,ObservationType> observationTypes;
	Map<String,ActionType> actionTypes;
	
	// initial state of the domain
	DialogueState initialState;
	
	// the specified models (list of rules)
	Map<Model.Type,Model> models;
	
	
	/**
	 * Create a (empty) dialogue domain
	 * 
	 * @param domainName name of dialogue domain (can be empty string)
	 */
	public Domain(String domainName) {
		this.domainName = domainName;

		allTypes = new HashMap<String,StandardType>();
		entityTypes = new HashMap<String,EntityType>();
		fixedVariableTypes = new HashMap<String,FixedVariableType>();
		observationTypes = new HashMap<String,ObservationType>();
		actionTypes = new HashMap<String,ActionType>();
		
		initialState = new DialogueState();
		models = new HashMap<Model.Type, Model>();
	}
	
	

	public void addInitialState(DialogueState initialState) {
		this.initialState = initialState;
	}
	
	
	public void addModel(Model model) {
		models.put(model.getType(), model);
	}
	
	
	
	public void setName(String domainName) {
		this.domainName = domainName;
	}



	/**
	 * 
	 * @param types
	 */
	public void addTypes(List<StandardType> types) {
		for (StandardType type : types) {
			addType(type);
		}
	}

	/**
	 * 
	 * @param type
	 */
	private void addType(StandardType type) {
		if (type instanceof EntityType) {
			entityTypes.put(type.getName(), (EntityType)type);
		}
		else if (type instanceof FixedVariableType) {
			fixedVariableTypes.put(type.getName(), (FixedVariableType)type);
		}
	//	else if (type instanceof FeatureType) {
	//		featureTypes.put(type.getName(), (FeatureType)type);
	//	}
		else if (type instanceof ObservationType) {
			observationTypes.put(type.getName(), (ObservationType)type);
		}
		else if (type instanceof ActionType) {
			actionTypes.put(type.getName(), (ActionType)type);
		}
		allTypes.put(type.getName(), type);		
	}



	/**
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasType(String type) {
		return allTypes.containsKey(type);
	}


	public StandardType getType(String type) {
		return allTypes.get(type);
	}


	public EntityType getEntityType(String type) {
		return entityTypes.get(type);
	}

	public FixedVariableType getFixedVariableType(String type) {
		return fixedVariableTypes.get(type);
	}
	
	public ObservationType getObversationType(String type) {
		return observationTypes.get(type);
	}
	
	public ActionType getActionType(String type) {
		return actionTypes.get(type);
	}
	
	/**
	 * 
	 * @return
	 */
	public DialogueState getInitialState() {
		return initialState;
	}


	/**
	 * 
	 * @param userPrediction
	 * @return
	 */
	public Model getModel(Type type) {
		return models.get(type);
	}



	/**
	 * TODO: improve efficiency of this!
	 * 
	 * @return
	 */
	public List<EntityType> getEntityTypes() {
		return new ArrayList<EntityType>(entityTypes.values());
	}


	/**
	 * 
	 * @return
	 */
	public List<ObservationType> getObservationTypes() {
		return new ArrayList<ObservationType>(observationTypes.values());

	}

	/**
	 * 
	 * @return
	 */
	public List<ActionType> getActionTypes() {
		return new ArrayList<ActionType>(actionTypes.values());
	}






}
