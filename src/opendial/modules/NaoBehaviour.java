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

package opendial.modules;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aldebaran.qimessaging.CallError;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.ValueFactory;
import opendial.modules.asr.ASRLock;
import opendial.modules.behaviour.CarriedObjectDetection;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NaoBehaviour implements SynchronousModule {

	static Logger log = new Logger("NaoBehaviour", Logger.Level.DEBUG);

	com.aldebaran.qimessaging.Object manager;

	public NaoBehaviour() {
		try {
			manager = NaoSession.grabSession().getService("ALBehaviorManager");
			executeBehaviour(Arrays.asList("standup"), new DialogueState());
			CarriedObjectDetection.initialise();
		}
		catch (Exception e) { log.warning("unable to connect to Nao"); } 	
	}

	/**
	 *
	 * @param varnode
	 * @param change
	 * @return
	 */
	@Override
	public boolean isTriggered(DialogueState state) {
		if  (manager != null && state.isVariableToProcess("a_m'") && 
				state.getNetwork().hasChanceNode("a_m'") && !getActionValue(state).equals("")) {
			return true;
		}
		return false;
	}


	/**
	 *
	 * @param state
	 */
	@Override
	public void trigger(DialogueState state) {
		String actionValue = getActionValue(state);
		if (!actionValue.equals("")) {
			log.debug("executing behaviour " + actionValue);
			executeBehaviour(Arrays.asList(actionValue), state);
		}
		//	NaoASR.unlockASR("movement");
		else {
			log.debug("actionValue value is null");
		}
	}
	


	private String getActionValue(DialogueState state) {
		try {
			SimpleTable actionTable = state.getContent("a_m'", true).toDiscrete().getProbTable(new Assignment());
			String fullVal = actionTable.getRows().iterator().next().getValue("a_m'").toString();
			Pattern p = Pattern.compile("Do\\((.*)\\)");
			Matcher m = p.matcher(fullVal);
			if (m.find()) {
				return m.group(1);
			} 
			else if (fullVal.contains("Goodbye")) {
				return "kneel";
			}
			return "";
		}
		catch (DialException e) {
			log.warning("problem extracting the action value: " + e);
			return "";
		}
	}



	private void executeBehaviour (List<String> parallelBehaviours, DialogueState state) {
		//	log.debug("trying to execute behaviour: " + parallelBehaviours);
		try {	

			List<BehaviourControl> controls = new LinkedList<BehaviourControl>();
			for (String behaviourName : parallelBehaviours) {	
				behaviourName = behaviourName.toLowerCase().replace("(", "-").replace(",", "_").replace(")", "");
				//			log.info("executing behaviour " + behaviourName);
				if (behaviourName.equals("stop")) {
					log.info("stopping all behaviours!");
					manager.call("stopAllBehaviors");
				}
				else if (manager.<Boolean>call("isBehaviorInstalled", behaviourName).get()) {
					//	if (!behaviourName.contains("Foot")) {
					BehaviourControl control = new BehaviourControl(behaviourName, 
							NaoSession.grabSession().getService("ALBehaviorManager"), state);
					control.start();
					controls.add(control);
				}
				else {
					log.info("behaviour " + behaviourName + " not present in robot library");
				}

			}	

			/**	for (BehaviourControl control : controls) {
			while (control.isAlive()) {
				Thread.sleep(100);
			}
		} */
		}
		catch (Exception e) {
			log.warning("cannot execute behaviour: " + e.toString());
		}
	}





	private final class BehaviourControl extends Thread {

		String behaviour;
		com.aldebaran.qimessaging.Object proxy;
		DialogueState state;

		public BehaviourControl (String behaviour, com.aldebaran.qimessaging.Object proxy, DialogueState state) {
			this.behaviour = behaviour;
			this.proxy = proxy;
			this.state = state;
		}

		@Override
		public void run() {
			try {
				String id = "behaviour" + (new Random()).nextInt()/100000;
				log.debug("starting behaviour " + behaviour + " (and locking ASR)");
	
			//	ASRLock.addLock(id);
				behaviour = behaviour.toLowerCase().replace("(", "-").replace(",", "_").replace(")", "");
				
				proxy.call("runBehavior", behaviour);
	
				while (!proxy.<Boolean>call("isBehaviorRunning", behaviour).get() || !state.isStable()) {
					Thread.sleep(50);
				}
				if (!state.getNetwork().hasChanceNode("motion")) {
					ChanceNode motionNode = new ChanceNode("motion");
					motionNode.addProb(ValueFactory.create(true), 1.0);
					state.getNetwork().addNode(motionNode);
				}
				
				while (proxy.<Boolean>call("isBehaviorRunning", behaviour).get() || ! state.isStable()) {
					Thread.sleep(50);
				}
				state.getNetwork().removeNode("motion");
				log.debug("behaviour " + behaviour + " + finished -- motion: " + state.getNetwork().hasNode("motion"));

				if (behaviour.contains("pickup")) {
					String obj = behaviour.split("-")[1];
					boolean isCarried = CarriedObjectDetection.detectCarriedObject();
					if (isCarried) {
						updateCarriedVariable(state, 1.0, obj);
						state.addContent(new Assignment("u_m", "Object successfully grasped"), "naoBehaviour");
					}
				}
				else if (behaviour.contains("release")) {
					updateCarriedVariable(state, 1.0, "");
					state.addContent(new Assignment("u_m", "Object successfully released"), "naoBehaviour");
				}
				
			//	ASRLock.removeLock(id);
				

			}
			catch (Exception e) {
				log.info("Exception: " + e.toString());
			}
		}
	}



	private void updateCarriedVariable(DialogueState state, double carriedProb, String obj) {
		try {
		SimpleTable table = new SimpleTable();
		table.addRow(new Assignment("carried", ValueFactory.create("["+obj+"]")), carriedProb);
		table.addRow(new Assignment("carried", ValueFactory.create("[]")), 1- carriedProb);
		state.addContent(table, "carriedObjDetection");
		if (!obj.equals("")) {
			NaoSession.grabSession().getService("ALMemory").call("insertData", "carryObj", true); 
		}
		else {
			NaoSession.grabSession().getService("ALMemory").call("insertData", "carryObj", false); 
		}
		}
		catch (Exception e) {
			log.warning("could not update carried variable: " + e);
		}
	}


	@Override
	public void shutdown() {
		executeBehaviour(Arrays.asList("kneel"), new DialogueState());
		try {
		manager.call("stopAllBehaviors");
		}
		catch (CallError e) {
			log.debug("error " + e);
		}
	}


}
