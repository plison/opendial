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

import java.util.Collection;
import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;

/**
 * Generic probability distribution of the type (PX1,...Xn | Y1,...Yn). The distribution
 * may be discrete or continuous.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-03-20 21:16:08 #$
 *
 */
public interface ProbDistribution  {

	/** possible types of distribution: discrete, continuous or a combination of the two */
	public enum DistribType {DISCRETE, CONTINUOUS, HYBRID}
	
	
	/**
	 * Checks that the probability distribution is well-formed (all assignments are covered,
	 * and the probabilities add up to 1.0f)
	 * 
	 * @return true is the distribution is well-formed, false otherwise
	 */
	public boolean isWellFormed();
	
	
	/**
	 * Creates a copy of the probability distribution
	 * 
	 * @return the copy
	 */
	public ProbDistribution copy();


	
	/**
	 * Changes a variable label in the distribution
	 * 
	 * @param oldId the old variable label
	 * @param newId the new variable label
	 */
	public void modifyVariableId(String oldId, String newId);
	
	
	/**
	 * Returns the labels for the random variables the distribution is defined on.
	 * 
	 * @return the collection of variable labels
	 */
	public Collection<String> getHeadVariables();

	/**
	 * Returns a sample value for the distribution given a particular conditional
	 * assignment.
	 * 
	 * @param condition the conditional assignment for Y1,...,Ym
	 * @return the sampled values for the head assignment X1,...,Xn.
	 * @throws DialException if no value(s) could be sampled
	 */
	public Assignment sample(Assignment condition) throws DialException;
	
	/**
	 * Returns the discrete form of the distribution.  If the current distribution
	 * has a continuous range, the returned distribution will be a discretised conversion
	 * of the current one.
	 * 
	 * @return the discretised form of the distribution
	 * @throws DialException 
	 */
	public DiscreteDistribution toDiscrete() throws DialException;
	
	
	
	/**
	 * Returns the preferred representation format for the distribution.
	 * 
	 * @return the preferred representation format
	 */
	public DistribType getPreferredType() ;
	
	
	/**
	 * Returns the probability table for the head variables, given the
	 * conditional assignment given as argument.  This method requires
	 * a full assignment of values to the conditional variables of the
	 * current distribution.
	 * 
	 * <p>If a head variable has a continuous range, the values defined
	 * in the table are based on a discretisation procedure which creates
	 * a sequence of buckets, each with an approximatively similar
	 * probability mass.
	 * 
	 * @param condition the assignment for the conditional variable
	 * @return the resulting probability table
	 * @throws DialException 
	 */
	public abstract IndependentProbDistribution getPosterior(Assignment condition) throws DialException ;
	
	/**
	 * Returns a new probability distribution that is the posterior of the
	 * current distribution, given the conditional assignment as argument.
	 * 
	 * @param condition an assignment of values to the conditional variables
	 * @return the posterior distribution
	 * @throws DialException 
	 */	
	public abstract ProbDistribution getPartialPosterior(Assignment condition) throws DialException;
	
	
	/**
	 * Returns the set of possible values for the distribution, given a set of possible values 
	 * for the conditional variables. If the distribution is continuous, the method returns
	 * a discretised set. 
	 * 
	 * @param range possible values for the conditional variables
	 * @return a set of assignments for the head variables 
	 * @throws DialException 
	 */
	public abstract Set<Assignment> getValues(ValueRange range) throws DialException;

	
	/**
	 * Prunes values whose frequency in the distribution is lower than the given threshold. 
	 * 
	 * @param threshold the threshold to apply for the pruning
	 */
	public void pruneValues(double threshold);
	
}
