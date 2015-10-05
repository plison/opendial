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
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURTAGE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.bn.values;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.stanford.nlp.international.Language;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;

public class RelationalVal implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	SemanticGraph graph;

	int cachedHashCode = 0;

	public RelationalVal() {
		graph = new SemanticGraph();
	}

	public RelationalVal(String string) {
		this.graph = new SemanticGraph();
		StringBuilder current = new StringBuilder();
		int openedBrackets = 0;
		recordRelations(string);

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (openedBrackets == 0 && !current.toString().trim().isEmpty()
					&& (c == ' ' || c == '[' || i == string.length() - 1)) {
				addGraph(SemanticGraph.valueOf(current.toString()));
				current = new StringBuilder();
			}
			current.append(c);
			if (c == '[') {
				openedBrackets++;
			}
			else if (c == ']') {
				openedBrackets--;
				if (openedBrackets == 0 && current.length() > 2) {
					addGraph(SemanticGraph.valueOf(current.toString()));
					current = new StringBuilder();
				}
			}
		}
	}

	public void addGraph(SemanticGraph newGraph) {
		int oldGraphSize = graph.size();
		for (IndexedWord iw : newGraph.vertexListSorted()) {
			IndexedWord copy = new IndexedWord(iw);
			copy.setIndex(graph.size());
			graph.addVertex(copy);
		}
		for (SemanticGraphEdge edge : newGraph.edgeListSorted()) {
			int dep = edge.getDependent().index() + oldGraphSize;
			int gov = edge.getGovernor().index() + oldGraphSize;
			GrammaticalRelation rel = edge.getRelation();
			addEdge(gov, dep, rel.getLongName());
		}
		cachedHashCode = 0;
	}

	public int addNode(String value) {
		CoreLabel label = new CoreLabel();
		label.setWord(value);
		label.setValue(value);
		IndexedWord fword = new IndexedWord(label);
		fword.setIndex(graph.size());
		graph.addVertex(fword);
		cachedHashCode = 0;
		return fword.index();
	}

	public int addNode(String value, List<String> attributes) {
		int index = addNode(value);
		IndexedWord label = graph.vertexListSorted().get(graph.size() - 1);
		String joinedAttrs = String.join("|", attributes);
		label.setTag(joinedAttrs);
		graph.resetRoots();
		cachedHashCode = 0;
		return index;
	}

	public void pruneIsolatedNodes() {
		for (IndexedWord iw : graph.vertexListSorted()) {
			if (graph.inDegree(iw) == 0 && graph.outDegree(iw) == 0) {
				graph.removeVertex(iw);
			}
		}
		graph.resetRoots();
		cachedHashCode = 0;
	}

	public void addEdge(int start, int end, String label) {
		if (start < 0 || end < 0) {
			throw new RuntimeException("Node index must be >= 0");
		}
		else if (start >= graph.size() || end >= graph.size()) {
			throw new RuntimeException(
					"Illegal node index (size=" + graph.size() + ")");
		}
		IndexedWord startWord = graph.vertexListSorted().get(start);
		IndexedWord endWord = graph.vertexListSorted().get(end);
		GrammaticalRelation relation = recordRelation(label);
		graph.addEdge(startWord, endWord, relation, 1.0, false);
		graph.resetRoots();
		cachedHashCode = 0;
	}

	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	@Override
	public RelationalVal copy() {
		RelationalVal val = new RelationalVal();
		val.graph = new SemanticGraph(graph);
		return val;
	}

	public RelationalVal getSubGraph(int i) {
		RelationalVal val = new RelationalVal();
		val.graph = new SemanticGraph(graph);
		val.graph.setRoot(val.graph.getNodeByIndex(i));
		return val;
	}

	@Override
	public boolean contains(Value subvalue) {
		for (IndexedWord word : graph.vertexSet()) {
			if (word.toString().contains(subvalue.toString())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<Value> getSubValues() {
		List<Value> subvals = new ArrayList<Value>();
		for (IndexedWord word : graph.vertexSet()) {
			subvals.add(ValueFactory.create(word.toString()));
		}
		return subvals;
	}

	public String getValue(int index) {
		if (index < 0 || index >= graph.size()) {
			throw new RuntimeException(
					index + " is out of bounds (max:" + graph.size() + ")");
		}
		return graph.vertexListSorted().get(index).value();
	}

	public List<String> getAttributes(int index) {
		if (index < 0 || index >= graph.size()) {
			throw new RuntimeException(
					index + " is out of bounds (max:" + graph.size() + ")");
		}
		String tag = graph.vertexListSorted().get(index).tag();
		List<String> tags = new ArrayList<String>();
		if (tag != null) {
			tags.addAll(Arrays.asList(tag.split("\\|")));
		}
		return tags;
	}

	public List<Integer> getRoots() {
		return graph.getRoots().stream().map(iw -> iw.index())
				.collect(Collectors.toList());
	}

	public String getRootValue() {
		return graph.getRoots().iterator().next().value();
	}

	@Override
	public Value concatenate(Value value) {

		if (value instanceof RelationalVal) {
			RelationalVal newVal = new RelationalVal();
			newVal.addGraph(graph);
			newVal.addGraph(((RelationalVal) value).graph);
			return newVal;
		}
		else if (value instanceof NoneVal) {
			return copy();
		}
		throw new RuntimeException("cannot concatenate RelationalVal and "
				+ value.getClass().getName());
	}

	@Override
	public int length() {
		return graph.vertexSet().size();
	}

	public SemanticGraph getGraph() {
		return graph;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof RelationalVal) {
			RelationalVal val2 = (RelationalVal) o;
			if (val2.graph.equals(graph)) {
				return true;
			}
			else if (graph.size() == val2.graph.size()
					&& graph.getRoots().size() == val2.graph.getRoots().size()
					&& graph.edgeListSorted().size() == val2.graph.edgeListSorted()
							.size()) {
				return toString().equals(val2.toString());
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = toString().hashCode();
		}
		return cachedHashCode;

	}

	@Override
	public String toString() {
		String str = graph.toCompactString(true);
		return str.replaceAll("/null(\\w)", " $1").replace("/null", "");
	}

	public boolean isEmpty() {
		return graph.isEmpty();
	}

	final static Pattern relPattern = Pattern.compile("\\s(\\w+)\\>");

	private static void recordRelations(String graphstr) {
		Matcher m = relPattern.matcher(graphstr);
		while (m.find()) {
			recordRelation(m.group(1));
		}
	}

	static Map<String, GrammaticalRelation> relations =
			new HashMap<String, GrammaticalRelation>();

	private static GrammaticalRelation recordRelation(String rel) {
		return relations.computeIfAbsent(rel,
				l -> new GrammaticalRelation(Language.Any, l, l,
						GrammaticalRelation.ROOT));
	}

}
