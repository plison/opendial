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

import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import opendial.utils.StringUtils;

/**
 * Representation of an array of doubles.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public final class ArrayVal implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the array of doubles
	final double[] array;
	final int hashcode;

	/**
	 * Creates a new array of doubles
	 * 
	 * @param values the array
	 */
	public ArrayVal(double[] values) {
		this.array = new double[values.length];
		for (int i = 0; i < array.length; i++) {
			array[i] = values[i];
		}
		hashcode = Arrays.hashCode(array);
	}

	/**
	 * Creates a new array of doubles
	 * 
	 * @param values the array (as a collection)
	 */
	public ArrayVal(Collection<Double> values) {
		array = values.stream().mapToDouble(d -> d).toArray();
		hashcode = Arrays.hashCode(array);
	}

	/**
	 * Compares to another value.
	 */
	@Override
	public int compareTo(Value arg0) {
		if (arg0 instanceof ArrayVal) {
			double[] otherVector = ((ArrayVal) arg0).getArray();
			if (array.length != otherVector.length) {
				return array.length - otherVector.length;
			}
			else {
				for (int i = 0; i < array.length; i++) {
					double val1 = array[i];
					double val2 = otherVector[i];

					// if the difference is very small, assume 0
					if (Math.abs(val1 - val2) > 0.0001) {
						return (new Double(val1).compareTo(new Double(val2)));
					}
				}
				return 0;
			}
		}
		return hashCode() - arg0.hashCode();
	}

	/**
	 * Copies the array
	 */
	@Override
	public Value copy() {
		return new ArrayVal(array);
	}

	/**
	 * Returns a vector with the elements of the array
	 * 
	 * @return the vector
	 */
	public Vector<Double> getVector() {
		Vector<Double> vector = new Vector<Double>(array.length);
		for (int i = 0; i < array.length; i++) {
			vector.add(array[i]);
		}
		return vector;
	}

	/**
	 * Returns the array length
	 * 
	 * @return the length
	 */
	@Override
	public int length() {
		return array.length;
	}

	/**
	 * Checks for equality
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ArrayVal) {
			return ((ArrayVal) o).getVector().equals(getVector());
		}
		return false;
	}

	/**
	 * Returns the array
	 * 
	 * @return the array
	 */
	public double[] getArray() {
		return array;
	}

	/**
	 * Returns the list of double values.
	 * 
	 * @return the list of double values
	 */
	@Override
	public List<Value> getSubValues() {
		return Arrays.stream(array).mapToObj(d -> new DoubleVal(d))
				.collect(Collectors.toList());
	}

	/**
	 * If v is an ArrayVal, returns the combined array value. Else, returns none.
	 * 
	 * @param v the value to concatenate
	 * @return the concatenated result
	 */
	@Override
	public Value concatenate(Value v) {
		if (v instanceof ArrayVal) {
			double[] newvals =
					new double[array.length + ((ArrayVal) v).getArray().length];
			for (int i = 0; i < array.length; i++) {
				newvals[i] = array[i];
			}
			for (int i = 0; i < ((ArrayVal) v).getArray().length; i++) {
				newvals[array.length + i] = ((ArrayVal) v).getArray()[i];
			}
			return new ArrayVal(newvals);
		}
		else if (v instanceof NoneVal) {
			return this;
		}
		else {
			Set<Value> corresponding =
					Arrays.stream(array).mapToObj(a -> ValueFactory.create(a))
							.collect(Collectors.toSet());
			return (new SetVal(corresponding)).concatenate(v);
		}
	}

	/**
	 * Returns the hashcode for the array
	 */
	@Override
	public int hashCode() {
		return hashcode;
	}

	/**
	 * Returns a string representation of the array
	 */
	@Override
	public String toString() {
		String s = "[";
		for (Double d : getVector()) {
			s += StringUtils.getShortForm(d) + ",";
		}
		return s.substring(0, s.length() - 1) + "]";
	}

	/**
	 * Returns false.
	 */
	@Override
	public boolean contains(Value filledValue) {
		if (filledValue instanceof DoubleVal) {
			for (double a : array) {
				if (Math.abs(a - ((DoubleVal) filledValue).d) < 0.0001) {
					return true;
				}
			}
		}
		return false;
	}

}
