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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;

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
	NaoSession session;
	boolean paused = true;


	public NaoBehaviour(DialogueSystem system) throws DialException {
		this.system = system;
		session = NaoSession.grabSession(system.getSettings());
	}

	@Override
	public void start() throws DialException {
		paused = false;
		executeBehaviour(Arrays.asList("standup"));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {  
				System.out.println("Shutting down Nao Behaviour");
				try {  
					executeBehaviour(Arrays.asList("kneel"));
					session.call("ALBehaviorManager", "stopAllBehaviors");;
				}	catch (Exception e) {}
			}
		});
	}


	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	public boolean isRunning() {
		return !paused;
	}


	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {	
		if  (session != null && updatedVars.contains(ACTION_VAR) && !paused) {
			String actionValue = getActionValue(state);
			if (actionValue != null) {
				executeBehaviour(Arrays.asList(actionValue));
			}
		}
	}



	private String getActionValue(DialogueState state) {
		if (state.hasChanceNode(ACTION_VAR)) {
			CategoricalTable actionTable = state.queryProb(ACTION_VAR).toDiscrete();
			String fullVal = actionTable.getBest().getValue(ACTION_VAR).toString();
			Matcher m = Pattern.compile("Do\\((.*)\\)").matcher(fullVal);
			if (m.find()) {
				return m.group(1).toLowerCase().replace("(", "-").replace(",", "_").replace(")", "");
			} 
			else if (fullVal.contains("Goodbye")) {
				return "kneel";
			}
		}
		return null;
	}



	private void executeBehaviour (List<String> parallelBehaviours) {
		try {	

			for (String behaviourName : parallelBehaviours) {	

				if (session.<Boolean>call("ALBehaviorManager", "isBehaviorInstalled", behaviourName)) {
					BehaviourControl control = new BehaviourControl(behaviourName);
					control.start();
				}

				else if (behaviourName.equals("stop")) {
					log.info("stopping all behaviours!");
					session.call("ALBehaviorManager", "stopAllBehaviors");
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
				NaoASR asr = system.getModule(NaoASR.class);
			
				if (asr != null) asr.lockASR("NaoBehaviour");
				
				system.addContent(new CategoricalTable(new Assignment("motion", true)));
				
				session.call("ALBehaviorManager","runBehavior", behaviour);
				
				if (asr != null) asr.unlockASR("NaoBehaviour");
				
				log.debug("behaviour " + behaviour + " successfully completed");

				system.getState().removeNode("motion");

				if (behaviour.contains("pickup")) {
					String obj = behaviour.split("-")[1];
					system.addContent(new Assignment("carried", ValueFactory.create("["+obj + "]")));
					system.addContent(new Assignment(system.getSettings().systemOutput, "Object successfully grasped"));
					session.call("ALMemory", "insertData", "carryObj", true); 
				}
				else if (behaviour.contains("release")) {
					system.addContent(new Assignment("carried", ValueFactory.create("[]")));
					system.addContent(new Assignment(system.getSettings().systemOutput, "Object successfully released"));
					session.call("ALMemory", "insertData", "carryObj", false); 
				}
				
			}
			catch (Exception e) {
				log.info("Exception: " + e.toString());
			}
		}
	}


}
