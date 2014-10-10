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

import java.awt.Container;
import java.util.List;

import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.modules.core.DialogueRecorder;
import opendial.modules.core.WizardControl;
import opendial.modules.core.WizardLearner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.state.DialogueState;

import org.junit.Test;

public class WizardTest {

	// logger
	public static Logger log = new Logger("WizardTest", Logger.Level.DEBUG);

	
	public static final String interactionFile = "test//domains//woz-dialogue.xml";
	public static final String domainFile = "test//domains//domain-woz.xml";

	@Test
	public void testWizardLearning() throws DialException {
		List<DialogueState> interaction = XMLInteractionReader.extractInteraction(interactionFile);
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = false;

		system.attachModule(WizardLearner.class);
		system.startSystem();
		for (DialogueState s : interaction) {
			system.addContent(s.copy());
		}
		log.debug("theta 1: " + ((ContinuousDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).getFunction().getMean()[0]);
		assertTrue(((ContinuousDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).getFunction().getMean()[0] > 16.0);
		log.debug("theta 2: " + ((ContinuousDistribution)system.getState().getChanceNode
				("theta_2").getDistrib()).getFunction().getMean()[0]);
		assertTrue(((ContinuousDistribution)system.getState().getChanceNode("theta_2").getDistrib())
				.getFunction().getMean()[0] < 9.0);
		
	}
	
	@Test
	public void testWizardControl() throws DialException, InterruptedException {
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = true;
		system.attachModule(WizardControl.class);
		system.startSystem();

		assertEquals(2, system.getModule(GUIFrame.class).getChatTab().getComponentCount());
		system.addContent(new Assignment("u_u", "hi"));
		assertEquals(3, system.getModule(GUIFrame.class).getChatTab().getComponentCount());
		assertEquals(3, ((JList)((JViewport)((JScrollPane)((Container)system.getModule(GUIFrame.class).
				getChatTab().getComponent(2)).getComponent(0)).getComponent(0)).getComponent(0)).getModel().getSize());
		system.addContent(new Assignment("a_m", "Say(Greet)"));		
		system.addContent(new Assignment("u_u", "move left"));
		assertEquals(3, system.getModule(GUIFrame.class).getChatTab().getComponentCount());
		assertEquals(4, ((JList)((JViewport)((JScrollPane)((Container)system.getModule(GUIFrame.class).
				getChatTab().getComponent(2)).getComponent(0)).getComponent(0)).getComponent(0)).getModel().getSize());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
				+ "<interaction><userTurn><variable id=\"u_u\"><value>hi</value></variable></userTurn>"
				+ "<systemTurn><variable id=\"u_m\"><value>Hi there</value></variable></systemTurn><userTurn>"
				+ "<variable id=\"u_u\"><value>move left</value></variable></userTurn></interaction>", 
				system.getModule(DialogueRecorder.class).getRecord());
		system.getModule(GUIFrame.class).getFrame().dispose();
	}
	
	
}

