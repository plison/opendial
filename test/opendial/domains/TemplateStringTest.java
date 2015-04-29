// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)

// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.domains;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.datastructs.Assignment;
import opendial.datastructs.Template;
import opendial.readers.XMLDomainReader;
import opendial.utils.MathUtils;

import org.junit.Test;

/**
 * 
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class TemplateStringTest {

	// logger
	public static Logger log = new Logger("TemplateStringTest",
			Logger.Level.DEBUG);

	@Test
	public void testTemplate1() {
		Template template = new Template("this is a first test");
		String utterance = "bla bla this is a first test bla";
		assertTrue(template.partialmatch(utterance).isMatching());
	}

	@Test
	public void testTemplate2() {
		Template template = new Template("hi my name is {name}");
		String utterance1 = "hi my name is Pierre, how are you?";
		assertTrue(template.partialmatch(utterance1).isMatching());
		String utterance2 = "hello how are you?";
		assertFalse(template.partialmatch(utterance2).isMatching());
		String utterance3 = "hi my name is Pierre";
		assertTrue(template.partialmatch(utterance3).isMatching());
		assertTrue(template.match(utterance3).isMatching());
	}

	@Test
	public void testTemplate3() {
		Template template = new Template(
				"hi my name is {name} and I need coffee");
		String utterance1 = " hi my name is Pierre and i need coffee ";
		String utterance2 = "hi my name is Pierre and I need coffee right now";
		assertTrue(template.partialmatch(utterance1).isMatching());
		assertTrue(template.partialmatch(utterance2).isMatching());
		String utterance3 = "hello how are you?";
		assertFalse(template.partialmatch(utterance3).isMatching());

		assertFalse(template.match(utterance3).isMatching());
		assertTrue(template.match(utterance1).isMatching());
	}

	@Test
	public void testTemplate4() {
		Template template1 = new Template("hi my name is {name}");
		assertEquals("Pierre Lison",
				template1.match("hi my name is Pierre Lison ").getFilledSlots()
						.getValue("name").toString());

		Template template2 = new Template("{name} is my name");
		assertEquals("Pierre Lison", template2.match("Pierre Lison is my name")
				.getFilledSlots().getValue("name").toString());

		Template template3 = new Template(
				"hi my name is {name} and I need coffee");
		assertEquals("Pierre",
				template3.match("hi my name is Pierre and I need coffee ")
						.getFilledSlots().getValue("name").toString());
	}

	@Test
	public void testTemplate5() {
		Template template1 = new Template("hi this is {A} and this is {B}");
		assertEquals("an apple",
				template1.match("hi this is an apple and this is a banana")
						.getFilledSlots().getValue("A").toString());
		assertEquals("a banana",
				template1.match("hi this is an apple and this is a banana")
						.getFilledSlots().getValue("B").toString());
	}

	@Test
	public void testTemplate6() {
		Template template1 = new Template("{anything}");
		assertEquals("bla bla bla", template1.match("bla bla bla")
				.getFilledSlots().getValue("anything").toString());

		Template template2 = new Template("{anything} is good");
		assertEquals("bla bla bla", template2.match("bla bla bla is good")
				.getFilledSlots().getValue("anything").toString());
		assertFalse(template2.match("blo blo").isMatching());
		assertFalse(template2.match("bla bla bla is bad").getFilledSlots()
				.containsVar("anything"));
		assertTrue(template2.match("blo is good").isMatching());

		Template template3 = new Template("this could be {anything}");
		assertEquals("pretty much anything",
				template3.match("this could be pretty much anything")
						.getFilledSlots().getValue("anything").toString());
		assertFalse(template3.match("but not this").isMatching());
		assertFalse(template3.match("this could beA").isMatching());
		assertFalse(template3.partialmatch("this could beA").isMatching());
		assertFalse(template3.match("this could be").isMatching());
		assertFalse(template3.partialmatch("this could be").isMatching());
	}

	@Test
	public void testTemplate7() throws Exception {
		Template template1 = new Template("here we have slot {A} and slot {B}");
		Assignment fillers = new Assignment();
		fillers.addPair("A", "apple");
		fillers.addPair("B", "banana");
		assertEquals("here we have slot apple and slot banana", template1
				.fillSlots(fillers).getRawString());
		fillers.removePair("B");
		assertEquals("B", template1.fillSlots(fillers).getSlots().iterator()
				.next());
	}

	@Test
	public void testTemplate8() throws Exception {
		Template template = new Template("here we have a test");
		assertFalse(template.match("here we have a test2").isMatching());
		assertFalse(template.partialmatch("here we have a test2").isMatching());
		assertTrue(template.partialmatch("here we have a test that is working")
				.isMatching());
		assertFalse(template.match("here we have a test that is working")
				.isMatching());

		Template template2 = new Template("bla");
		assertFalse(template2.partialmatch("bla2").isMatching());
		assertFalse(template2.partialmatch("blabla").isMatching());
		assertTrue(template2.partialmatch("bla bla").isMatching());
		assertFalse(template2.match("bla bla").isMatching());
	}

	@Test
	public void testTemplate9() {
		Template template1 = new Template("{anything}");
		assertEquals(0, template1.match("bla bla bla").getBoundaries()[0], 0.0);
		assertEquals(11, template1.match("bla bla bla").getBoundaries()[1], 0.0);
		Template template2 = new Template("this could be {anything}, right");
		assertEquals(
				4,
				template2.partialmatch(
						"and this could be pretty much anything, right")
						.getBoundaries()[0], 0.0);
		assertEquals(
				"and this could be pretty much anything, right".length(),
				template2.partialmatch(
						"and this could be pretty much anything, right")
						.getBoundaries()[1], 0.0);
		assertEquals(-1,
				template2
						.partialmatch("and this could be pretty much anything")
						.getBoundaries()[1], 0.0);

		Template template3 = new Template("{}");
		assertEquals(0, template3.getSlots().size());
		assertTrue(template3.match("{}").isMatching());
		assertTrue(template3.partialmatch("{}").isMatching());
		assertFalse(template3.match("something").isMatching());
		assertFalse(template3.partialmatch("something").isMatching());
	}

	/**
	 * public void testTemplateOr() { Template t1 = new Template("var({X})");
	 * Template t2 = new Template("var3"); Template t3 = new Template("bli");
	 * Template or = new Template(Arrays.asList(t1, t2, t3));
	 * assertTrue(or.match("var3").isMatching());
	 * assertTrue(or.match("var(blo)").isMatching());
	 * assertTrue(or.match("bli").isMatching());
	 * assertFalse(or.match("var3bli").isMatching());
	 * assertFalse(or.match("var").isMatching()); }
	 */

	@Test
	public void testTemplateQuick() throws DialException {
		Domain domain = XMLDomainReader
				.extractDomain("test/domains/quicktest.xml");
		DialogueSystem system = new DialogueSystem(domain);
		system.getSettings().showGUI = false;

		system.startSystem();
		assertEquals(system.getContent("caught").getProb(false), 1.0, 0.01);
		assertEquals(system.getContent("caught2").getProb(true), 1.0, 0.01);
	}

	@Test
	public void testTemplateMath() {
		assertEquals(MathUtils.evaluateExpression("1+2"), 3.0, 0.001);
		assertEquals(MathUtils.evaluateExpression("-1.2*3"), -3.6, 0.001);
		Template t = new Template("{X}+2");
		assertEquals(t.fillSlots(new Assignment("X", "3")).toString(), "5");
	}

	@Test
	public void ComplexRegex() {
		Template t = new Template("a (pizza)? margherita");
		assertTrue(t.match("a margherita").isMatching());
		assertTrue(t.match("a pizza margherita").isMatching());
		assertFalse(t.match("a pizza").isMatching());
		assertTrue(t.partialmatch("I would like a margherita").isMatching());
		Template t2 = new Template("a (bottle of)? (beer|wine)");
		assertTrue(t2.match("a beer").isMatching());
		assertTrue(t2.match("a bottle of wine").isMatching());
		assertFalse(t2.match("a bottle of").isMatching());
		assertFalse(t2.match("a coke").isMatching());
		assertTrue(t2.partialmatch("I would like a bottle of beer")
				.isMatching());
		Template t3 = new Template("move (a (little)? bit)? (to the)? left");
		assertTrue(t3.match("move a little bit to the left").isMatching());
		assertTrue(t3.match("move a bit to the left").isMatching());
		assertTrue(t3.match("move to the left").isMatching());
		assertTrue(t3.match("move a little bit left").isMatching());
		assertFalse(t3.match("move a to the left").isMatching());
		Template t4 = new Template("I want beer(s)?");
		assertTrue(t4.match("I want beer").isMatching());
		assertTrue(t4.match("I want beers").isMatching());
		assertFalse(t4.match("I want beer s").isMatching());
		Template t5 = new Template("(beer(s)?|wine)");
		assertTrue(t5.match("beer").isMatching());
		assertTrue(t5.match("beers").isMatching());
		assertTrue(t5.match("wine").isMatching());
		assertFalse(t5.match("wines").isMatching());
		assertFalse(t5.match("beer wine").isMatching());
		assertTrue(new Template("* (to the|at the)? left of").match(
				"window to the left of").isMatching());
		assertTrue(new Template("* (to the|at the)? left of").match(
				"window left of").isMatching());
		assertTrue(new Template("* (to the|at the)? left of").match("left of")
				.isMatching());
	}

	@Test
	public void testDouble() {
		Template t = new Template("MakeOrder({Price})");
		assertTrue(t.match("MakeOrder(179)").isMatching());
		assertTrue(t.match("MakeOrder(179.0)").isMatching());
		assertFalse(t.match("MakkeOrder(179.0)").isMatching());
		assertFalse(t.match("MakkeOrder()").isMatching());
	}

	@Test
	public void testMatchInString() {
		Template t = new Template("{X}th of March");
		assertTrue(t.match("20th of March").isMatching());
		assertTrue(t.partialmatch("on the 20th of March").isMatching());
		assertFalse(t.match("20 of March").isMatching());
	}

	@Test
	public void testStar() {
		Template t1 = new Template("here is * test");
		assertTrue(t1.match("here is test").isMatching());
		assertTrue(t1.match("here is a test").isMatching());
		assertTrue(t1.match("here is a great test").isMatching());
		assertFalse(t1.match("here is a bad nest").isMatching());
		t1 = new Template("* test");
		assertTrue(t1.match("test").isMatching());
		assertTrue(t1.match("great test").isMatching());
		assertFalse(t1.match("here is a bad nest").isMatching());
		t1 = new Template("test *");
		assertTrue(t1.match("test").isMatching());
		assertTrue(t1.match("test that works").isMatching());
		assertFalse(t1.match("nest that is bad").isMatching());
		t1 = new Template("this is a * {test}");
		assertTrue(t1.match("this is a ball").isMatching());
		assertTrue(t1.match("this is a really great ball").isMatching());
		assertFalse(t1.match("this is huge").isMatching());
		assertEquals("ball", t1.match("this is a ball").getFilledSlots()
				.getValue("test").toString());
		assertEquals("ball", t1.match("this is a great blue ball")
				.getFilledSlots().getValue("test").toString());
		t1 = new Template("* {test}");
		assertEquals("ball", t1.match("this is a great ball").getFilledSlots()
				.getValue("test").toString());
		assertEquals("ball", t1.match("ball").getFilledSlots().getValue("test")
				.toString());
		t1 = new Template("{test} *");
		assertEquals("great ball", t1.match("great ball").getFilledSlots()
				.getValue("test").toString());
		assertEquals("ball", t1.match("ball").getFilledSlots().getValue("test")
				.toString());
	}
}
