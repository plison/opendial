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

import java.util.HashSet;

import opendial.arch.Logger;
import opendial.bn.nodes.BNode;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.gui.GUIFrame;

/**
 *  
 *  The dialogue system should minimally include a domain, and optionally
 *  parameters, and a system configuration (which could be default).
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueSystem {

	// logger
	public static Logger log = new Logger("DialogueSystem", Logger.Level.NORMAL);
	
	Domain domain;
	
	DialogueState curState;
	
	public DialogueSystem(Domain domain) throws DialException {
		this.domain = domain;
		curState = domain.getInitialState().copy();
		for (Model<?> model : domain.getModels()) {
			curState.attachModule(model);
		}
	}
	
	public void showGUI() {
		ConfigurationSettings.getInstance().showGUI(true);
		GUIFrame.getSingletonInstance();
	}
	
	
	public void startSystem() {
		curState.triggerUpdates();
	}

	/**
	 * 
	 * @return
	 */
	public DialogueState getState() {
		return curState;
	}
	
	
}
