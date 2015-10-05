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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Test;

import opendial.bn.values.RelationalVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Template;

public class RelationalTest {

	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void relValueTest() throws InterruptedException {

		RelationalVal builder = new RelationalVal();
		List<String> attrs = new ArrayList<String>();
		attrs.add("name");
		builder.addNode("John", attrs);
		builder.addNode("sees");
		builder.addNode("Anne", attrs);
		builder.addNode("with");
		builder.addNode("a");
		builder.addNode("red");
		builder.addNode("telescope");
		builder.addEdge(1, 0, "subject");
		builder.addEdge(1, 2, "object");
		builder.addEdge(1, 6, "instrument");
		builder.addEdge(6, 5, "colour");

		assertEquals(builder.length(), 7);
		Value setval = ValueFactory
				.create("[John/name, sees, Anne/name, with, a, red, telescope]");
		assertEquals(new HashSet<Value>(builder.getSubValues()),
				setval.getSubValues());
		assertTrue(builder.contains(ValueFactory.create("Anne")));
		assertEquals(3, builder.getRoots().size());
		builder.pruneIsolatedNodes();
		assertEquals(builder.length(), 5);
		assertEquals(1, builder.getRoots().size());
		setval = ValueFactory.create("[John/name, sees, Anne/name, red, telescope]");
		assertEquals(new HashSet<Value>(builder.getSubValues()),
				setval.getSubValues());
		assertEquals(builder.toString(),
				"[sees subject>John/name object>Anne/name instrument>[telescope colour>red]]");

		RelationalVal builder2 = new RelationalVal();
		builder2.addNode("John", attrs);
		builder2.addNode("sees");
		builder2.addNode("Anne", attrs);
		builder2.addNode("red");
		builder2.addNode("telescope");
		builder2.addEdge(1, 0, "subject");
		builder2.addEdge(1, 2, "object");
		builder2.addEdge(1, 4, "instrument");
		builder2.addEdge(4, 3, "colour");
		assertEquals(builder.toString(), builder2.toString());
		assertEquals(builder, builder2);
		assertEquals(builder.hashCode(), builder2.hashCode());

		builder = new RelationalVal();
		builder.addNode("this");
		builder.addNode("is");
		builder.addNode("interesting");
		builder.addEdge(1, 0, "subject");
		builder.addEdge(1, 2, "pred");
		Value concatenation = builder2.concatenate(builder);
		assertEquals(8, concatenation.getSubValues().size());
		assertEquals(RelationalVal.class, concatenation.getClass());
		assertEquals(2, ((RelationalVal) concatenation).getRoots().size());

	}

	@Test
	public void relValTest2() {
		String simpleRel =
				"[sees subject>John/name object>Anne/name instrument>[telescope colour>red]]";
		Value val = ValueFactory.create(simpleRel);
		assertTrue(val instanceof RelationalVal);
		assertEquals(1, ((RelationalVal) val).getRoots().size());
		assertEquals(5, val.getSubValues().size());
		RelationalVal builder2 = new RelationalVal();
		List<String> attrs = new ArrayList<String>();
		attrs.add("name");
		builder2.addNode("John", attrs);
		builder2.addNode("sees");
		builder2.addNode("Anne", attrs);
		builder2.addNode("red");
		builder2.addNode("telescope");
		builder2.addEdge(1, 0, "subject");
		builder2.addEdge(1, 2, "object");
		builder2.addEdge(1, 4, "instrument");
		builder2.addEdge(4, 3, "colour");
		assertEquals(val, builder2);
		assertEquals(val.hashCode(), builder2.hashCode());

		simpleRel = "[sees\n" + "          subject>John/name\n"
				+ "          object>Anne/name\n"
				+ "          instrument>[telescope colour>red]]\n"
				+ "[is subject>this pred>interesting]";
		val = ValueFactory.create(simpleRel);
		assertTrue(val instanceof RelationalVal);

		assertEquals(2, ((RelationalVal) val).getRoots().size());
		assertEquals(8, val.getSubValues().size());
	}

	@Test
	public void newRelationsTest() {
		RelationalVal test0 = (RelationalVal) ValueFactory.create(
				"[sees subject>John/name object>Anne/name instrument>[telescope colour>red]]");
		assertEquals(test0.toString(),
				"[sees subject>John/name object>Anne/name instrument>[telescope colour>red]]");
		RelationalVal test = (RelationalVal) ValueFactory
				.create("[eat argu>Pierre argu2>[apple colour>red]]");
		assertEquals(test.toString(), "[eat argu>Pierre argu2>[apple colour>red]]");
		RelationalVal test2 = test.getSubGraph(2);
		assertEquals(test2.toString(), "[apple colour>red]");
		RelationalVal test3 = test.getSubGraph(3);
		assertEquals(test3.toString(), "red");

	}

	@Test
	public void templateTest() {
		RelationalVal test0 = new RelationalVal();
		test0.addNode("Pierre");
		test0.addNode("likes");
		test0.addNode("his");
		test0.addNode("ties");
		test0.addEdge(1, 0, "subj");
		test0.addEdge(1, 3, "obj");
		test0.addEdge(3, 2, "poss");
		test0.addEdge(2, 0, "ref");
		Template t = Template.create("likes subj>{Name}");
		assertEquals("SemgrexTemplate", t.getClass().getSimpleName());
		assertTrue(t.partialmatch(test0.toString()).isMatching());
	}
}
