
// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package oblig2;

import java.util.ArrayList;
import java.util.List;

import oblig2.util.Logger;

/**
 * Representation of an N-Best list provided by the speech recogniser
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NBest {

	// logger
	public static Logger log = new Logger("NBest", Logger.Level.NORMAL);
	
	// list of ordered hypotheses in N-Best list
	List<Hypothesis> hypotheses;
	
	/**
	 * Creates a new, empty N-Best list
	 */
	public NBest () {
		hypotheses = new ArrayList<Hypothesis>();
	}
	
	/**
	 * Creates an N-Best list with a single hypothesis
	 * 
	 * @param hyp the hypothesis
	 * @param conf its confidence score
	 */
	public NBest (String hyp, double conf) {
		this();
		addHypothesis(hyp, conf);
	}
	
	/**
	 * Adds a new hypothesis to the N-Best list
	 * 
	 * @param hyp the hypothesis
	 * @param conf its confidence score
	 */
	public void addHypothesis(String hyp, double conf) {
		hypotheses.add(new Hypothesis (hyp, conf));
	}
	
	/**
	 * Returns the list of hypotheses included in the N-Best list
	 * 
	 * @return the hypotheses
	 */
	public List<Hypothesis> getHypotheses() {
		return hypotheses;
	}
	
	/**
	 * Returns a string representation of the N-Best list
	 *
	 * @return the string
	 */
	public String toString() {
		String s = "";
		if (hypotheses.isEmpty()) {
			return "";
		}
		for (Hypothesis hyp : hypotheses) {
			s += hyp + "\n";
		}
		return s.substring(0, s.length()-1);
	}
	
	
	/**
	 * Representation of a single recognition hypothesis, made of a string 
	 * and an associated confidence score.
	 *
	 */
	public final class Hypothesis {
		
		// the string
		String hyp;
		
		// the confidence score
		double conf;
		
		/**
		 * Creates a new hypothesis
		 * 
		 * @param hyp the hypothesis
		 * @param conf the confidence score
		 */
		public Hypothesis (String hyp, double conf) {
			this.hyp = hyp;
			this.conf = conf;
		}
		
		/**
		 * Returns a string representation of the hypothesis
		 *
		 * @return the string
		 */
		public String toString() {
			String s = hyp;
			if (conf < 1.0) {
				s += " (" + conf + ")";
			}
			return s;
		}
		
		/**
		 * Returns the string hypothesis (without score)
		 * 
		 * @return the hypothesis
		 */
		public String getString() {
			return hyp;
		}
		
		
		/**
		 * Returns the confidence score for the hypothesis
		 * 
		 * @return the score
		 */
		public double getConf() {
			return conf;
		}
	}
}
