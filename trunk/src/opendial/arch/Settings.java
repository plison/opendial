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

package opendial.arch;


import java.util.Arrays;
import java.util.List;

import opendial.arch.Logger;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.SwitchingAlgorithm;
import opendial.inference.VariableElimination;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Settings {

	// logger
	public static Logger log = new Logger("Settings",
			Logger.Level.NORMAL);
	
	
	public static Class<? extends InferenceAlgorithm> inferenceAlgorithm = SwitchingAlgorithm.class;;
	
	public static int nbSamples = 1000;
	
	// maximum sampling time (in milliseconds)
	public static long maximumSamplingTime = 300;
	
	public static int nbDiscretisationBuckets = 100;
	
	public static boolean showGUI;
	
	public static Settings singletonSettings;
	
	public static boolean activatePlanner = true;
	
	public static boolean activatePruning = true;
	
	public static int planningHorizon = 2;
	
	public static double discountFactor = 0.8;
	
	public static double observationUncertainty = 0.8;
	
	public static String userUtteranceVar = "u_u";
	
	public static String systemUtteranceVar = "u_m";
	
	public static List<String> varsToMonitor = Arrays.asList("i_u");
		
}
