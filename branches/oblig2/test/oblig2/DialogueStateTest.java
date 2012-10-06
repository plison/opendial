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

package oblig2;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import oblig2.state.DialogueState;
import oblig2.util.Logger;

import org.junit.Test;

/**
 * Some JUnit testing for the dialogue state
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class DialogueStateTest {

	public static Logger log = new Logger("DialogueStateTest", Logger.Level.DEBUG);

	// AT&T parameters
	static String uuid = "F9A9D13BC9A811E1939C95CDF95052CC";
	static String	appname = "def001";
	static String	grammar = "numbers";
	

	@Test
	public void basicTest() throws InterruptedException, FileNotFoundException {
		
		ConfigParameters parameters = new ConfigParameters (uuid, appname, grammar);
		parameters.activateGUI = false;
		parameters.doTesting = false;
		parameters.activateSound = false;

		DialoguePolicy policy = new BasicPolicy();
		
		DialogueSystem system = new DialogueSystem(policy, parameters);
		system.start();
		
		DialogueState dstate = system.getDialogueState();
		assertTrue(dstate.getDialogueHistory().isEmpty());

		// just waiting for the speech recogniser to connect to the dialogue state
		Thread.sleep(80);
		
		dstate.newSpeechSignal(new FileInputStream(parameters.testASRFile));
		
		assertEquals(2, dstate.getDialogueHistory().size());
		assertEquals("one two three four five", dstate.getDialogueHistory().get(0).getUtterance().getHypotheses().get(0).getString());
		assertEquals(1, dstate.getDialogueHistory().get(0).getUtterance().getHypotheses().size());
		assertEquals(1.0f, dstate.getDialogueHistory().get(0).getUtterance().getHypotheses().get(0).getConf(), 0.0001f);
		assertEquals("user", dstate.getDialogueHistory().get(0).getAgent());
		
		assertEquals("you said: one two three four five", dstate.getDialogueHistory().get(1).getUtterance().getHypotheses().get(0).getString());
		assertEquals(1, dstate.getDialogueHistory().get(1).getUtterance().getHypotheses().size());
		assertEquals(1.0f, dstate.getDialogueHistory().get(1).getUtterance().getHypotheses().get(0).getConf(), 0.0001f);
		assertEquals("robot", dstate.getDialogueHistory().get(1).getAgent());
		
		
	}
}
