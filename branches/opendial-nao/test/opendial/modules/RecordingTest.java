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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.distribs.continuous.functions.UniformDensityFunction;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.gui.GUIFrame;
import opendial.gui.GUIMenuBar;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.utils.StringUtils;

import org.junit.Test;

public class RecordingTest {

	public static final String domainFile = "test//domains//domain-demo.xml";
	public static final String domainFile2 = "test//domains//domain-woz.xml";
	public static final String importState = "test//domains//domain-demo-importstate.xml";
	public static final String importParams = "test//domains//domain-demo-importparams.xml";
	public static final String exportState = "test//domains//domain-demo-exportstate.xml";
	public static final String exportParams = "test//domains//domain-demo-exportparams.xml";
	public static String dialogueFile = "test/domains/woz-dialogue.xml";

	// logger
	public static Logger log = new Logger("RecordingTest", Logger.Level.DEBUG);
	
	@Test
	public void testRecord() throws DialException, InterruptedException {
		
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
		table.addRow(new Assignment("u_u", "move left"), 0.2);
		table.addRow(new Assignment("u_u", "move a bit to the left"), 0.65);	
		system.addContent(table);
		
		assertTrue(system.getModule(GUIFrame.class).getChatTab().getChat().contains
				("<font size=\"4\">move a bit to the left (0.05)</font>"));
		assertTrue(system.getModule(GUIFrame.class).getChatTab().getChat().contains
				("<font size=\"4\">OK, moving Left a little bit</font>"));
		assertEquals(6, StringUtils.countOccurrences(system.getModule(DialogueRecorder.class).getRecord(), "userTurn"));
		assertEquals(4, StringUtils.countOccurrences(system.getModule(DialogueRecorder.class).getRecord(), "systemTurn"));
		assertEquals(10, StringUtils.countOccurrences(system.getModule(DialogueRecorder.class).getRecord(), "variable"));
	
		system.getModule(GUIFrame.class).getFrame().dispose();
	}
	
	@Test
	public void testXML() throws DialException, InterruptedException, IOException {
		
		DialogueSystem system = new DialogueSystem(XMLDomainReader.extractDomain(domainFile2));
		system.getSettings().showGUI = false;
		system.startSystem();
		
		GUIMenuBar.importAction(system, importState, "state");
		assertEquals(12, system.getState().getChanceNodeIds().size());
		assertEquals(0.7, system.getContent("aha").toDiscrete().getProb(new Assignment("aha", "ohoho")), 0.01);
		
		GUIMenuBar.importAction(system, importParams, "parameters");
		assertEquals(14, system.getState().getChanceNodeIds().size());
		assertTrue(system.getContent("theta_2").toContinuous().getFunction() instanceof UniformDensityFunction);
		
		Settings.nbSamples = Settings.nbSamples / 100;
		DialogueImporter importer = new DialogueImporter(system, 
				XMLInteractionReader.extractInteraction(dialogueFile));
		system.startSystem();
		importer.start();
		while (importer.isAlive()) {
			Thread.sleep(50);
		}
		Settings.nbSamples = Settings.nbSamples * 100;

		GUIMenuBar.exportAction(system, exportState, "state");
		String str = "";
		BufferedReader br = new BufferedReader(new FileReader(exportState));
		String line = "";
		while ((line = br.readLine()) != null) {
			str += line;
		}
		br.close();
		assertEquals(26, StringUtils.countOccurrences(str, "variable"));
		GUIMenuBar.exportAction(system, exportParams, "parameters");
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

