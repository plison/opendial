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

package opendial.nao;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoBehaviour implements Module {

	static Logger log = new Logger("NaoBehaviour", Logger.Level.DEBUG);

	public static final String ACTION_VAR = "a_m";
	
	DialogueSystem system;
	NaoSession manager;
	boolean paused = true;
	

	@Override
	public void start(DialogueSystem system) throws DialException {
		this.system = system;
		manager = NaoSession.grabSession(system.getSettings());
		paused = false;
		executeBehaviour(Arrays.asList("standup"));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			   @Override
			   public void run() {  
				  System.out.println("Shutting down Nao Behaviour");
				  try {  
					executeBehaviour(Arrays.asList("kneel"));
					  manager.call("ALBehaviorManager", "stopAllBehaviors");;
				}	catch (Exception e) {}
			   }
			 });
		NaoCarryDetection.initialise(manager.getIP());
	}


	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}


	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger() {
		if  (manager != null && system.getState().getUpdatedVariables().contains(ACTION_VAR + "'") && !paused && 
				system.getState().hasChanceNode(ACTION_VAR  + "'") && !getActionValue().equals("")) {
		String actionValue = getActionValue();
		if (!actionValue.equals("")) {
			log.debug("executing behaviour " + actionValue);
			executeBehaviour(Arrays.asList(actionValue));
		}
		else {
			log.debug("actionValue value is null");
		}
		}
	}
	


	private String getActionValue() {
			CategoricalTable actionTable = system.getContent(ACTION_VAR  + "'").toDiscrete();
			String fullVal = actionTable.getBest().getValue(ACTION_VAR  + "'").toString();
			Matcher m = Pattern.compile("Do\\((.*)\\)").matcher(fullVal);
			if (m.find()) {
				return m.group(1);
			} 
			else if (fullVal.contains("Goodbye")) {
				return "kneel";
			}
			return "";
	}



	private void executeBehaviour (List<String> parallelBehaviours) {
		try {	

			for (String behaviourName : parallelBehaviours) {	
				behaviourName = behaviourName.toLowerCase().replace("(", "-").replace(",", "_").replace(")", "");
			
				if (manager.<Boolean>call("isBehaviorInstalled", behaviourName)) {
					BehaviourControl control = new BehaviourControl(behaviourName);
					control.start();
				}
				
				else if (behaviourName.equals("stop")) {
					log.info("stopping all behaviours!");
					manager.call("ALBehaviorManager", "stopAllBehaviors");
				}
				
				else {
					log.info("behaviour " + behaviourName + " not present in robot library");
				}

			}	
		}
		catch (Exception e) {
			log.warning("cannot execute behaviour: " + e.toString());
		}
	}





	private final class BehaviourControl extends Thread {

		String behaviour;

		public BehaviourControl (String behaviour) {
			this.behaviour = behaviour;
		}

		@Override
		public void run() {
			try {
				log.debug("starting behaviour " + behaviour);
	
				behaviour = behaviour.toLowerCase().replace("(", "-").replace(",", "_").replace(")", "");
				
				manager.call("ALBehaviorManager","runBehavior", behaviour);
	
				while (!manager.<Boolean>call("ALBehaviorManager", "isBehaviorRunning", behaviour)) {
					Thread.sleep(50);
				}
				if (!system.getState().hasChanceNode("motion")) {
					system.addContent(new CategoricalTable(new Assignment("motion", true)));
				}
				
				while (manager.<Boolean>call("ALBehaviorManager", "isBehaviorRunning", behaviour)) {
					Thread.sleep(50);
				}
				system.getState().removeNode("motion");
				log.debug("behaviour " + behaviour + " + finished");

				if (behaviour.contains("pickup")) {
					String obj = behaviour.split("-")[1];
					boolean isCarried =  true; // CarriedObjectDetection.detectCarriedObject();
					if (isCarried) {
						updateCarriedVariable(1.0, obj);
						system.addContent(new Assignment(system.getSettings().systemOutput, "Object successfully grasped"));
					}
				}
				else if (behaviour.contains("release")) {
					updateCarriedVariable(1.0, "");
					system.addContent(new Assignment(system.getSettings().systemOutput, "Object successfully released"));
				}				
			}
			catch (Exception e) {
				log.info("Exception: " + e.toString());
			}
		}
	}



	private void updateCarriedVariable(double carriedProb, String obj) {
		try {
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("carried", ValueFactory.create("["+obj+"]")), carriedProb);
		if (!obj.equals("")) {
			manager.call("ALMemory", "insertData", "carryObj", true); 
			table.addRow(new Assignment("carried", ValueFactory.create("[]")), 1- carriedProb);
		}
		else {
			manager.call("ALMemory", "carryObj", false); 
		}
		system.addContent(table);
		}
		catch (Exception e) {
			log.warning("could not update carried variable: " + e);
		}
	}


}
