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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import opendial.DialogueSystem;
import opendial.Settings;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.densityfunctions.UniformDensityFunction;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLDialogueReader;
import opendial.utils.StringUtils;
import opendial.utils.XMLUtils;

import org.junit.Test;

public class RecordingTest {

	public static final String domainFile = "test//domains//domain-demo.xml";
	public static final String domainFile2 = "test//domains//domain-woz.xml";
	public static final String importState =
			"test//domains//domain-demo-importstate.xml";
	public static final String importParams =
			"test//domains//domain-demo-importparams.xml";
	public static final String exportState =
			"test//domains//domain-demo-exportstate.xml";
	public static final String exportParams =
			"test//domains//domain-demo-exportparams.xml";
	public static String dialogueFile = "test/domains/woz-dialogue.xml";

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void testRecord() throws InterruptedException {
		if (!GraphicsEnvironment.isHeadless()) {
			DialogueSystem system =
					new DialogueSystem(XMLDomainReader.extractDomain(domainFile));

			system.getSettings().showGUI = true;
			system.startSystem();

			CategoricalTable.Builder builder = new CategoricalTable.Builder("u_u");
			builder.addRow("move left", 0.3);
			builder.addRow("move a bit to the left", 0.05);
			system.addContent(builder.build());
			builder = new CategoricalTable.Builder("u_u");
			builder.addRow("no", 0.5);
			system.addContent(builder.build());
			system.pause(true);
			builder.addRow("now you should not hear anything", 0.8);
			system.pause(false);
			builder = new CategoricalTable.Builder("u_u");
			builder.addRow("move left", 0.2);
			builder.addRow("move a bit to the left", 0.65);
			system.addContent(builder.build());

			assertTrue(
					system.getModule(GUIFrame.class).getChatTab().getChat().contains(
							"<font size=\"4\">move a bit to the left (0.05)</font>"));
			assertTrue(
					system.getModule(GUIFrame.class).getChatTab().getChat().contains(
							"<font size=\"4\">OK, moving Left a little bit</font>"));
			assertEquals(6,
					StringUtils.countOccurrences(
							system.getModule(DialogueRecorder.class).getRecord(),
							"userTurn"));
			if (StringUtils.countOccurrences(
					system.getModule(DialogueRecorder.class).getRecord(),
					"systemTurn") != 4) {
				Thread.sleep(250);
			}
			assertEquals(4,
					StringUtils.countOccurrences(
							system.getModule(DialogueRecorder.class).getRecord(),
							"systemTurn"));
			assertEquals(14,
					StringUtils.countOccurrences(
							system.getModule(DialogueRecorder.class).getRecord(),
							"variable"));

			system.getModule(GUIFrame.class).getFrame().dispose();
		}
	}

	@Test
	public void testXML() throws InterruptedException, IOException {

		DialogueSystem system =
				new DialogueSystem(XMLDomainReader.extractDomain(domainFile2));
		system.getSettings().showGUI = false;
		system.startSystem();

		XMLUtils.importContent(system, importState, "state");
		assertEquals(12, system.getState().getChanceNodeIds().size());
		assertEquals(0.7, system.getContent("aha").getProb("ohoho"), 0.01);

		XMLUtils.importContent(system, importParams, "parameters");
		assertEquals(14, system.getState().getChanceNodeIds().size());
		assertTrue(system.getContent("theta_2").toContinuous()
				.getFunction() instanceof UniformDensityFunction);

		Settings.nbSamples = Settings.nbSamples / 100;
		DialogueImporter importer = new DialogueImporter(system,
				XMLDialogueReader.extractDialogue(dialogueFile));
		importer.setWizardOfOzMode(true);
		system.startSystem();
		importer.start();
		while (importer.isAlive()) {
			Thread.sleep(50);
		}
		Settings.nbSamples = Settings.nbSamples * 100;
		XMLUtils.exportContent(system, exportState, "state");
		String str = "";
		BufferedReader br = new BufferedReader(new FileReader(exportState));
		String line = "";
		while ((line = br.readLine()) != null) {
			str += line;
		}
		br.close();
		Thread.sleep(100);
		assertEquals(26, StringUtils.countOccurrences(str, "variable"));
		XMLUtils.exportContent(system, exportParams, "parameters");
		str = "";
		br = new BufferedReader(new FileReader(exportParams));
		line = "";
		while ((line = br.readLine()) != null) {
			str += line;
		}
		br.close();
		assertEquals(14, StringUtils.countOccurrences(str, "variable"));
		assertEquals(5, StringUtils.countOccurrences(str, "gaussian"));

	}

}
