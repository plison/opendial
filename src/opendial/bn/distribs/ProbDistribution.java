// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.bn.distribs;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import opendial.arch.DialException;
import opendial.bn.distribs.continuous.ContinuousDistribution;
import opendial.bn.distribs.discrete.DiscreteDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.ValueRange;

/**
 * Generic probability distribution of the type (PX1,...Xn | Y1,...Yn). The distribution
 * may be discrete or continuous.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
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
