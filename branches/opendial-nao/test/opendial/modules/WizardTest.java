// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
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
import opendial.bn.distribs.other.MarginalEmpiricalDistribution;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
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
		log.debug("theta 1: " + ((MarginalEmpiricalDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).toContinuous().getFunction().getMean()[0]);
		assertTrue(((MarginalEmpiricalDistribution)system.getState().getChanceNode
				("theta_1").getDistrib()).toContinuous().getFunction().getMean()[0] > 16.0);
		log.debug("theta 2: " + ((MarginalEmpiricalDistribution)system.getState().getChanceNode
				("theta_2").getDistrib()).toContinuous().getFunction().getMean()[0]);
		assertTrue(((MarginalEmpiricalDistribution)system.getState().getChanceNode("theta_2").getDistrib())
				.toContinuous().getFunction().getMean()[0] < 9.0);
		
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

