// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
 * @version $Date:: 2014-03-20 21:16:08 #$
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
