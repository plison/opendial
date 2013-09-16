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
import opendial.gui.WOZFrame;
import opendial.modules.NaoASR;
import opendial.modules.NaoBehaviour;
import opendial.modules.NaoPerception;
import opendial.modules.NaoTTS;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLSettingsReader;
import opendial.readers.XMLStateReader;
import opendial.simulation.UserSimulator;

public class LennyDemo {

	// logger
	public static Logger log = new Logger("Main", Logger.Level.DEBUG);


	public static final String domainFile = "domains//demo/domain.xml";
	public static final String settingsFile = "domains//demo/settings.xml";

	public static void main(String[] args) {
		try {
			Domain domain = XMLDomainReader.extractDomain(domainFile);
			Settings settings = XMLSettingsReader.extractSettings(settingsFile); 
		DialogueSystem system = new DialogueSystem(settings, domain);
	/**	WOZFrame woz = new WOZFrame(system);
		NaoTTS tts = new NaoTTS();
		system.getState().attachModule(tts);
		NaoBehaviour b = new NaoBehaviour();
		system.getState().attachModule(b);
		NaoPerception perception = new NaoPerception(system);
		system.attachAsynchronousModule(perception);
		
		NaoASR asr = new NaoASR(system);
		system.attachAsynchronousModule(asr); */

		system.startSystem(); 
		
		}
		catch (Exception e) {
			log.warning("exception thrown " + e + ", aborting");
			e.printStackTrace();
		}
	}
	
}
