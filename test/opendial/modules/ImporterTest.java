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
import opendial.bn.distribs.ContinuousDistribution;
import opendial.readers.XMLDomainReader;
import opendial.utils.StringUtils;

import org.junit.Test;

public class ImporterTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	public static String domainFile = "test/domains/domain-woz.xml";
	public static String dialogueFile = "test/domains/woz-dialogue.xml";
	public static String domainFile2 = "test/domains/example-domain-params.xml";
	public static String dialogueFile2 = "test/domains/dialogue.xml";

	@Test
	public void testImporter() throws InterruptedException {
		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = false;
		Settings.nbSamples = Settings.nbSamples / 10;
		DialogueImporter importer = system.importDialogue(dialogueFile);
		system.startSystem();
		while (importer.isAlive()) {
			Thread.sleep(250);
		}
		assertEquals(20, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(22, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		Settings.nbSamples = Settings.nbSamples * 10;

	}

	@Test
	public void testImporter2() throws InterruptedException {
		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = false;
		Settings.nbSamples = Settings.nbSamples / 5;
		system.startSystem();
		DialogueImporter importer = system.importDialogue(dialogueFile);
		importer.setWizardOfOzMode(true);
		while (importer.isAlive()) {
			Thread.sleep(300);
		}
		assertEquals(20, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(22, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		assertTrue(
				((ContinuousDistribution) system.getState().getChanceNode("theta_1")
						.getDistrib()).getFunction().getMean()[0] > 12.0);
		Settings.nbSamples = Settings.nbSamples * 5;
	}

	@Test
	public void testImporter3() throws InterruptedException {
		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile2));
		system.getSettings().showGUI = false;
		system.startSystem();
		DialogueImporter importer = system.importDialogue(dialogueFile2);
		while (importer.isAlive()) {
			Thread.sleep(300);
		}
		assertEquals(10, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(10, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		assertEquals(((ContinuousDistribution) system.getState()
				.getChanceNode("theta_repeat").getDistrib()).getFunction()
						.getMean()[0],
				0.0, 0.2);
	}

	@Test
	public void testImporter4() throws InterruptedException {
		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile2));
		Settings.nbSamples = Settings.nbSamples * 3;
		Settings.maxSamplingTime = Settings.maxSamplingTime * 3;
		system.getSettings().showGUI = false;
		system.startSystem();
		DialogueImporter importer = system.importDialogue(dialogueFile2);
		importer.setWizardOfOzMode(true);
		while (importer.isAlive()) {
			Thread.sleep(250);
		}
		assertEquals(10, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(10, StringUtils.countOccurrences(
				system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		assertEquals(((ContinuousDistribution) system.getState()
				.getChanceNode("theta_repeat").getDistrib()).getFunction()
						.getMean()[0],
				1.35, 0.3);
		Settings.nbSamples = Settings.nbSamples / 3;
		Settings.maxSamplingTime = Settings.maxSamplingTime / 3;
	}

}
