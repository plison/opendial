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

package opendial.bn.distribs;


import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.CategoricalTable;
import opendial.datastructs.Assignment;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Generic probability distribution P(X1,...,Xn) without conditional variables.
 * The probability distribution may be continuous or discrete.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 */
public interface IndependentProbDistribution extends ProbDistribution {
	
	/**
	 * Returns a sample from the probability distribution
	 * 
	 * @return the sample value
	 * @throws DialException if no value could be sampled
	 */
	public Assignment sample() throws DialException;

	/**
	 * Returns a discrete representation of the distribution as a categorical table.
	 * 
	 * @return the distribution in the format of a categorical table
	 * @throws DialException if the distribution could not be converted to a discrete form
	 */
	@Override
	public CategoricalTable toDiscrete();
	
	
	/**
	 * Returns a continuous representation of the distribution.
	 * 
	 * @return the distribution in a continuous form
	 * @throws DialException if the distribution could not be converted to a continuous form
	 */
	public ContinuousDistribution toContinuous() throws DialException;
	
	/**
	 * Returns a copy of the distribution.
	 */
	@Override
	public IndependentProbDistribution copy();
	
	
	/**
	 * Returns a set of possible values for the distribution.  If the distribution is continuous,
	 * assumes a discretised representation of the distribution.
	 * 
	 * @return the possible values for the distribution
	 */
	public abstract Set<Assignment> getValues();

	/**
	 * Generates a XML node that represents the distribution.
	 * 
	 * @param document the XML node to which the node will be attached
	 * @return the corresponding XML node
	 * @throws DialException 
	 */
	public abstract Node generateXML(Document document) throws DialException;
}

