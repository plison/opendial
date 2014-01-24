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


import static org.junit.Assert.*;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
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
		
		system.startSystem();
		Thread.sleep(5000);
		system.pause(true);

		log.debug("theta_correct " + system.getContent("theta_correct"));
		log.debug("theta_incorrect " + system.getContent("theta_incorrect"));
		log.debug("theta_repeat " + system.getContent("theta_repeat"));
		assertEquals(2.0, system.getContent("theta_correct").toContinuous().getFunction().getMean()[0], 0.6);
		assertEquals(-2.0, system.getContent("theta_incorrect").toContinuous().getFunction().getMean()[0], 0.6);
		assertEquals(0.5, system.getContent("theta_repeat").toContinuous().getFunction().getMean()[0], 0.3);
		
		
	}
	
	private static void checkCondition(String str) {
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
				assertTrue(str.contains("YouSee")); 
						assertTrue(str.contains("Reward: 10.0"));
								assertTrue(str.contains("Do(Pick"));
								
			
	}
	
	
}

