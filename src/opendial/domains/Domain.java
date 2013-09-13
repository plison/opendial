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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.domains.rules.CaseBasedRule;
import opendial.domains.rules.parameters.SingleParameter;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Domain {

	static Logger log = new Logger("Domain", Logger.Level.NORMAL);
	
	// domain name
	String domainName;

	// and the configuration settings?
	
	BNetwork parameters;
	
	// initial dialogue state
	DialogueState initState;
	
	List<Model<? extends CaseBasedRule>> models;

	
	public Domain() {
		models = new LinkedList<Model<? extends CaseBasedRule>>();
		parameters = new BNetwork();
		initState = new DialogueState();
	}
	

	/**
	 * 
	 * @param nodeValue
	 */
	public void setName(String domainName) {
		this.domainName = domainName;
	}

		
	
	public void addModel(Model<? extends CaseBasedRule> model) {
		models.add(model);
	}
	
	public List<Model<? extends CaseBasedRule>> getModels() {
		return models;
	}
	


	/**
	 * 
	 * @param network
	 */
	public void setInitialState(DialogueState initState) {
		this.initState = initState;
	}
	
	
	public DialogueState getInitialState() {
		return initState;
	}

	
}
