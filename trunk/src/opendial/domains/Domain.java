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

import opendial.domains.Model.Type;
import opendial.domains.actions.Action;
import opendial.domains.observations.Observation;
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

	static Logger log = new Logger("DialogueDomain", Logger.Level.NORMAL);
	
	// name of dialogue domain
	String domainName;
	
	// entity declarations
	Map<String,EntityType> entityTypes;
	
	// initial state of the domain
	DialogueState initialState;
	
	// the specified models (list of rules)
	Map<Model.Type,Model> models;
	
	// the possible observations for the domain
	List<Observation> observations;
	
	// the possible actions for the domain
	List<Action> actions;
	
	
	/**
	 * Create a (empty) dialogue domain
	 * 
	 * @param domainName name of dialogue domain (can be empty string)
	 */
	public Domain(String domainName) {
		this.domainName = domainName;
		entityTypes = new HashMap<String,EntityType>();
		initialState = new DialogueState();
		models = new HashMap<Model.Type, Model>();
		observations = new ArrayList<Observation>();
		actions = new ArrayList<Action>();
	}
	
	
	public void addEntityType(EntityType newEntity) {
		entityTypes.put(newEntity.getName(), newEntity);
	}


	public void addEntityTypes(List<EntityType> entityTypes) {
		for (EntityType e: entityTypes){
			addEntityType(e);
		}
	}
	

	public void addInitialState(DialogueState initialState) {
		this.initialState = initialState;
	}
	
	
	public void addModel(Model model) {
		models.put(model.getType(), model);
	}
	
	public void addObservation(Observation obs) {
		observations.add(obs);
	}
	
	public void addAction(Action action) {
		actions.add(action);
	}
	
	
	public void setName(String domainName) {
		this.domainName = domainName;
	}

	
	public Collection<EntityType> getEntityTypes() {
		return entityTypes.values();
	}


	/**
	 * 
	 * @param type
	 * @return
	 */
	public boolean hasEntityType(String type) {
		return entityTypes.containsKey(type);
	}

	
	public EntityType getEntityType(String type) {
		return entityTypes.get(type);
	}


	/**
	 * 
	 * @param observations2
	 */
	public void addObservations(List<Observation> observations2) {
		observations.addAll(observations2);
	}


	/**
	 * 
	 * @param actions2
	 */
	public void addActions(List<Action> actions2) {
		actions.addAll(actions2);
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
	 * 
	 * @return
	 */
	public List<Observation> getObservations() {
		return observations;
	}
	
	public List<Action> getActions() {
		return actions;
	}



}
