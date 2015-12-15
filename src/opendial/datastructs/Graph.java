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

package opendial.datastructs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a relational structure as a directed acyclic graph. The two
 * parameters V and R respectively express the types of values associated with each
 * node and each relation. In addition to its main content, each node can also
 * express a set of attributes (such as POS tags, named entities, timing information,
 * etc.).
 * 
 * <p>
 * The string representation of the graph is similar to the one used in the Stanford
 * NLP package. For instance, the string [loves subject>John object>Mary] represents
 * a graph with three nodes (loves, John and Mary), with a relation labelled
 * "subject" from "loves" to "John", and a relation labelled "object" between "loves"
 * and "Mary". Brackets are used to construct embedded graphs, such as for instance
 * [eats subject>Pierre object>[apple attribute>red]], which is a graph with four
 * nodes, where the node "apple" is itself the governor of the node "red".
 * 
 * <p>
 * The optional attributes are indicated via a | bar followed by a key:value pair
 * right after the node content. For instance, "loves|pos:VB" indicates that the pos
 * attribute for the node has the value "VB". To incorporate several attributes, you
 * can simply add additional | bars, like this: loves|pos:VB|index:1.
 * 
 * <p>
 * Finally, it is also possible to construct graph with more than one root by
 * including several brackets at the top level, such as [eats subject>Pierre][drinks
 * subject>Milen].
 * 
 * <p>
 * The class is abstract, and its extension requires the definition of three methods
 * that define how the values V and R can be created from string, and how values V
 * can be copied.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 * @param <V>
 * @param <R>
 */
public abstract class Graph<V, R> {

	// the roots of the graph (nodes without governors)
	final List<Node> roots;

	// all nodes in the graph
	final List<Node> nodes;

	final String string;

	final static Logger log = Logger.getLogger("OpenDial");

	static final String valueRegex =
			"([^\\[\\]\\s\\|]+)((?:\\|\\w+:[^\\[\\]\\s\\|]+)*)";
	static final Pattern graphPattern = Pattern.compile(
			"\\[" + valueRegex + "((\\s+\\S+>" + valueRegex + ")*)" + "\\]");

	/**
	 * Constructs an empty graph
	 */
	public Graph() {
		roots = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		string = "";
	}

