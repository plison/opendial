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

package opendial.bn;

import static org.junit.Assert.*;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import org.junit.Test;

import opendial.DialogueSystem;
import opendial.bn.values.RelationalVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.domains.Domain;
import opendial.readers.XMLDomainReader;
import opendial.templates.RelationalTemplate;

public class RelationalTest {

	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void relationalTest() {
		RelationalVal rel = (RelationalVal) ValueFactory.create(
				"[sees|tag:VB subject>John object>Anne instrument>[telescope|tag:NN colour>red|tag:ADJ]]");
		assertEquals(5, rel.length());
		assertTrue(rel.getSubValues().contains(ValueFactory.create("telescope")));
		assertEquals("sees", rel.getNodes().get(0).getContent().toString());
		RelationalTemplate t = new RelationalTemplate("[sees subject>John]");
		assertEquals(1, t.getMatches(rel).size());
		t = new RelationalTemplate("[sees {S}>John]");
		assertEquals(1, t.getMatches(rel).size());
		assertEquals("subject", t.getMatches(rel).get(0).getValue("S").toString());
		t = new RelationalTemplate("[sees {S}>{O}]");
		assertEquals(3, t.getMatches(rel).size());
		assertEquals("instrument",
				t.getMatches(rel).get(0).getValue("S").toString());
		assertEquals("telescope", t.getMatches(rel).get(0).getValue("O").toString());
		t = new RelationalTemplate("[{V}|tag:{T} subject>{X} object>{Y}]");
		assertEquals("sees", t.getMatches(rel).get(0).getValue("V").toString());
		assertEquals("VB", t.getMatches(rel).get(0).getValue("T").toString());
		assertEquals("John", t.getMatches(rel).get(0).getValue("X").toString());
		assertEquals("Anne", t.getMatches(rel).get(0).getValue("Y").toString());
		t = new RelationalTemplate("[sees +>red|tag:{X}]");
		assertEquals(1, t.getMatches(rel).size());
		assertEquals("ADJ", t.getMatches(rel).get(0).getValue("X").toString());
		RelationalVal rel2 = (RelationalVal) ValueFactory.create(
				"[sees|tag:VB object>Anne instrument>[telescope|tag:NN colour>red|tag:ADJ] subject>John]");
		assertEquals(rel, rel2);
		assertEquals(rel.hashCode(), rel2.hashCode());
		assertTrue(rel2.contains(ValueFactory.create("Anne")));
		t = new RelationalTemplate("[sees {S}>John]");
		assertEquals(1, t.getSlots().size());
		assertEquals("[sees subject>John]",
				t.fillSlots(new Assignment("S", "subject")));

	}

	@Test
	public void functionTest() throws InterruptedException {
		Domain d = XMLDomainReader.extractDomain("test/domains/relationaltest.xml");
		DialogueSystem system = new DialogueSystem(d);
		system.getSettings().showGUI = false;
		system.startSystem();
		// assertEquals(0.5, system.getContent("second").getProb("bla"), 0.05);
	}

	public static final class TestFunction implements Function<List<String>, Value> {

		@Override
		public Value apply(List<String> t) {
			String arg = t.get(0);
			int length = arg.length();
			int nbWords = arg.split(" ").length;
			return ValueFactory.create(new double[] { length, nbWords });
		}
	}

}
