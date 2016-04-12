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

import com.aldebaran.qi.AnyObject;
import com.aldebaran.qi.CallError;
import com.aldebaran.qi.Session;
import com.aldebaran.qi.helper.proxies.ALMemory;
import com.aldebaran.qi.helper.proxies.ALMotion;
import com.aldebaran.qi.helper.proxies.ALVideoDevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.SingleValueDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.modules.Module;

public class NaoPerception implements Module {

	final static Logger log = Logger.getLogger("OpenDial");

	public static final double INIT_PROB = 0.7;

	long lastUpdate = -1000;

	DialogueSystem system;
	Session session;
	boolean paused = true;

	public NaoPerception(DialogueSystem system) {
		this.system = system;
		session = NaoUtils.grabSession(system.getSettings());
	}

	@Override
	public void start() {
		try {
			paused = false;
			(new ALVideoDevice(session)).setActiveCamera(1);
			AnyObject detector = session.service("ALLandMarkDetection");
			detector.call("subscribe", "naoPerception", 500, 0);

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("Shutting down Nao perception");
				try {
					detector.call("unsubscribe", "naoPerception");
				}
				catch (Exception e) {
				}
			}));
			ALMemory memory = new ALMemory(session);
			memory.subscribeToEvent("LandmarkDetected", r -> processEvent(r));
			(new Thread(new DetectionLoop())).start();

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
	public void processEvent(Object event) throws CallError, InterruptedException {
		try {
			List<PerceivedObject> curObjects = new ArrayList<PerceivedObject>();
			if (event instanceof ArrayList && ((ArrayList) event).size() > 0) {
				ArrayList<ArrayList> markInfos =
						(ArrayList) ((ArrayList) event).get(1);
				for (ArrayList<ArrayList<?>> markInfo : markInfos) {
					try {
						PerceivedObject pobj = new PerceivedObject(markInfo);
						curObjects.add(pobj);
					}
					catch (Exception e) {
						log.warning("Cannot process perceived object: " + e.toString());
					}
				}
			}
			if (!system.isPaused()) {
				(new Thread(new PerceptionUpdate(curObjects))).start();			
			}
		}
		catch (RuntimeException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
	}


	private class PerceivedObject {

		String identifier;
		String colour = "";
		String type = "";
		double xPos;
		double yPos;
		double zPos;

		PerceivedObject(ArrayList<ArrayList<?>> markInfo) throws Exception {

			int id = (Integer)markInfo.get(1).get(0);
			this.identifier = "" + id;

			if (id == 84) {
				this.colour = "red";
			}
			else if (id == 107 || id == 127) {
				this.colour = "blue";		
			}
			this.type = "cylinder";

			// Retrieve landmark center position in radians.
			double wzCamera = (Float)markInfo.get(0).get(1);
			double wyCamera = (Float)markInfo.get(0).get(2);

			// Retrieve landmark angular size in radians.
			double angularSize = (Float)markInfo.get(0).get(3);

			double landmarkTheoreticalSize = 0.092;

			String currentCamera = "CameraBottom";

			// Compute distance to landmark.
			double distanceFromCameraToLandmark = landmarkTheoreticalSize / ( 2 * Math.tan( angularSize / 2));

			ALMotion motion = new ALMotion(session);

			// Get current camera position in NAO space.
			Transform robotToCamera = new Transform(motion.getTransform(currentCamera, 2, true));
			Transform cameraToLandmarkRotationTransform = (new Transform()).rotate(0, wyCamera, wzCamera);
			Transform cameraToLandmarkTranslationTransform = new Transform(distanceFromCameraToLandmark, 0, 0);
			Transform robotToLandmark = robotToCamera;
			robotToLandmark.multiply(cameraToLandmarkRotationTransform);
			robotToLandmark.multiply(cameraToLandmarkTranslationTransform);

			this.xPos = robotToLandmark.t[0][3] - 0.2;
			this.yPos = robotToLandmark.t[1][3];
			this.zPos = Math.atan(yPos/xPos);

		}


		@Override
		public String toString() {
			return "[" + identifier + " colour>" + this.colour + " type>" + this.type
					+ " xpos>" + xPos + " ypos>"+yPos + " zpos>"+zPos + "]";
		}

	}



	private static class Transform {

		enum Axis {X, Y, Z};

		double[][] t = new double[][] {
			new double[] {1.0, 0.0, 0.0, 0.0}, 
			new double[] {0.0, 1.0, 0.0, 0.0},
			new double[] {0.0, 0.0, 1.0, 0.0}};

			Transform() {		}

			Transform(List<Float> floats) {
				if (floats.size() == 16) {
					for (int i = 0 ; i < 3 ; i++) {
						for (int j = 0 ; j < 4 ; j++) {
							t[i][j] = floats.get(i*3 + j);
						}
					}
				}
				else {
					log.warning("illegal number of floats: " + floats.size());
				}
			}

			Transform(double x, double y, double z) {
				this();
				t[0][3] = x;
				t[1][3] = y;
				t[2][3] = z;
			}

			void multiply(Transform other) {	
				double c1 = t[0][0];	double c2 = t[0][1];	double c3 = t[0][2];
				t[0][0] = (c1 * other.t[0][0]) + (c2 * other.t[1][0]) + (c3 * other.t[2][0]);
				t[0][1] = (c1 * other.t[0][1]) + (c2 * other.t[1][1]) + (c3 * other.t[2][1]);
				t[0][2] = (c1 * other.t[0][2]) + (c2 * other.t[1][2]) + (c3 * other.t[2][2]);
				t[0][3] = (c1 * other.t[0][3]) + (c2 * other.t[1][3]) + (c3 * other.t[2][3]) + t[0][3];
				c1 = t[1][0];	c2 = t[1][1];	c3 = t[1][2];
				t[1][0] = (c1 * other.t[0][0]) + (c2 * other.t[1][0]) + (c3 * other.t[2][0]);
				t[1][1] = (c1 * other.t[0][1]) + (c2 * other.t[1][1]) + (c3 * other.t[2][1]);
				t[1][2] = (c1 * other.t[0][2]) + (c2 * other.t[1][2]) + (c3 * other.t[2][2]);
				t[1][3] = (c1 * other.t[0][3]) + (c2 * other.t[1][3]) + (c3 * other.t[2][3]) + t[1][3];
				c1 = t[2][0];	c2 = t[2][1];	c3 = t[2][2];
				t[2][0] = (c1 * other.t[0][0]) + (c2 * other.t[1][0]) + (c3 * other.t[2][0]);
				t[2][1] = (c1 * other.t[0][1]) + (c2 * other.t[1][1]) + (c3 * other.t[2][1]);
				t[2][2] = (c1 * other.t[0][2]) + (c2 * other.t[1][2]) + (c3 * other.t[2][2]);
				t[2][3] = (c1 * other.t[0][3]) + (c2 * other.t[1][3]) + (c3 * other.t[2][3]) + t[2][3];
			}

			Transform rotate(double Wx, double Wy, double Wz) {
				Transform T = rotate(Wz, Axis.Z);
				T.multiply(rotate(Wy,Axis.Y));
				T.multiply(rotate(Wx,Axis.X));
				return T;
			}

			Transform rotate(double w, Axis a) {
				double c = Math.cos(w);
				double s = Math.sin(w);
				Transform T = new Transform();
				if (a==Axis.X) {
					T.t[1][1] = c;
					T.t[1][2] = -s;
					T.t[2][1] = s;
					T.t[2][2] = c;
				}
				else if (a==Axis.Y) {
					T.t[0][0] = c;
					T.t[0][2] = s;
					T.t[2][0] = -s;
					T.t[2][2] = c;
				}
				else if (a==Axis.Z) {
					T.t[0][0] = c;
					T.t[0][1] = -s;
					T.t[1][0] = s;
					T.t[1][1] = c;
				}
				return T;
			}
	}


	class DetectionLoop implements Runnable {

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				if ((System.currentTimeMillis() - lastUpdate) >=1000) {

					(new Thread(new PerceptionUpdate())).start();			
				}
			}
		}
	}

	class PerceptionUpdate implements Runnable {

		List<PerceivedObject> newObjects;

		public PerceptionUpdate() {
			newObjects = Collections.emptyList();
		}

		public PerceptionUpdate(List<PerceivedObject> newObjects) {
			this.newObjects = newObjects;
		}


		@Override
		public void run() {
			if (paused) {
				return;
			}
			else if (!system.getState().getNewVariables().isEmpty()) {
				log.info("previous update is not finished, aborting update");
				return;
			}

			BNetwork newData = new BNetwork();
			Set<String> detectedObjs = new HashSet<String>();

			DialogueState curState = system.getState();

			synchronized (curState) {
				for (PerceivedObject pobj : newObjects) {

					String nodeId = "obj_" + pobj.identifier;
					detectedObjs.add(nodeId);

					double newProb = INIT_PROB;
					if (curState.hasChanceNode(nodeId) && !curState.hasChanceNode("motion")) {
						double oldProb = 1-system.getContent(nodeId).getProb(ValueFactory.none());
						newProb += (1-newProb)*oldProb;
						if ((newProb - oldProb) < 0.05) {
							continue;
						}
					}
					Value nodeValue = ValueFactory.create(pobj.toString());
					CategoricalTable.Builder builder =new CategoricalTable.Builder(nodeId);
					builder.addRow(nodeValue, newProb);
					newData.addNode(new ChanceNode(nodeId, builder.build()));
				}

				for (String curVar : curState.getChanceNodeIds()) {
					if (curVar.startsWith("obj_") && !detectedObjs.contains(curVar)) {
						SingleValueDistribution svd =  new SingleValueDistribution(curVar, ValueFactory.none());
						newData.addNode(new ChanceNode(curVar, svd));				
					}
				}
				if (!newData.getChanceNodeIds().isEmpty()) {
					system.addContent(newData);
				}
			}
			lastUpdate = System.currentTimeMillis();
		}
	}


}
