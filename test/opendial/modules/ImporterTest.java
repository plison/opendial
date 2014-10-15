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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.ContinuousDistribution;
import opendial.modules.core.DialogueImporter;
import opendial.modules.core.DialogueRecorder;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.utils.StringUtils;

import org.junit.Test;

public class ImporterTest {

	// logger
	public static Logger log = new Logger("ImporterTest", Logger.Level.DEBUG);
	
	public static String domainFile = "test/domains/domain-woz.xml";
	public static String dialogueFile = "test/domains/woz-dialogue.xml";
	
	@Test
	public void testImporter() throws DialException, InterruptedException {
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
		assertTrue(((ContinuousDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).getFunction().getMean()[0] > 12.0);
		Settings.nbSamples = Settings.nbSamples * 5;

	}

}

