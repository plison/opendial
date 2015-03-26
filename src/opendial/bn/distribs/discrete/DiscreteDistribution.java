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

package opendial.bn.distribs.discrete;


import opendial.arch.DialException;
import opendial.bn.distribs.ProbDistribution;
import opendial.datastructs.Assignment;


/**
 * Representation of a discrete probability distribution P(X1,...,Xn | Y1,...Y_m) where
 * X1,..., Xn are variables with a discrete range of values.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 *
 */
public interface DiscreteDistribution extends ProbDistribution {

	/**
	 * Returns the probability P(head|condition), if any is specified.  Else,
	 * returns 0.0f.
	 * 
	 * @param condition the conditional assignment for Y1,..., Ym
	 * @param head the head assignment X1,...,Xn
	 * @return the associated probability, if one exists.
	 * @throws DialException if the probability could not be extracted
	 */
	public abstract double getProb(Assignment condition, Assignment head) throws DialException ;
	
	
	/**
	 * Returns the posterior distribution P(X1,...,Xn | condition) as a categorical table.
	 * 
	 * @param condition the conditional assignment for Y1,..., Ym
	 * @return the probability table P(X1,..., Xn) given the conditional assignment
	 */
	@Override
	public abstract CategoricalTable getPosterior(Assignment condition) throws DialException;

	
	/**
	 * Copies the distribution
	 * 
	 * @return the copy of the discrete distribution
	 */
	@Override
	public abstract DiscreteDistribution copy();
	
}
