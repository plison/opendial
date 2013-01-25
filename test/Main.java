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
import opendial.common.InferenceChecks;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;

public class Main {

	// logger
	public static Logger log = new Logger("Main", Logger.Level.NORMAL);


	public static final String domainFile = "domains//testing//basicfulltest.xml";

	public static void main(String[] args) {
		try {
		Domain domain = XMLDomainReader.extractDomain(domainFile); 
		Settings.showGUI = true;
		DialogueSystem system = new DialogueSystem(domain);
		system.startSystem(); 
		}
		catch (DialException e) {
			log.warning("exception thrown " + e + ", aborting");
		}
	}
}

