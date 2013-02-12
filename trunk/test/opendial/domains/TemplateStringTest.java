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

package opendial.domains;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Logger.Level;
import opendial.bn.Assignment;
import opendial.bn.values.ValueFactory;
import opendial.domains.datastructs.Template;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class TemplateStringTest {

	// logger
	public static Logger log = new Logger("TemplateStringTest",
			Logger.Level.NORMAL);
	
	

	@Test
	public void SurfaceTemplateTest1() {
		Template template = new Template("this is a first test");
		String utterance = "bla bla this is a first test bla";
		assertTrue(template.isMatching(utterance, true));
	}
	
	@Test
	public void SurfaceTemplateTest2() {
		Template template = new Template("hi my name is {name}");
		String utterance1 = "hi my name is Pierre, how are you?";
		assertTrue(template.isMatching(utterance1, true));
		String utterance2 = "hello how are you?";
		assertFalse(template.isMatching(utterance2, true));
		String utterance3 = "hi my name is Pierre";
		assertTrue(template.isMatching(utterance3, true));
		assertTrue(template.isMatching(utterance3, false));
	}
	
	@Test
	public void SurfaceTemplateTest3() {
		Template template = new Template("hi my name is {name} and I need coffee");
		String utterance1 = " hi my name is Pierre and i need coffee ";
		String utterance2 = "hi my name is Pierre and I need coffee right now";
		assertTrue(template.isMatching(utterance1, true));
		assertTrue(template.isMatching(utterance2, true));
		String utterance3 = "hello how are you?";
		assertFalse(template.isMatching(utterance3, true));
		
		assertFalse(template.isMatching(utterance3, false));
		assertTrue(template.isMatching(utterance1, false));
	}
	 
	@Test
	public void SurfaceTemplateTest4() {
		Template template1 = new Template("hi my name is {name}");
		assertEquals("Pierre Lison", template1.extractParameters("hi my name is Pierre Lison ", true).getValue("name").toString());

		Template template2 = new Template("{name} is my name");
		assertEquals("Pierre Lison", template2.extractParameters("Pierre Lison is my name", true).getValue("name").toString());

		
		Template template3 = new Template("hi my name is {name} and I need coffee");
		assertEquals("Pierre", template3.extractParameters("hi my name is Pierre and I need coffee ", true).getValue("name").toString());
	}
	
	@Test
	public void SurfaceTemplateTest5() {
		Template template1 = new Template("hi this is {A} and this is {B}");
		assertEquals("an apple", template1.extractParameters("hi this is an apple and this is a banana", true).getValue("A").toString());
		assertEquals("a banana", template1.extractParameters("hi this is an apple and this is a banana", true).getValue("B").toString());
	}
	
	
	@Test
	public void SurfaceTemplateTest6() {
		Template template1 = new Template("{anything}");
		assertEquals("bla bla bla", template1.extractParameters("bla bla bla", true).getValue("anything").toString());
		
		Template template2 = new Template("{anything} is good");
		assertEquals("bla bla bla", template2.extractParameters("bla bla bla is good", true).getValue("anything").toString());
		assertFalse(template2.isMatching("blo blo", true));
		assertFalse(template2.extractParameters("bla bla bla is bad", true).containsVar("anything"));
		assertTrue(template2.isMatching("blo is good", true));
		
		Template template3 = new Template("this could be {anything}");
		assertEquals("pretty much anything", template3.extractParameters("this could be pretty much anything", true).getValue("anything").toString());
		assertFalse(template3.isMatching("but not this", true));
		assertFalse(template3.isMatching("this could beA", true));		
		assertFalse(template3.isMatching("this could beA", false));		
		assertFalse(template3.isMatching("this could be", true));		
		assertFalse(template3.isMatching("this could be", false));		
	}
	
	
	@Test
	public void SurfaceTemplateTest7() throws Exception {
		Template template1 = new Template("here we have slot {A} and slot {B}");
		Assignment fillers = new Assignment();
		fillers.addPair("A", "apple");
		fillers.addPair("B", "banana");
		assertEquals("here we have slot apple and slot banana", template1.fillSlots(fillers));
		fillers.removePair("B");
		try {
			Template.log.setLevel(Level.MIN);
			assertEquals("here we have slot apple and slot banana", template1.fillSlots(fillers));	
			throw new Exception("?");
		}
		catch (DialException e) { }
	}
	
	@Test
	public void SurfaceTemplateTest8() throws Exception {
		Template template = new Template("here we have a test");
		assertFalse(template.isMatching("here we have a test2", false));
		assertFalse(template.isMatching("here we have a test2", true));
		assertTrue(template.isMatching("here we have a test that is working", true));
		assertFalse(template.isMatching("here we have a test that is working", false));
	
		Template template2 = new Template("bla");
		assertFalse(template2.isMatching("bla2", true));
		assertFalse(template2.isMatching("blabla", true));
		assertTrue(template2.isMatching("bla bla", true));
		assertFalse(template2.isMatching("bla bla", false));
	}
	
	@Test
	public void SurfaceTemplateTest9() {
		Template template1 = new Template("{anything}");
		assertEquals(ValueFactory.create(0), template1.getMatchBoundaries
				("bla bla bla", false).getValue("match.start"));
		assertEquals(ValueFactory.create(11), template1.getMatchBoundaries
				("bla bla bla", false).getValue("match.end"));
		Template template2 = new Template("this could be {anything}, right");
		assertEquals(ValueFactory.create(4), template2.getMatchBoundaries
				("and this could be pretty much anything, right", true).getValue("match.start"));
		assertEquals(ValueFactory.create("and this could be pretty much anything, right".length()), 
				template2.getMatchBoundaries
				("and this could be pretty much anything, right", true).getValue("match.end"));
		assertEquals(ValueFactory.create(-1), 
				template2.getMatchBoundaries
				("and this could be pretty much anything", true).getValue("match.end"));
		

	}
}
