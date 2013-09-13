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
import java.util.LinkedList;
import java.util.List;

import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.nodes.BNode;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.gui.GUIFrame;
import opendial.modules.AsynchronousModule;
import opendial.modules.SynchronousModule;
import opendial.simulation.Simulator;
import opendial.state.DialogueState;

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
	
	GUIFrame gui;
	
	Simulator simulator;
	
	List<AsynchronousModule> asyncModules;
	
	boolean paused = false;

		
	public DialogueSystem(Domain domain) throws DialException {
		this (Settings.getInstance(), domain);
	}
	
	public DialogueSystem(Settings settings, Domain domain) throws DialException {
		Settings.loadSettings(settings);
		curState = domain.getInitialState().copy();
		curState.setName("current");
		for (Model<?> model : domain.getModels()) {
			curState.attachModule(model);
		}
		if (Settings.getInstance().gui.showGUI) {
			gui = new GUIFrame(this);
			curState.addListener(gui);
		}
		asyncModules = new LinkedList<AsynchronousModule>();
		attachShutDownHook();
	}
	
	
	public void attachAsynchronousModule(AsynchronousModule module) {
		asyncModules.add(module);
		(new Thread(module)).start();
	}
	
	public void addParameters(BNetwork parameterNetwork) {
		curState.addParameters(parameterNetwork);
	}
	

	
	public void startSystem() {
		curState.startState();
		if (simulator != null) {
			simulator.startSimulator();
		}
	}

	/**
	 * 
	 * @return
	 */
	public DialogueState getState() {
		return curState;
	}

	public GUIFrame getGUI() {
		return gui;
	}

	public void attachSimulator(Simulator simulator) throws DialException {
		this.simulator = simulator;
		if (Settings.getInstance().gui.showGUI) {
			simulator.addListener(gui);
		}
	}
	
	
	public void attachShutDownHook(){
		  Runtime.getRuntime().addShutdownHook(new Thread() {
		   @Override
		   public void run() {
		    for (AsynchronousModule module: asyncModules) {
		    	module.shutdown();
		    }
		    curState.shutdown();
			  System.out.println("Shutting down dialogue system");
		   }
		  });
		 }
	
	
	public void pause(boolean shouldBePaused) {
		paused = shouldBePaused;
		if (simulator != null) {
			simulator.pause(shouldBePaused);
		}
	}

	public boolean isPaused() {
		return paused;
	}
	
}
