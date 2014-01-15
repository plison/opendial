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
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;

public class RecordingTest {

	public static final String domainFile = "test//domains//domain-demo.xml";

	// logger
	public static Logger log = new Logger("GUITest", Logger.Level.DEBUG);
	
	@Test
	public void recordTest() throws DialException, InterruptedException {
		
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile));
		system.getSettings().showGUI = true;
		system.startSystem();
		
		CategoricalTable table = new CategoricalTable();
		table.addRow(new Assignment("u_u", "move left"), 0.3);
		table.addRow(new Assignment("u_u", "move a bit to the left"), 0.05);
		system.addContent(table);

		table = new CategoricalTable();
		table.addRow(new Assignment("u_u", "no"), 0.5);
		system.addContent(table);
		system.pause(true);
		table.addRow(new Assignment("u_u", "now you should not hear anything"), 0.8);
		system.pause(false);
		table = new CategoricalTable();
		table.addRow(new Assignment("u_u", "move left"), 0.3);
		table.addRow(new Assignment("u_u", "move a bit to the left"), 0.65);	
		system.addContent(table);
		
		log.debug("size " + system.getModule(GUIFrame.class).getChatTab().getChat().length());
		assertTrue(system.getModule(GUIFrame.class).getChatTab().getChat().contains
				("<font size=\"4\">move a bit to the left (0.05)</font>"));
		assertTrue(system.getModule(GUIFrame.class).getChatTab().getChat().contains
				("<font size=\"4\">OK, moving Left a little bit</font>"));
		assertEquals(1566, system.getModule(GUIFrame.class).getChatTab().getChat().length());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n<interaction><userTurn><variable id=\"u_u\">"
				+ "<value prob=\"0.65\">None</value><value prob=\"0.3\">move left</value><value prob=\"0.05\">"
				+ "move a bit to the left</value></variable></userTurn><systemTurn><variable id=\"u_m\"><value "
				+ "prob=\"1.0\">OK, moving Left</value></variable></systemTurn><userTurn><variable id=\"u_u\">"
				+ "<value prob=\"0.5\">None</value><value prob=\"0.5\">no</value></variable></userTurn><userTurn>"
				+ "<variable id=\"u_u\"><value prob=\"0.05\">None</value><value prob=\"0.3\">move left</value>"
				+ "<value prob=\"0.65\">move a bit to the left</value></variable></userTurn><systemTurn>"
				+ "<variable id=\"u_m\"><value prob=\"1.0\">OK, moving Left a little bit</value></variable>"
				+ "</systemTurn></interaction>", system.getModule(DialogueRecorder.class).getRecord());
		system.getModule(GUIFrame.class).getFrame().dispose();
	}
		

}

