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

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

public class SimulatorTest {

	// logger
	public static Logger log = new Logger("SimulatorTest", Logger.Level.DEBUG);

	public static String mainDomain = "test//domains//domain-demo.xml";
	public static String simDomain = "test//domains//domain-simulator.xml";
	
	@Test
	public void simulatorTest() throws DialException, InterruptedException {
		
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
		for (int i = 0 ; i < 100 && ! system.isPaused(); i++) {
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
	
	private static void checkCondition(String str) {
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
				assertTrue(str.contains("YouSee")); 
						assertTrue(str.contains("Reward: 10.0"));
								assertTrue(str.contains("Do(Pick"));
								
			
	}
	
	
}

