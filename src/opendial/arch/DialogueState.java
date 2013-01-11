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

package opendial.arch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import opendial.arch.Logger;
import opendial.arch.statechange.DistributionRule;
import opendial.arch.statechange.Rule;
import opendial.arch.statechange.StateController;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.bn.distribs.ProbDistribution;
import opendial.bn.nodes.BNode;
import opendial.modules.SynchronousModule;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueState {

	// logger
	public static Logger log = new Logger("DialogueState", Logger.Level.DEBUG);

	protected BNetwork network;

	Assignment evidence;

	StateController controller;

	boolean isFictive = false;
	
	public DialogueState() {
		this(new BNetwork());
	}

	public DialogueState(BNetwork network) {
		this.network = network;
		evidence = new Assignment();
		controller = new StateController(this);
		for (String nodeId : network.getNodeIds()) {
			controller.setVariableAsNew(nodeId);
		}
	}

	public void addEvidence(Assignment newEvidence) {
		evidence.addAssignment(newEvidence);
	}

	public Assignment getEvidence() {
		return evidence;
	}

	public BNetwork getNetwork() {
		return network;
	}

	public void attachModule(SynchronousModule module) {
		controller.attachModule(module);
	}

	public void addContent(ProbDistribution distrib, String origin) 
			throws DialException {
		origin = network.getUniqueNodeId(origin);
		applyRule(new DistributionRule(distrib, origin));
	}
		
	public void applyRule(Rule rule) throws DialException {
		controller.applyRule(rule);
	}

	
	public DialogueState copy() throws DialException {
		BNetwork netCopy = network.copy();
		DialogueState stateCopy = new DialogueState(netCopy);
		stateCopy.addEvidence(evidence.copy());
		return stateCopy;
	}
	
	public void triggerUpdates() {
		controller.triggerUpdates();
	}
	
	
	public boolean isStable() {
		return controller.isStable();
	}
	
	public void setAsFictive(boolean fictive) {
		this.isFictive = fictive;
	}
	
	public boolean isFictive() {
		return isFictive;
	}
	
	@Override
	public String toString() {
		String s = "Current network: ";
		s += network.toString();
		if (!evidence.isEmpty()) {
			s += " (with evidence " + evidence + ")";
		}
		s += "\n" + controller.toString();
		return s;
	}

	
	public StateController getController() {
		return controller;
	}

	
}

