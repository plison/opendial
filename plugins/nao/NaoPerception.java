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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.SetVal;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.state.DialogueState;


public class NaoPerception implements Module, NaoEventListener {

	public static Logger log = new Logger("NaoPerception", Logger.Level.DEBUG);


	DialogueSystem system;
	NaoSession session;
	boolean paused = true;

	public NaoPerception(DialogueSystem system) throws DialException {
		this.system = system;
		session = NaoSession.grabSession(system.getSettings());
	}

	@Override
	public void start() {
		try {
			paused = false;
			session.call("ALVideoDevice", "setActiveCamera", 1);
			session.call("ALLandMarkDetection", "subscribe", "naoPerception", 200, 0);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {  
					System.out.println("Shutting down Nao perception");
					try {  
						session.call("ALLandMarkDetection", "unsubscribe", "naoPerception");
					}	catch (Exception e) {}
			}));
			session.listenToEvent("LandmarkDetected", this);

		}			
		catch (Exception e) {
			log.warning("could not initiate Nao perception: " + e);
		}
	}

	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void callback(Object event) {
		try {
			Set<String> detected = new HashSet<String>();
			if (event instanceof ArrayList && ((ArrayList)event).size()>0) {
				ArrayList<ArrayList> landmarkList = (ArrayList) ((ArrayList)event).get(1);
				for (ArrayList<ArrayList<Integer>> landmark: landmarkList) {
					int id = landmark.get(1).get(0);
					if (id == 84) {
						detected.add("RedObj");
						session.call("ALMemory", "insertData", "Position(RedObj)", landmark);
					}
					else if (id == 107 || id == 127) {
						detected.add("BlueObj");
						session.call("ALMemory", "insertData", "Position(BlueObj)", landmark);
					}
				}

			}	
			updateState(detected);
		} 	
		catch (DialException e) {
			e.printStackTrace();
		}
	}

	public void updateState(Set<String> perceivedObjects) throws DialException {

		CategoricalTable oldPerception = new CategoricalTable("perceived", ValueFactory.none());
		if (system.getState().hasChanceNode("perceived")) {
			oldPerception = system.getContent("perceived").toDiscrete();
		}

		SetVal newVal = (SetVal)ValueFactory.create(perceivedObjects.toString());
		final Assignment assign = new Assignment("perceived", newVal);

		if (!oldPerception.getBest().equals(assign)) {
			log.debug("previous perception: " + oldPerception + " vs " + assign);

			if (!system.isPaused()) {
				Runnable runnable = () -> {
						try {
							system.addContent(assign);
						}
						catch (DialException e) {
							log.warning("could not add content: " + e);
						}
				};
				(new Thread(runnable)).start();	
			}
			else {
				system.getState().addToState(assign);
				system.getState().reduce();
				if (system.getModule(GUIFrame.class)!=null) {
					system.getModule(GUIFrame.class).trigger(system.getState(), Arrays.asList("perceived"));
				}
			}


		}
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		 }



}

