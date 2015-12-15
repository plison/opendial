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

package opendial.bn.values;

import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import opendial.datastructs.Graph;

/**
 * Representation of a relational value. Its extends a graph where the nodes contains
 * Value objects, and the relation are plain strings. See the Graph class for more
 * information about the syntax for the graph.
 * 
 * @author Pierre Lison
 *
 */
public class RelationalVal extends Graph<Value, String> implements Value {

	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Creates an empty relational structure.
	 */
	public RelationalVal() {
		super();
	}

	/**
	 * Creates a relational structure from a string representation.
	 * 
	 * @param string
	 */
	public RelationalVal(String string) {
		super(string);
	}

	/**
	 * Compares the value with another one (based on their string).
	 */
	@Override
	public int compareTo(Value o) {
		return toString().compareTo(o.toString());
	}

	/**
	 * Returns true if the value is contained in the relation structure. (this is
	 * done
	 */
	@Override
	public boolean contains(Value subvalue) {
		return toString().contains(subvalue.toString());
	}

	/**
	 * Returns the collection of values in the relational structure
	 */
	@Override
	public Collection<Value> getSubValues() {
		return getNodes().stream().map(n -> n.getContent())
				.collect(Collectors.toList());
	}

	/**
	 * Concatenates two relational structures (by juxtaposing their roots).
	 */
	@Override
	public Value concatenate(Value value) {
		if (!(value instanceof RelationalVal)) {
			throw new RuntimeException(
					"Cannot concatenate " + this + " with " + value);
		}
		return new RelationalVal(toString() + value.toString());
	}

	/**
	 * Returns the number of nodes in the graph.
	 */
	@Override
	public int length() {
		return getNodes().size();
	}

	/**
	 * Copies the relational structure
	 */
	@Override
	public RelationalVal copy() {
		return new RelationalVal(toString());
	}

	/**
	 * Returns true if the structure does not contain any nodes, else false.
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return getNodes().isEmpty();
	}

	/**
	 * Creates a value from a string representation within the graph.
	 */
	@Override
	protected Value createValue(String valueStr) {
		return ValueFactory.create(valueStr);
	}

	/**
	 * Creates a relation from a string representation within the graph.
	 */
	@Override
	protected String createRelation(String relStr) {
		return relStr;
	}

	/**
	 * Copies a value
	 */
	@Override
	protected Value copyValue(Value v) {
		return v.copy();
	}

}
