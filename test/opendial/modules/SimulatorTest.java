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

package opendial.modules;

import java.util.logging.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.Settings;
import opendial.bn.distribs.densityfunctions.DensityFunction;
import opendial.domains.Domain;
import opendial.modules.simulation.Simulator;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class SimulatorTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static String mainDomain = "test//domains//domain-demo.xml";
	public static String simDomain = "test//domains//domain-simulator.xml";

	public static String mainDomain2 = "test//domains//example-domain-params.xml";
	public static String simDomain2 = "test//domains//example-simulator.xml";

	@Test
	public void testSimulator() throws InterruptedException {

		DialogueSystem system = null;
		int nbSamples = Settings.nbSamples;
		log.setLevel(Level.WARNING);
		Settings.nbSamples = nbSamples / 5;
		outloop: for (int k = 0; k < 3; k++) {
			system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain));
			if (k > 0) {
				log.warning("restarting the simulator...");
			}

			system.getDomain().getModels().remove(0);
			system.getDomain().getModels().remove(0);
			system.getDomain().getModels().remove(0);
			Domain simDomain2 = XMLDomainReader.extractDomain(simDomain);
			Simulator sim = new Simulator(system, simDomain2);
			system.attachModule(sim);
			system.getSettings().showGUI = false;

			system.startSystem();

			String str = "";
			for (int i = 0; i < 40
					&& system.getModule(Simulator.class) != null; i++) {
				Thread.sleep(200);
				str = system.getModule(DialogueRecorder.class).getRecord();
				try {
					checkCondition(str);
					system.detachModule(Simulator.class);
					break outloop;
				}
				catch (AssertionError e) {
				}
			}
			system.detachModule(Simulator.class);
		}
		checkCondition(system.getModule(DialogueRecorder.class).getRecord());
		system.detachModule(Simulator.class);
		system.pause(true);
		Settings.nbSamples = nbSamples * 5;
		log.setLevel(Level.INFO);
	}

	@Test
	public void testRewardLearner() throws InterruptedException {
		DialogueSystem system = null;
		Settings.nbSamples = Settings.nbSamples * 2;
		log.setLevel(Level.WARNING);
		outloop: for (int k = 0; k < 3; k++) {
			if (k > 0) {
				log.info("restarting the learner...");
			}
			system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain2));

			Domain simDomain3 = XMLDomainReader.extractDomain(simDomain2);
			Simulator sim = new Simulator(system, simDomain3);
			system.attachModule(sim);
			system.getSettings().showGUI = false;
			system.startSystem();

			for (int i = 0; i < 20
					&& system.getModule(Simulator.class) != null; i++) {
				Thread.sleep(100);
				try {
					checkCondition2(system);
					system.detachModule(Simulator.class);
					break outloop;
				}
				catch (AssertionError e) {

				}
			}
			system.detachModule(Simulator.class);
		}
		checkCondition2(system);
		system.detachModule(Simulator.class);
		system.pause(true);
		DensityFunction theta_correct, theta_incorrect, theta_repeat;
		theta_correct =
				system.getContent("theta_correct").toContinuous().getFunction();
		theta_incorrect =
				system.getContent("theta_incorrect").toContinuous().getFunction();
		theta_repeat =
				system.getContent("theta_repeat").toContinuous().getFunction();
		log.fine("theta_correct " + theta_correct);
		log.fine("theta_incorrect " + theta_incorrect);
		log.fine("theta_repeat " + theta_repeat);
		Settings.nbSamples = Settings.nbSamples / 2;
		log.setLevel(Level.INFO);
	}

	private static void checkCondition(String str) {
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
		assertTrue(str.contains("YouSee"));
		assertTrue(str.contains("Reward: 10"));
		assertTrue(str.contains("Do(Pick"));
	}

	private static void checkCondition2(DialogueSystem system) {
		DensityFunction theta_correct, theta_incorrect, theta_repeat;
		theta_correct =
				system.getContent("theta_correct").toContinuous().getFunction();
		theta_incorrect =
				system.getContent("theta_incorrect").toContinuous().getFunction();
		theta_repeat =
				system.getContent("theta_repeat").toContinuous().getFunction();
		assertEquals(2.0, theta_correct.getMean()[0], 0.7);
		assertEquals(-2.0, theta_incorrect.getMean()[0], 1.5);
		assertEquals(0.5, theta_repeat.getMean()[0], 0.7);
	}

}
