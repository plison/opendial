package opendial.experiments;
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


import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.BNetwork;
import opendial.common.InferenceChecks;
import opendial.domains.Domain;
import opendial.gui.GUIFrame;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLSettingsReader;
import opendial.readers.XMLStateReader;
import opendial.simulation.UserSimulator;

public class ACL2013 {

	// logger
	public static Logger log = new Logger("Main", Logger.Level.DEBUG);


//	public static final String domainFile = "domains//is2013/linear/domain.xml";
//	public static final String parametersFile = "domains//is2013/linear/params_linear.xml";
	public static final String simulatorFile = "domains//acl2013/simulator/simulator.xml";

	public static void main(String[] args) {
		try {
			if (args.length != 3) {
				throw new DialException("must provide arguments for domain and parameter");
			}
			String settingsFile = args[0];
			String domainFile = args[1];
			String parametersFile = args[2];
			Settings settings = XMLSettingsReader.extractSettings(settingsFile); 
			Domain domain = XMLDomainReader.extractDomain(domainFile);
			BNetwork params = XMLStateReader.extractBayesianNetwork(parametersFile);
			Domain simulatorDomain = XMLDomainReader.extractDomain(simulatorFile); 
		DialogueSystem system = new DialogueSystem(settings, domain);
		system.addParameters(params);
		system.attachSimulator(simulatorDomain);
		system.startSystem(); 
		
		}
		catch (DialException e) {
			log.warning("exception thrown " + e + ", aborting");
		}
	}
}

