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

import static org.junit.Assert.*;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import opendial.DialogueSystem;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;

import org.junit.Test;

/**
 * Testing the GUI (especially the domain editor).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class GUITest {

	final static Logger log = Logger.getLogger("OpenDial");

	final Robot robot;

	public GUITest() throws AWTException {
		if (!GraphicsEnvironment.isHeadless()) {
			robot = new Robot();
		}
		else {
			robot = null;
		}
	}

	@Test
	public void newDomainTest() throws IOException {
		if (!GraphicsEnvironment.isHeadless()) {
			DialogueSystem system = new DialogueSystem();
			system.startSystem();
			GUIFrame gui = system.getModule(GUIFrame.class);
			log.setLevel(Level.OFF);
			gui.newDomain(new File("blablaNew.xml"));
			assertEquals("blablaNew.xml",
					system.getDomain().getSourceFile().getName());
			assertEquals(0, system.getDomain().getModels().size());
			assertEquals("<domain>\n\n</domain>", gui.getEditorTab().getText());
			assertEquals("blablaNew.xml",
					gui.getEditorTab().getShownFile().getName());
			String minDom = "<domain><initialstate>"
					+ "<variable id=\"foo\"><value>bar</value></variable>"
					+ "</initialstate></domain>";
			gui.getEditorTab().setText(minDom);
			gui.saveDomain();
			assertTrue(gui.getChatTab().getChat().contains("successfully created"));
			assertTrue(gui.getChatTab().getChat().contains("successfully updated"));
			gui.resetInteraction();
			assertEquals(1, system.getState().getChanceNodes().size());
			assertEquals(minDom, gui.getEditorTab().getText());
			Path p = Paths.get(new File("blablaNew.xml").toURI());
			assertEquals(1, Files.readAllLines(p).size());
			assertEquals(94, Files.readAllLines(p).get(0).length());
			log.setLevel(null);
			Files.delete(p);
			system.getModule(GUIFrame.class).getFrame().dispose();
		}
	}

	@Test
	public void existingDomainTest() {
		if (!GraphicsEnvironment.isHeadless()) {
			DialogueSystem system = new DialogueSystem();
			system.startSystem();
			log.setLevel(Level.OFF);

			GUIFrame gui = system.getModule(GUIFrame.class);
			system.changeDomain(XMLDomainReader
					.extractDomain("test/domains/example-step-by-step_params.xml"));
			gui.refresh();
			assertTrue(gui.getEditorTab().getText().contains("<parameters>"));
			assertEquals(2, system.getState().getChanceNodes().size());
			assertEquals(1, gui.getEditorTab().getFiles().size());
			system.addUserInput("move left");
			assertEquals("Ok, moving Left",
					system.getContent("u_m").getBest().toString());
			system.changeDomain(XMLDomainReader
					.extractDomain("test/domains/example-flightbooking.xml"));
			gui.refresh();
			assertFalse(gui.getEditorTab().getText().contains("<parameters>"));
			system.addUserInput("move left");
			assertTrue(system.getContent("u_m").getBest().toString()
					.contains("Sorry, could you"));
			assertEquals(6, system.getState().getChanceNodes().size());
			assertEquals(4, gui.getEditorTab().getFiles().size());
			gui.newDomain(new File("blablaNew.xml"));
			assertEquals(1, gui.getEditorTab().getFiles().size());
			assertEquals("<domain>\n\n</domain>", gui.getEditorTab().getText());
			gui.resetInteraction();
			assertEquals(0, system.getState().getChanceNodes().size());

			log.setLevel(null);
			system.getModule(GUIFrame.class).getFrame().dispose();
		}
	}

	@Test
	public void saveDomainTest() throws IOException, InterruptedException {
		if (!GraphicsEnvironment.isHeadless()) {
			DialogueSystem system = new DialogueSystem();
			system.startSystem();
			GUIFrame gui = system.getModule(GUIFrame.class);
			log.setLevel(Level.OFF);
			gui.newDomain(new File("blablaNew.xml"));
			String minDom = "<domain><model trigger=\"u_u\">"
					+ "<rule><case><condition><if var=\"u_u\" value=\"input\"/>"
					+ "</condition><effect><set var=\"u_m\" value=\"output\"/>"
					+ "</effect></case></rule></model></domain>";
			gui.getEditorTab().setText(minDom);
			system.addUserInput("input");
			assertEquals(1, system.getState().getChanceNodes().size());
			gui.saveDomain();
			assertEquals(1, system.getState().getChanceNodes().size());
			system.addUserInput("input");
			assertEquals(2, system.getState().getChanceNodes().size());
			assertEquals("output", system.getContent("u_m").getBest().toString());

			String minDom2 = "<domain><rule></domain>";
			gui.getEditorTab().setText(minDom2);
			gui.saveDomain();
			assertTrue(gui.getChatTab().getChat().contains("be terminated"));
			assertEquals(2, system.getState().getChanceNodes().size());
			assertEquals(0, system.getDomain().getModels().size());
			gui.resetInteraction();
			assertEquals(0, system.getState().getChanceNodes().size());

			minDom2 = "<domain><rule></rule></domain>";
			gui.getEditorTab().setText(minDom2);
			gui.saveDomain();
			assertTrue(gui.getChatTab().getChat().contains("Invalid tag"));
			assertEquals(0, system.getState().getChanceNodes().size());
			assertEquals(0, system.getDomain().getModels().size());

			gui.getEditorTab().setText(minDom);
			gui.saveDomain();
			assertTrue(gui.getChatTab().getChat().contains("successfully updated"));
			system.addUserInput("input");
			assertEquals(2, system.getState().getChanceNodes().size());
			assertEquals("output", system.getContent("u_m").getBest().toString());

			Path p = Paths.get(new File("blablaNew.xml").toURI());
			assertEquals(1, Files.readAllLines(p).size());
			assertEquals(172, Files.readAllLines(p).get(0).length());
			gui.saveDomain(new File("blabla2.xml"));
			if (gui.getEditorTab().getShownFile()
					.equals(system.getDomain().getSourceFile())) {
				system.getDomain().setSourceFile(new File("blabla2.xml"));
			}
			system.refreshDomain();

			p = Paths.get(new File("blabla2.xml").toURI());
			assertEquals(1, Files.readAllLines(p).size());
			assertEquals(172, Files.readAllLines(p).get(0).length());
			minDom2 = "<domain><rule></domain>";
			gui.getEditorTab().setText(minDom2);
			gui.saveDomain();
			assertEquals(1, Files.readAllLines(p).size());
			assertEquals(23, Files.readAllLines(p).get(0).length());
			p = Paths.get(new File("blablaNew.xml").toURI());
			assertEquals(1, Files.readAllLines(p).size());
			assertEquals(172, Files.readAllLines(p).get(0).length());

			log.setLevel(null);
			Files.delete(Paths.get(new File("blablaNew.xml").toURI()));
			Files.delete(Paths.get(new File("blabla2.xml").toURI()));
			system.getModule(GUIFrame.class).getFrame().dispose();
		}
	}

	// @Test
	public void autoCompletion()
			throws IOException, InterruptedException, AWTException {
		if (!GraphicsEnvironment.isHeadless()) {
			DialogueSystem system = new DialogueSystem();
			system.startSystem();
			GUIFrame gui = system.getModule(GUIFrame.class);
			gui.newDomain(new File("blablaNew.xml"));
			gui.setActiveTab(2);
			robot.delay(200);
			for (int i = 0; i < 8; i++) {
				typeKey(KeyEvent.VK_RIGHT);
			}
			typeKey(KeyEvent.VK_ENTER);
			typeKey(KeyEvent.VK_ENTER);
			typeKey(KeyEvent.VK_BACK_QUOTE);
			typeKey(KeyEvent.VK_M);
			typeKey(KeyEvent.VK_O);
			typeKey(KeyEvent.VK_D);
			typeKey(KeyEvent.VK_E);
			typeKey(KeyEvent.VK_L);
			typeKey(KeyEvent.VK_SPACE);
			typeKey(KeyEvent.VK_U);
			typeKey(KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH);
			typeKey(KeyEvent.VK_U);
			robot.delay(100);
			assertEquals(55, gui.getEditorTab().getText().length());
			for (int i = 0; i < 2; i++) {
				typeKey(KeyEvent.VK_RIGHT);
			}
			typeKey(KeyEvent.VK_ENTER);
			typeKey(KeyEvent.VK_ENTER);
			typeKey(KeyEvent.VK_BACK_QUOTE);
			typeKey(KeyEvent.VK_R);
			typeKey(KeyEvent.VK_U);
			typeKey(KeyEvent.VK_L);
			typeKey(KeyEvent.VK_E);
			typeKey(KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE);
			robot.delay(100);
			assertEquals(236, gui.getEditorTab().getText().length());
			typeKey(KeyEvent.VK_U);
			typeKey(KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH);
			typeKey(KeyEvent.VK_U);
			typeKey(KeyEvent.VK_TAB);
			typeKey(KeyEvent.VK_B);
			typeKey(KeyEvent.VK_TAB);
			typeKey(KeyEvent.VK_C);
			typeKey(KeyEvent.VK_TAB);
			typeKey(KeyEvent.VK_D);
			for (int i = 0; i < 4; i++) {
				typeKey(KeyEvent.VK_DOWN);
			}
			typeKey(KeyEvent.VK_ENTER);
			typeKey(KeyEvent.VK_ENTER);
			robot.delay(100);
			assertEquals(244, gui.getEditorTab().getText().length());

			typeKey(KeyEvent.VK_META, KeyEvent.VK_Z);
			typeKey(KeyEvent.VK_META, KeyEvent.VK_Z);
			robot.delay(100);
			assertEquals(242, gui.getEditorTab().getText().length());

			gui.saveDomain();
			Path p = Paths.get(new File("blablaNew.xml").toURI());
			assertEquals(19, Files.readAllLines(p).size());
			system.addUserInput("b");
			assertEquals("d", system.getContent("c").getBest().toString());
			Files.delete(Paths.get(new File("blablaNew.xml").toURI()));
			system.getModule(GUIFrame.class).getFrame().dispose();
		}
	}

	private void typeKey(int... keys) {
		for (int key : keys) {
			robot.keyPress(key);
		}
		for (int key : keys) {
			robot.keyRelease(key);
		}
	}

}