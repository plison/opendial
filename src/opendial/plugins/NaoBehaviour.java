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

import java.util.logging.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALAutonomousLife;
import com.aldebaran.qi.helper.proxies.ALBehaviorManager;
import com.aldebaran.qi.helper.proxies.ALMotion;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class NaoBehaviour implements Module {

	final static Logger log = Logger.getLogger("OpenDial");

	public static final String ACTION_VAR = "a_m";
	public static final String APP_NAME = "lenny";

	DialogueSystem system;
	Session session;
	ALBehaviorManager bmanager;
	boolean paused = true;

	public NaoBehaviour(DialogueSystem system) {
		this.system = system;
		session = NaoUtils.grabSession(system.getSettings());
	}

	@Override
	public void start() {
		paused = false;
		try {
			(new ALAutonomousLife(session)).setState("disabled");
			(new ALMotion(session)).setExternalCollisionProtectionEnabled("All", false);
			bmanager = new ALBehaviorManager(session);
			executeBehaviour("standup");
		}
		catch (Exception e) {
			log.warning("Cannot start NaoBehaviour: " + e.toString());
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutting down Nao Behaviour");
			try {
				executeBehaviour("kneel");
				bmanager.stopAllBehaviors();
			}
			catch (Exception e) {
			}
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

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (session != null && updatedVars.contains(ACTION_VAR) && !paused) {
			String behaviourName = getBehaviourName(state);
			if (behaviourName != null) {
				executeBehaviour(behaviourName);
			}
		}
	}

	private String getBehaviourName(DialogueState state) {
		if (state.hasChanceNode(ACTION_VAR)) {
			String fullVal =
					state.queryProb(ACTION_VAR).toDiscrete().getBest().toString();
			Matcher m = Pattern.compile("Do\\((.*)\\)").matcher(fullVal);
			if (m.find() && m.group(1).equals("Stop")) {
				return "stop";
			}
			else if (m.find()) {
				return "generic";
			}
			else if (fullVal.contains("Goodbye")) {
				return "kneel";
			}
		}
		return null;
	}

	private void executeBehaviour(String... parallelBehaviours) {
		try {

			for (String behaviour : parallelBehaviours) {

				String fullName = APP_NAME + "/" + behaviour;
				if (bmanager.isBehaviorInstalled(fullName)) {
					BehaviourControl control =new BehaviourControl(fullName);
					control.start();
				}
				else if (behaviour.equals("stop")) {
					log.info("stopping all behaviours!");
					bmanager.stopAllBehaviors();
				}
				else {
					log.info("behaviour " + behaviour + " not present in robot library");
				}
			}
		}
		catch (Exception e) {
			log.warning("cannot execute behaviour: " + e.toString());
		}
	}

	private final class BehaviourControl extends Thread {

		String behaviour;

		public BehaviourControl(String behaviour) {
			this.behaviour = behaviour;
		}

		@Override
		public void run() {
			
			try {
				log.fine("starting behaviour " + behaviour);

	//			NaoASR asr = system.getModule(NaoASR.class);
	//			if (asr != null) asr.lockASR("NaoBehaviour");

				system.addContent("motion", true);

				bmanager.runBehavior(behaviour);

	//			if (asr != null) asr.unlockASR("NaoBehaviour");


				log.fine("behaviour " + behaviour + " successfully completed");
				system.addContent("motion",ValueFactory.none());

			}
			catch (Exception e) {
				log.info("Exception: " + e.toString());
			}
		}
	}

}
