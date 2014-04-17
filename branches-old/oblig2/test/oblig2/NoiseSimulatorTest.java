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

import static org.junit.Assert.*;

import org.junit.Test;

import oblig2.NBest.Hypothesis;
import oblig2.util.Logger;
import oblig2.util.NoiseSimulator;


/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NoiseSimulatorTest {


	public static Logger log = new Logger("NoiseSimulatorTest", Logger.Level.NORMAL);

	@Test
	public void noiseSimulatorTest1() {

		double addition = 0.0;
		for (int i = 0 ; i < 500 ; i++) {
			NBest nbest = new NBest();
			nbest.addHypothesis("hyp 1", 1.0f);
			nbest.addHypothesis("hyp 2", 1.0f);
			nbest.addHypothesis("hyp 3", 1.0f);

			NBest noisyNBest = NoiseSimulator.addNoise(nbest);
			double sum = 0.0;
			for (Hypothesis hyp : noisyNBest.getHypotheses()) {
				sum += hyp.getConf();
				assertTrue(hyp.getConf() >= 0.0f && hyp.getConf() <= 1.0f);
			}
			assertEquals(1.0, sum, 0.05);
			addition += noisyNBest.getHypotheses().get(0).getConf();
			log.debug("noisy nbest: " + noisyNBest);
		}
		assertEquals(NoiseSimulator.MEAN, addition / 500, 0.05);
	}

	// test order of hypothesis
	// valid probability
	// unknown hypothesis
}

