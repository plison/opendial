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


/**
 * Representation of a boolean value.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
 public final class BooleanVal implements Value {
	
	 // the boolean
	final boolean b;
	
	/**
	 * Creates the boolean value
	 * (protected, use the ValueFactory to create it)
	 * 
	 * @param b the boolean
	 */
	protected BooleanVal(boolean b) { this.b = b; };
	
	/**
	 * Returns the hashcode of the boolean
	 *
	 * @return hashcode
	 */
	@Override
	public int hashCode() { return new Boolean(b).hashCode(); }
	
	/**
	 * Returns true if the boolean value is similar, false otherwise
	 *
	 * @param o the value to compare
	 * @return true if similar, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		return (o instanceof BooleanVal && ((BooleanVal)o).getBoolean() == getBoolean());
	}
	
	/**
	 * Returns the boolean value 
	 * 
	 * @return the boolean value
	 */
	public boolean getBoolean() {return b; }
	
	/**
	 * Copies the boolean value
	 *
	 * @return the copy
	 */
	@Override
	public BooleanVal copy() { return new BooleanVal(b); }
	
	/**
	 * Returns a string representation of the boolean value
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() { return ""+b; }

	 
	/**
	 * Compares the boolean to another value
	 * 
	 * @return usual ordering, or hashcode difference if the value is not a boolean
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof BooleanVal) {
			return (new Boolean(b)).compareTo(((BooleanVal)o).getBoolean());
		}
		else {
			return hashCode() - o.hashCode();
		}
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}
	
}