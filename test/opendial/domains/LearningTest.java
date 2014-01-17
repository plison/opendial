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

package opendial.domains;

import static org.junit.Assert.*;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.BNetwork;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.modules.ForwardPlanner;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLStateReader;

public class LearningTest {

	// logger
	public static Logger log = new Logger("LearningTest", Logger.Level.DEBUG);


	public static final String domainFile = "test//domains//domain-is.xml";
	public static final String parametersFile = "test//domains/params-is.xml";

	@Test
	public void is2013Test() throws DialException, InterruptedException {
		Domain domain = XMLDomainReader.extractDomain(domainFile);
		BNetwork params = XMLStateReader.extractBayesianNetwork(parametersFile, "parameters");
		domain.setParameters(params);
	//	Settings.guiSettings.showGUI = true;
	DialogueSystem system = new DialogueSystem(domain);
	system.getSettings().showGUI = false;
	system.detachModule(system.getModule(ForwardPlanner.class));
	Settings.maxSamplingTime = Settings.maxSamplingTime*3;
	system.startSystem(); 
	
	Double[] initMean = system.getContent("theta_1").toContinuous().getFunction().getMean();

	CategoricalTable table = new CategoricalTable();
	table.addRow(new Assignment("a_u", "Move(Left)"), 1.0);
	table.addRow(new Assignment("a_u", "Move(Right)"), 0.0);
	table.addRow(new Assignment("a_u", "None"), 0.0);
	system.addContent(table);
	system.getState().removeNodes(system.getState().getUtilityNodeIds());
	system.getState().removeNodes(system.getState().getActionNodeIds());
	
	Double[] afterMean = system.getContent("theta_1").toContinuous().getFunction().getMean();

	assertTrue(afterMean[0] - initMean[0] > 0.04);
	assertTrue(afterMean[1] - initMean[1] < 0.04);
	assertTrue(afterMean[2] - initMean[2] < 0.04);
	assertTrue(afterMean[3] - initMean[3] < 0.04);
	assertTrue(afterMean[4] - initMean[4] < 0.04);
	assertTrue(afterMean[5] - initMean[5] < 0.04);
	assertTrue(afterMean[6] - initMean[6] < 0.04);
	assertTrue(afterMean[7] - initMean[7] < 0.04);
	
	Settings.maxSamplingTime = Settings.maxSamplingTime/3;
	
	}
}

