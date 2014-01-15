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
import opendial.bn.distribs.other.MarginalEmpiricalDistribution;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.utils.StringUtils;

public class ImporterTest {

	// logger
	public static Logger log = new Logger("ImporterTest", Logger.Level.DEBUG);
	
	public static String domainFile = "test/domains/domain-woz.xml";
	public static String dialogueFile = "test/domains/woz-dialogue.xml";
	
	@Test
	public void importerTest() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = false;
		Settings.nbSamples = Settings.nbSamples / 5;
		DialogueImporter importer = new DialogueImporter(system, 
				XMLInteractionReader.extractInteraction(dialogueFile));
		system.startSystem();
		importer.start();
		while (importer.isAlive()) {
			Thread.sleep(100);
		}
		assertEquals(20, StringUtils.countOccurrences(system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(22, StringUtils.countOccurrences(system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		assertTrue(((MarginalEmpiricalDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).toContinuous().getFunction().getMean()[0] > 12.0);
		Settings.nbSamples = Settings.nbSamples * 5;

	}

}

