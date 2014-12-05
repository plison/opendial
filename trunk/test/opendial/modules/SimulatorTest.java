// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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


import static org.junit.Assert.*;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.densityfunctions.DensityFunction;
import opendial.domains.Domain;
import opendial.modules.core.DialogueRecorder;
import opendial.modules.simulation.Simulator;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

public class SimulatorTest {

	// logger
	public static Logger log = new Logger("SimulatorTest", Logger.Level.DEBUG);

	public static String mainDomain = "test//domains//domain-demo.xml";
	public static String simDomain = "test//domains//domain-simulator.xml";

	public static String mainDomain2 = "test//domains//example-domain-params.xml";
	public static String simDomain2 = "test//domains//example-simulator.xml";

	@Test
	public void testSimulator() throws DialException, InterruptedException {

		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain));
		system.getDomain().getModels().remove(0);
		system.getDomain().getModels().remove(0);
		system.getDomain().getModels().remove(0);
		Domain simDomain2 = XMLDomainReader.extractDomain(simDomain);
		Simulator sim = new Simulator(system, simDomain2);
		int nbSamples = Settings.nbSamples;
		Settings.nbSamples = nbSamples / 10;
		system.attachModule(sim);
		system.getSettings().showGUI = false;

		system.startSystem();

		String str = "";
		for (int i = 0 ; i < 30 && ! system.isPaused(); i++) {
			Thread.sleep(500);
			str = system.getModule(DialogueRecorder.class).getRecord();
			try {
				checkCondition(str);
				system.pause(true);
			}
			catch (AssertionError e) {	}
		}

		checkCondition(str);
		system.pause(true);

		Settings.nbSamples = nbSamples * 10 ;
	}

	@Test
	public void testRewardLearner() throws DialException, InterruptedException {
		DialogueSystem system = null;
		outloop: for (int k = 0 ; k < 2 ; k++) {
			system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain2));

			Domain simDomain3 = XMLDomainReader.extractDomain(simDomain2);
			Simulator sim = new Simulator(system, simDomain3);
			system.attachModule(sim);
			system.getSettings().showGUI = false;
			Settings.nbSamples = Settings.nbSamples * 2 ;
			system.startSystem();

			for (int i = 0 ; i < 20 && !system.isPaused() ; i++) {
				Thread.sleep(500);
				try {
					checkCondition2(system);
					system.pause(true);
					break outloop;
				}
				catch (AssertionError e) { 	}
			}

		}
		checkCondition2(system);
		system.pause(true);
		DensityFunction theta_correct, theta_incorrect, theta_repeat;
		theta_correct = system.getContent("theta_correct").toContinuous().getFunction();
		theta_incorrect = system.getContent("theta_incorrect").toContinuous().getFunction();
		theta_repeat = system.getContent("theta_repeat").toContinuous().getFunction();
		log.debug("theta_correct " + theta_correct);
		log.debug("theta_incorrect " + theta_incorrect);
		log.debug("theta_repeat " + theta_repeat);
		Settings.nbSamples = Settings.nbSamples  / 2;
	}

	private static void checkCondition(String str) {
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
		assertTrue(str.contains("YouSee")); 
		assertTrue(str.contains("Reward: 10"));
		assertTrue(str.contains("Do(Pick"));
	}

	private static void checkCondition2(DialogueSystem system) throws DialException {
		DensityFunction theta_correct, theta_incorrect, theta_repeat;
		theta_correct = system.getContent("theta_correct").toContinuous().getFunction();
		theta_incorrect = system.getContent("theta_incorrect").toContinuous().getFunction();
		theta_repeat = system.getContent("theta_repeat").toContinuous().getFunction();
		assertEquals(2.0, theta_correct.getMean()[0], 0.5);
		assertEquals(-2.0, theta_incorrect.getMean()[0], 1.5);
		assertEquals(0.5, theta_repeat.getMean()[0], 0.7);
	}


}

