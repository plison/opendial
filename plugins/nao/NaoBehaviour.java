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

package opendial.plugins;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoBehaviour implements Module {

	static Logger log = new Logger("NaoBehaviour", Logger.Level.DEBUG);

	public static final String ACTION_VAR = "a_m";
	public static final String APP_NAME = "lenny";
	
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
		session.call("ALAutonomousLife", "setState", "disabled");
		session.call("ALMotion", "setExternalCollisionProtectionEnabled", "All", false);
		executeBehaviour(Arrays.asList("standup"));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down Nao Behaviour");
				try {  
					executeBehaviour(Arrays.asList("kneel"));
					session.call("ALBehaviorManager", "stopAllBehaviors");;
				}	catch (Exception e) {}
		}));
	}


	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
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
			String fullVal = state.queryProb(ACTION_VAR).toDiscrete().getBest().toString();
			Matcher m = Pattern.compile("Do\\((.*)\\)").matcher(fullVal);
			if (m.find()) {
				return m.group(1).toLowerCase();
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
				
				if (APP_NAME != null && session.<Boolean>call("ALBehaviorManager", "isBehaviorInstalled", APP_NAME + "/" + behaviourName)) {
					BehaviourControl control = new BehaviourControl(APP_NAME + "/" + behaviourName);
					control.start();
				}
				
				else if (session.<Boolean>call("ALBehaviorManager", "isBehaviorInstalled", behaviourName)) {
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
				
				system.addContent(new Assignment("motion", true));
				
				session.call("ALBehaviorManager","runBehavior", behaviour);
				
				if (asr != null) asr.unlockASR("NaoBehaviour");
				
				log.debug("behaviour " + behaviour + " successfully completed");

				system.getState().removeNode("motion");

				if (behaviour.contains("pickup")) {
					Matcher m = Pattern.compile("pickup\\((.*)\\)").matcher(behaviour);
					if (m.find()) {
						String obj =  m.group(1).toLowerCase();
						system.addContent(new Assignment("carried", ValueFactory.create("["+obj + "]")));
						system.addContent(new Assignment(system.getSettings().systemOutput, "Object successfully grasped"));
						session.call("ALMemory", "insertData", "carryObj", true); 
					} 			
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
