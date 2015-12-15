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

package opendial.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import opendial.bn.values.RelationalVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.datastructs.Graph;

/**
 * Template for a relational structure. Both the node content and relations can be
 * "templated" (i.e. underspecified). See the Graph class for more details on the
 * string representation.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class RelationalTemplate extends Graph<Template, Template>
		implements Template {

	// Slots in the template
	final Set<String> slots;

	/**
	 * Creates a new template from a string representation
	 * 
	 * @param string
	 */
	public RelationalTemplate(String string) {
		super(string);
		slots = new HashSet<String>();
		for (Node node : getNodes()) {
			slots.addAll(node.getContent().getSlots());
			for (Template relation : node.getRelations()) {
				slots.addAll(relation.getSlots());
			}
		}
	}

	/**
	 * Returns the result of a full match between the template and the string. (this
	 * is performed using an equivalent regex template).
	 */
	@Override
	public MatchResult match(String str) {
		RegexTemplate template = new RegexTemplate(toString());
		return template.match(str);
	}

	/**
	 * Searches for the occurrences of the relational template in the string (if the
	 * string is itself a relational structure). Else returns an empty list.
	 */
	@Override
	public List<MatchResult> find(String str, int maxResults) {
		Value val = ValueFactory.create(str);
		if (val instanceof RelationalVal) {
			return getMatches((RelationalVal) val);
		}
		return Collections.emptyList();
	}

	/**
	 * Returns true if all slots are filled by the assignment, else returns false.
	 */
	@Override
	public boolean isFilledBy(Assignment input) {
		return input.containsVars(slots);
	}

	/**
	 * Fills the slots in the template and returns the result.
	 */
	@Override
	public String fillSlots(Assignment fillers) {
		fillers.filterValues(v -> !v.toString().equals(""));
		RegexTemplate template = new RegexTemplate(toString());
		return template.fillSlots(fillers);
	}

	/**
	 * Returns the slots in the template
	 */
	@Override
	public Set<String> getSlots() {
		return slots;
	}

	/**
	 * Returns true
	 */
	@Override
	public boolean isUnderspecified() {
		return true;
	}

	/**
	 * Returns the list of occurrences of the template in the relational structure.
	 * 
	 * @param relVal the relational structure to search in
	 * @return the corresponding matches for the template
	 */
	public List<MatchResult> getMatches(RelationalVal relVal) {
		List<MatchResult> results = new ArrayList<MatchResult>();
		for (Node root : getRoots()) {
			for (RelationalVal.Node n : relVal.getNodes()) {
				results.addAll(getMatches(root, n));
			}
		}
		return results;
	}

	/**
	 * Returns the list of possible matches between the template node and a node
	 * inside a relational value.
	 * 
	 * @param tNode the node in the template
	 * @param vNode the node in the relational value
	 * @return the list of possible matches (may be empty)
	 */
	private static List<MatchResult> getMatches(Node tNode,
			RelationalVal.Node vNode) {

		// first checks whether the contents are matching
		MatchResult contentMatch =
				tNode.getContent().match(vNode.getContent().toString());
		if (!contentMatch.isMatching) {
			return Collections.emptyList();
		}

		// checks whether the attributes are matching
		for (String attr : tNode.getAttributes()) {
			if (!vNode.getAttributes().contains(attr)) {
				return Collections.emptyList();
			}
			Template attrVal = tNode.getAttrValue(attr);
			Value vVal = vNode.getAttrValue(attr);
			MatchResult attrMatch = attrVal.match(vVal.toString());
			if (!attrMatch.isMatching) {
				return Collections.emptyList();
			}
			contentMatch.addAssignment(attrMatch);
		}

		// creates for each template relation a list of possible matches
		List<List<MatchResult>> allRelResults = new ArrayList<List<MatchResult>>();
		for (Template rel : tNode.getRelations()) {
			List<MatchResult> relResults =
					getMatches(rel, tNode.getChild(rel), vNode);
			if (relResults.isEmpty()) {
				return Collections.emptyList();
			}
			else {
				allRelResults.add(relResults);
			}
		}

		// generates the combination of all matches
		allRelResults.add(Arrays.asList(contentMatch));
		List<MatchResult> results = flattenResults(allRelResults);

		return results;
	}

	/**
	 * Searches for all children of vNode that satisfy the template relation rel and
	 * also match the template node tSubNode.
	 * 
	 * @param rel the template relation
	 * @param tSubNode the template node
	 * @param vNode the node in the relational value
	 * @return the list of corresponding matches
	 */
	private static List<MatchResult> getMatches(Template rel, Node tSubNode,
			RelationalVal.Node vNode) {
		List<MatchResult> relResults = new ArrayList<MatchResult>();

		if (rel.toString().equals("+")) {
			for (RelationalVal.Node descendant : vNode.getDescendants()) {
				List<MatchResult> subMatches = getMatches(tSubNode, descendant);
				relResults.addAll(subMatches);
			}
			return relResults;
		}

		for (String vrel : vNode.getRelations()) {
			MatchResult relMatch = rel.match(vrel);
			if (!relMatch.isMatching) {
				continue;
			}
			RelationalVal.Node vSubNode = vNode.getChild(vrel);
			List<MatchResult> subMatches = getMatches(tSubNode, vSubNode);
			for (MatchResult subMatch : subMatches) {
				subMatch.addAssignment(relMatch);
				relResults.add(subMatch);
			}
		}
		return relResults;
	}

	/**
	 * Creates the combination of all match results.
	 * 
	 * @param allResults the list of all results for each template relation
	 * @return the "flattened" combination of all matches
	 */
	private static List<MatchResult> flattenResults(
			List<List<MatchResult>> allResults) {
		List<MatchResult> results = new ArrayList<MatchResult>();
		results.addAll(allResults.remove(0));
		for (List<MatchResult> relResults : allResults) {
			List<MatchResult> newResults = new ArrayList<MatchResult>();
			for (MatchResult curResult : results) {
				for (MatchResult relResult : relResults) {
					MatchResult newResult = curResult.copy();
					newResult.addAssignment(relResult);
					newResults.add(newResult);
				}
			}
			results = newResults;
		}
		return results;
	}

	/**
	 * Creates a template from its string representation
	 */
	@Override
	protected Template createValue(String valueStr) {
		return Template.create(valueStr);
	}

	/**
	 * Creates a template from its string representation
	 */
	@Override
	protected Template createRelation(String relationStr) {
		return Template.create(relationStr);
	}

	/**
	 * Copies a template
	 */
	@Override
	protected Template copyValue(Template t) {
		return Template.create(t.toString());
	}
}
