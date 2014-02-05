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

package opendial.inference;


import opendial.arch.DialException;
import opendial.bn.BNetwork;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.utility.UtilityTable;
import opendial.inference.queries.ProbQuery;
import opendial.inference.queries.ReductionQuery;
import opendial.inference.queries.UtilQuery;

/**
 * Generic interface for probabilistic inference algorithms. Three distinct types of queries
 * are possible: <ul>
 * <li> probability queries of the form P(X1,...,Xn)
 * <li> utility queries of the form U(X1,...,Xn)
 * <li> reduction queries where a Bayesian network is reduced to a subet of variables X1,...,Xn
 * </ul>
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public interface InferenceAlgorithm {

	
	/**
	 * Computes the probability distribution for the query variables given the 
	 * provided evidence, all specified in the query
	 * 
	 * @param query the full query
	 * @return the resulting probability distribution
	 */
	public IndependentProbDistribution queryProb (ProbQuery query) throws DialException;
	
	
	/**
	 * Computes the utility distribution for the action variables, given the provided
	 * evidence.
	 * 
	 * @param query the full query
	 * @return the resulting utility table
	 */
	public UtilityTable queryUtil (UtilQuery query) throws DialException;

	
	/**
	 * Estimates the probability distribution on a reduced subset of variables
	 * 
	 * @param reductionQuery the reduction query
	 * @return the reduced network
	 */
	public BNetwork reduce(ReductionQuery reductionQuery) throws DialException;
	
	
}
