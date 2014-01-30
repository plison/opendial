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

package opendial.domains;

import java.util.LinkedList;
import java.util.List;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.state.DialogueState;

/**
 * Representation of a dialogue domain, composed of (1) an initial dialogue state and
 * (2) a list of probability and utility models employed to update the dialogue state
 * upon relevant changes.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Domain {

	static Logger log = new Logger("Domain", Logger.Level.NORMAL);
	
	// domain name
	String domainName;	
	
	// initial dialogue state
	DialogueState initState;
	
	BNetwork parameters;
	
	// list of models
	List<Model> models;
	
	// settings
	Settings settings;


	/**
	 * Creates a new domain with an empty dialogue state and list of models.
	 */
	public Domain() {
		settings = new Settings();
		models = new LinkedList<Model>();
		initState = new DialogueState();
		parameters = new BNetwork();
	}

	/**
	 * Sets the domain name 
	 * 
	 * @param domainName the domain name
	 */
	public void setName(String domainName) {
		this.domainName = domainName;
	}

	/**
	 * Sets the initial dialogue state
	 * 
	 * @param initState the initial state
	 */
	public void setInitialState(DialogueState initState) {
		this.initState = initState;
	}
		
	/**
	 * Adds a model to the domain
	 * 
	 * @param model the model to add
	 */
	public void addModel(Model model) {
		models.add(model);
	}
	
	/**
	 * Returns the initial dialogue state
	 * 
	 * @return the initial state
	 */
	public DialogueState getInitialState() {
		return initState;
	}

	/**
	 * Returns the models for the domain
	 * 
	 * @return the models
	 */
	public List<Model> getModels() {
		return models;
	}
	
	
	/**
	 * Replaces the domain-specific settings
	 * 
	 * @param settings the settings
	 */
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	
	
	/**
	 * Returns the domain-specific settings
	 * 
	 * @return the settings for the domain
	 */
	public Settings getSettings() {
		return settings;
	}
	
	
	/**
	 * Returns the domain name.
	 */
	@Override
	public String toString() {
		return domainName;
	}

	/**
	 * Sets the prior distribution for the domain parameters
	 * 
	 * @param parameters the parameters
	 */
	public void setParameters(BNetwork parameters) {
		this.parameters = parameters;
	}

	
	/**
	 * Returns the prior distribution for the domain parameters
	 * 
	 * @return the prior distribution for the parameters
	 */
	public BNetwork getParameters() {
		return parameters;
	}

	
	/**
	 * Returns the domain name
	 * 
	 * @return the domain name
	 */
	public String getName() {
		return domainName;
	}
	
	
}
