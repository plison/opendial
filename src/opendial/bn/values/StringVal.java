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

import opendial.arch.Logger;
import opendial.datastructs.Template;

/**
 * String value.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */

public final class StringVal implements Value {
	
	public static Logger log = new Logger("StringVal", Logger.Level.DEBUG);

	// the string
	final String str;
	Template template;
	
	/**
	 * Creates a new string value
	 * (protected, use the ValueFactory instead)
	 * 
	 * @param str the string
	 */
	public StringVal(String str) { 
		this.str = str.trim(); 
	//	StringUtils.checkForm(str); 
	};
	
	
	/**
	 * Returns the hashcode for the string
	 *
	 * @return the hashcode
	 */
	@Override
	public int hashCode() { return str.hashCode(); }
	
	
	
	/**
	 * Returns true if the strings are equals, false otherwise
	 *
	 * @param o the object to compare
	 * @return true if equals, false otherwise
	 */
	@Override
	public boolean equals (Object o) {
		if (o instanceof StringVal) {
			StringVal stringval = (StringVal)o;
			if (stringval.str.equalsIgnoreCase(str)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the string itself
	 * 
	 * @return the string
	 */
	public String getString() {return str; }
	
	/**
	 * Returns a copy of the string value
	 *
	 * @return the copy
	 */
	@Override
	public StringVal copy() { return new StringVal(str); }
	
	/**
	 * Returns the string itself
	 *
	 * @return the string
	 */
	@Override
	public String toString() { return str; }
	
	
	/**
	 * Compares the string value to another value
	 * 
	 * @return usual ordering, or hashcode if the value is not a string
	 */
	@Override
	public int compareTo(Value o) {
		if (o instanceof StringVal) {
			return str.compareTo(((StringVal)o).str);
		}
		else {
			return hashCode() - o.hashCode();
		}
	}
	
	
	/**
	 * Returns the concatenation of the two values.  
	 */
	public Value concatenate (Value v) {
		if (v instanceof StringVal) {
			return ValueFactory.create(str + " " + v.toString());
		}
		else if (v instanceof DoubleVal) {
			return ValueFactory.create(str + " " + v.toString());			
		}
		else if (v instanceof NoneVal) {
			return this;
		}
		else {
			log.warning("cannot concatenate " + this + " and " + v);
			return ValueFactory.noneValue;
		}
	}


	/**
	 * Returns true if subvalue is a substring of the current StringVal, and
	 * false otherwise
	 * 
	 * @return true is subvalue is a substring of the object, false otherwise
	 */
	@Override
	public boolean contains(Value subvalue) {
		if (subvalue instanceof StringVal) {
			StringVal stringval = (StringVal)subvalue;
			if (stringval.template == null) {
				stringval.template = new Template(stringval.str);
			}
			return stringval.template.match(str,false).isMatching();
		}
		return false;
	}
	
}