	/**
	 * Constructs a graph from a string
	 * 
	 * @param string the string representation for the graph
	 */
	public Graph(String string) {
		roots = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		string = string.trim().replace("> ", ">");
		StringBuilder current = new StringBuilder();
		int openedBrackets = 0;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			current.append(c);
			if (c == '[') {
				openedBrackets++;
			}
			else if (c == ']') {
				openedBrackets--;
				if (openedBrackets == 0) {
					Node root = createNode(current.toString());
					roots.add(root);
				}
			}
		}
		String str = "";
		for (Node root : roots) {
			str += root.toString();
		}
		this.string = str;
	}

	/**
	 * Returns true if the string represents a relational structure, else false.
	 * 
	 * @param string the string to check
	 * @return true if the string encodes a graph, else false
	 */
	public static boolean isRelational(String string) {
		if (!string.startsWith("[") || !string.endsWith("]")
				|| !string.contains(">")) {
			return false;
		}
		return graphPattern.matcher(string).find();
	}

	/**
	 * Creates a value of type V from a string
	 * 
	 * @param valueStr the string
	 * @return the corresponding value
	 */
	protected abstract V createValue(String valueStr);

	/**
	 * Creates a value of type R from a string
	 * 
	 * @param relStr the string
	 * @return the corresponding value
	 */
	protected abstract R createRelation(String relStr);

	/**
	 * Copies the value V.
	 * 
	 * @param val the value to copy
	 * @return the copied value
	 */
	protected abstract V copyValue(V val);

	/**
	 * Returns the hashcode for the graph
	 */
	@Override
	public int hashCode() {
		return string.hashCode();
	}

	/**
	 * Returns the string representation for the graph
	 */
	@Override
	public String toString() {
		return string;
	}

	/**
	 * Returns the nodes of the graph
	 * 
	 * @return the nodes
	 */
	public List<Node> getNodes() {
		return nodes;
	}

	/**
	 * Returns the roots of the graph.
	 * 
	 * @return the roots
	 */
	public List<Node> getRoots() {
		return roots;
	}

	/**
	 * Returns true if the object is a graph with the same content.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Graph) {
			return string.equals(o.toString());
		}
		return false;
	}

	/**
	 * Creates a new node from the string representation. This method is called
	 * recursively to build the full graph structure.
	 * 
	 * @param string the string
	 * @return the corresponding node
	 */
	private Node createNode(String string) {
		Matcher m = graphPattern.matcher(string);
		while (m.find()) {

			// if we find an embedded graph, process it first
			// (and add the corresponding node to "nodes")
			if (m.start() > 0 || m.end() < string.length()) {
				Node subnode = createNode(m.group());
				string = string.substring(0, m.start())
						+ System.identityHashCode(subnode)
						+ string.substring(m.end(), string.length());
				m = graphPattern.matcher(string);
			}

			else {
				// creates the node
				String content = m.group(1);
				Node newNode = new Node(createValue(content));

				// adds the attributes
				String attrs = m.group(2);
				attrs = (!attrs.isEmpty()) ? attrs.substring(1, attrs.length())
						: attrs;
				for (String attr : attrs.split("\\|")) {
					if (attr.length() == 0) {
						continue;
					}
					String[] attrsplit = attr.split(":");
					newNode.addAttribute(attrsplit[0], createValue(attrsplit[1]));
				}

				// adds the relations
				for (String rel : m.group(3).split(" ")) {
					if (rel.length() == 0 || !rel.contains(">")) {
						continue;
					}
					String[] relsplit = rel.split(">");
					R relation = createRelation(relsplit[0]);
					String relcontent = relsplit[1];

					// if the subnode actually refers to an embedded node that was
					// already created, simply link the two. Else, creates a new node
					Optional<Node> opt = nodes.stream()
							.filter(n -> relcontent
									.equals("" + System.identityHashCode(n)))
							.findAny();
					Node subNode = (opt.isPresent()) ? opt.get()
							: createNode("[" + relcontent + "]");
					newNode.addChild(relation, subNode);
				}
				nodes.add(0, newNode);
				return newNode;
			}
		}
		throw new RuntimeException("could not create node from " + string);
	}

	/**
	 * Representation of an individual node, with a content, optional attributes and
	 * outgoing relations.
	 *
	 */
	public final class Node {

		// the node content
		V content;

		// the node attributes
		SortedMap<String, V> attributes;

		// the node children
		SortedMap<R, Node> children;

		/**
		 * Creates a new node with the given content
		 * 
		 * @param content the content
		 */
		public Node(V content) {
			this.content = content;
			children = new TreeMap<R, Node>();
			attributes = new TreeMap<String, V>();
		}

		/**
		 * Adds a new attribute to the node
		 * 
		 * @param attr the attribute label
		 * @param V the corresponding value
		 */
		public void addAttribute(String attr, V V) {
			attributes.put(attr, V);
		}

		/**
		 * Adds a new outgoing relation to the node. Throws an exception if a cycle
		 * is found.
		 * 
		 * @param relation the relation label
		 * @param node the dependent node
		 */
		public void addChild(R relation, Node node) {
			if (node.getDescendants().contains(this)) {
				throw new RuntimeException(
						"cannot add " + node + " as child of " + this);
			}
			children.put(relation, node);
		}

		/**
		 * Returns the node content
		 * 
		 * @return
		 */
		public V getContent() {
			return content;
		}

		/**
		 * returns the relation labels going out of the node.
		 * 
		 * @return the set of labelled relations
		 */
		public Set<R> getRelations() {
			return children.keySet();
		}

		/**
		 * Returns the node that is a child of the current node through the given
		 * relation. Returns null if the child cannot be found.
		 * 
		 * @param relation the labelled relation
		 * @return the corresponding child node
		 */
		public Node getChild(R relation) {
			return children.get(relation);
		}

		/**
		 * Returns the set of attribute keys.
		 * 
		 * @return the keys
		 */
		public Set<String> getAttributes() {
			return attributes.keySet();
		}

		/**
		 * Returns the attribute value for the given key, if it exists. Else returns
		 * null.
		 */
		public V getAttrValue(String attr) {
			return attributes.get(attr);
		}

		/**
		 * Returns the set of children nodes
		 * 
		 * @return the children nodes
		 */
		public Collection<Node> getChildren() {
			return children.values();
		}

		/**
		 * Returns the set of all descendant nodes.
		 * 
		 * @return the descendant nodes.
		 */
		public Set<Node> getDescendants() {
			Set<Node> descendants = new HashSet<Node>();
			Stack<Node> toProcess = new Stack<Node>();
			toProcess.addAll(children.values());
			while (!toProcess.isEmpty()) {
				Node n = toProcess.pop();
				descendants.add(n);
				toProcess.addAll(n.children.values());
			}
			return descendants;
		}

		/**
		 * Copies the node.
		 * 
		 * @return the copy
		 */
		public Node copy() {
			Node copy = new Node(copyValue(content));
			for (String attr : attributes.keySet()) {
				copy.addAttribute(attr, copyValue(attributes.get(attr)));
			}
			for (R child : children.keySet()) {
				copy.addChild(child, children.get(child).copy());
			}
			return copy;
		}

		/**
		 * Merges the node with another one (if two values are incompatible, the
		 * content of the other node takes precedence).
		 * 
		 * @param otherGraphNode the other node
		 * @return the merged node
		 */
		public Node merge(Node otherGraphNode) {
			Node mergedNode = otherGraphNode.copy();
			for (String attr : attributes.keySet()) {
				mergedNode.addAttribute(attr, copyValue(attributes.get(attr)));
			}
			for (R child : children.keySet()) {
				if (mergedNode.children.containsKey(child)) {
					Node mergedChildNode = mergedNode.children.get(child);
					mergedChildNode = children.get(child).merge(mergedChildNode);
					mergedNode.addChild(child, mergedChildNode);
				}
				else {
					mergedNode.addChild(child, children.get(child));
				}
			}
			return mergedNode;
		}

		/**
		 * Returns a string representation of the node and its descendants
		 */
		@Override
		public String toString() {
			String str = content.toString();
			for (String attr : attributes.keySet()) {
				str += "|" + attr + ":" + attributes.get(attr).toString();
			}
			for (R child : children.keySet()) {
				str += " " + child + ">" + children.get(child).toString();
			}
			return (children.isEmpty()) ? str : "[" + str + "]";
		}
 
		/**
		 * Returns true if the
		 */
		@Override
		public boolean equals(Object o) {
			if (o instanceof Graph.Node) {
				Graph<?,?>.Node n = (Graph<?,?>.Node)o;
				return content.equals(n.content) && children.equals(n.children)
						&& attributes.equals(n.attributes);
			}
			return false;
		}

		/**
		 * Returns the hashcode for the node
		 */
		@Override
		public int hashCode() {
			return content.hashCode() - children.hashCode() + attributes.hashCode();
		}
	}
}
