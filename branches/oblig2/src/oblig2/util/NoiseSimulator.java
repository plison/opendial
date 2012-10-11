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

package oblig2.util;

import java.util.Random;

import oblig2.NBest;
import oblig2.NBest.Hypothesis;


/**
 * Class adding some artificial Gaussian noise to the N-Best list (since the results
 * coming from the AT&T servers don't seem to provide such information)
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NoiseSimulator {

	
	public static double MEAN = 0.7;
	public static double VARIANCE = 0.15;
	// logger
	public static Logger log = new Logger("NoiseSimulator", Logger.Level.DEBUG);
	
	
	public static NBest addNoise(NBest initNBest) {
		NBest noisyNBest = new NBest();
		
		Random random = new Random();
		
		double remainder = 1.0;
		for (Hypothesis hyp : initNBest.getHypotheses()) {
			
			// adds a Gaussian noise the confidence score
			double probability = remainder * ( MEAN + random.nextGaussian() * VARIANCE);
			
			// we have to ensure the probability is valid
			
			probability = (probability > remainder) ? remainder : (probability < 0.0)? 0.0: probability;
			noisyNBest.addHypothesis(hyp.getString(), probability);
			
			remainder = remainder - probability;
		}
		// if we have some probability mass left, assign it to an "unknown" hypothesis
		if (remainder > 0.01) {
			noisyNBest.addHypothesis("UNKNOWN", remainder);
		}
		
		return noisyNBest;
	}
	
}
