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

package opendial.processes;

import static org.junit.Assert.*;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.domains.Domain;
import opendial.inputs.NBestList;
import opendial.outputs.Action;
import opendial.outputs.VoidAction;
import opendial.readers.XMLDomainReader;
import opendial.utils.Logger;

/**
 * Testing for the general dialogue system pipeline
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueSystemTest1 {

	static Logger log = new Logger("DialogueSystemTest1", Logger.Level.DEBUG);
	
	public String dialDomain = "domains//testing//microdom2.xml";

	 
	@Test
	public void pipelineTest1() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		DialogueSystem ds = new DialogueSystem(domain);
		Action action = ds.pollNextAction();
		log.debug("yoowhoo, action retrieved!!");
		assertTrue(action instanceof VoidAction);
	}
	
	
	@Test
	public void pipelineTest2() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		DialogueSystem ds = new DialogueSystem(domain);
		NBestList obs = new NBestList();
		obs.addUtterance("robot, please do X!", 1.0f);
		ds.addObservation(obs);
		Action action = ds.pollNextAction();
		log.debug("yoowhoo, action retrieved!!");
		assertTrue(action instanceof VoidAction);
	}
	
	
	@Test
	public void pipelineTest3() throws DialException, InterruptedException {
		
		XMLDomainReader reader = new XMLDomainReader();
		Domain domain = reader.extractDomain(dialDomain);
		DialogueSystem ds = new DialogueSystem(domain);
		NBestList obs = new NBestList();
		obs.addUtterance("robot, please do X!", 1.0f);
		ds.addObservation(obs);

		Thread.sleep(200);

		Action action = ds.pollNextAction();
		log.debug("yoowhoo, action retrieved!!");
		assertTrue(action instanceof VoidAction);
	}
}
