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
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.domains.Domain;
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
		for (int i = 0 ; i < 20 && ! system.isPaused(); i++) {
			Thread.sleep(1000);
			str = system.getModule(DialogueRecorder.class).getRecord();
			try {
				checkCondition(str);
				system.pause(true);
			}
			catch (AssertionError e) {		}
		}
		
		checkCondition(str);
		log.debug("full interaction: " + str);
		system.pause(true);
		
		Settings.nbSamples = nbSamples * 10 ;
	}
	
	@Test
	public void testRewardLearner() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain2));
		Domain simDomain3 = XMLDomainReader.extractDomain(simDomain2);
		Simulator sim = new Simulator(system, simDomain3);
		system.attachModule(sim);
		system.getSettings().showGUI = false;
		Settings.nbSamples = Settings.nbSamples * 3;
		system.startSystem();
		Thread.sleep(5000);
		system.pause(true);
		Thread.sleep(100);
		
		IndependentProbDistribution theta_correct = system.getContent("theta_correct");
		IndependentProbDistribution theta_incorrect = system.getContent("theta_incorrect");
		IndependentProbDistribution theta_repeat = system.getContent("theta_repeat");
		log.debug("theta_correct " + theta_correct);
		log.debug("theta_incorrect " + theta_incorrect);
		log.debug("theta_repeat " + theta_repeat);
		assertEquals(2.0, theta_correct.toContinuous().getFunction().getMean()[0], 0.75);
		assertEquals(-2.0, theta_incorrect.toContinuous().getFunction().getMean()[0], 0.75);
		assertEquals(0.5, theta_repeat.toContinuous().getFunction().getMean()[0], 0.3);
		
		Settings.nbSamples = Settings.nbSamples  / 3;
	}
	
	private static void checkCondition(String str) {
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
				assertTrue(str.contains("YouSee")); 
						assertTrue(str.contains("Reward: 10.0"));
								assertTrue(str.contains("Do(Pick"));
								
			
	}
	
	
}

