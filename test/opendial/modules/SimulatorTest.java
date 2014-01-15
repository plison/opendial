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

	public static String mainDomain = "domains//demo/domain.xml";
	public static String simDomain = "test//domains//domain-simulator.xml";
	
	@Test
	public void simulatorTest() throws DialException, InterruptedException {
		
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(mainDomain));
		system.getSettings().showGUI = false;
		Domain simDomain2 = XMLDomainReader.extractDomain(simDomain);
		Simulator sim = new Simulator(simDomain2);
		int nbSamples = Settings.nbSamples;
		Settings.nbSamples = nbSamples / 10;
		sim.getSettings().showGUI = false;
		system.attachModule(sim, false);
		system.startSystem();
		
		String str = "";
		for (int i = 0 ; i < 100 ; i++) {
			Thread.sleep(1000);
			str = system.getModule(DialogueRecorder.class).getRecord();
			if (checkCondition(str)) {
				return;
			}
		}
		assertTrue(str.contains("AskRepeat"));
		assertTrue(str.contains("Do(Move"));
		assertTrue(str.contains("YouSee"));
		assertTrue(str.contains("Reward: 10.0"));
	//	assertTrue(str.contains("Reward: -1.0"));
	//	assertTrue(str.contains("Describe"));
	//	assertTrue(str.contains("Do(Pick"));
		
		Settings.nbSamples = nbSamples * 10 ;

	}
	
	private static boolean checkCondition(String str) {
		return str.contains("AskRepeat") && str.contains("Do(Move") 
				&& str.contains("YouSee") 
				&& str.contains("Reward: 10.0")
		//		&& str.contains("Reward: -1.0")
		//		&& str.contains("Describe") && str.contains("Do(Pick"
			;
	}
	
	
}

