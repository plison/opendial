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


import opendial.arch.Logger;
import opendial.inference.ImportanceSampling;
import opendial.inference.InferenceAlgorithm;
import opendial.inference.VariableElimination;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class ConfigurationSettings {

	// logger
	public static Logger log = new Logger("ConfigurationSettings",
			Logger.Level.NORMAL);
	
	
	Class<? extends InferenceAlgorithm> currentInferenceAlgorithm;
	
	int nbSamples = 1000;
	
	// maximum sampling time (in milliseconds)
	long maximumSamplingTime = 500;
	
	int nbDiscretisationBuckets = 100;
	
	boolean showGUI;
	
	static ConfigurationSettings singletonSettings;
	
	public static ConfigurationSettings getInstance() {
		if (singletonSettings == null) {
			singletonSettings = new ConfigurationSettings();
		}
		return singletonSettings;
	}

	private ConfigurationSettings() {		
		currentInferenceAlgorithm = VariableElimination.class;
	}
	
	public Class<? extends InferenceAlgorithm> getInferenceAlgorithm() {
		return currentInferenceAlgorithm;
	} 
	
	public int getNbSamples() {
		return nbSamples;
	}
	
	public int getNbDiscretisationBuckets() {
		return nbDiscretisationBuckets;
	}
	
	
	public void setInferenceAlgorithm(Class<? extends InferenceAlgorithm> algorithm) {
		log.info("Inference algorithm changed to : " + algorithm.getSimpleName());
		currentInferenceAlgorithm = algorithm;
	} 
	
	public void setNbSamples(int nbSamples) {
		this.nbSamples = nbSamples;
	}
	
	public void setMaximumSamplingtime(long maximumSamplingTime) {
		this.maximumSamplingTime = maximumSamplingTime;
	}
	
	
	public void setNbDiscretisationBuckets(int nbDiscretisationBuckets) {
		this.nbDiscretisationBuckets = nbDiscretisationBuckets;
	}
	
	public void showGUI(boolean flag) {
		showGUI = flag;
	}
	
	public boolean isGUIShown() {
		return showGUI;
	}

	/**
	 * 
	 * @return
	 */
	public long getMaximumSamplingTime() {
		return maximumSamplingTime;
	}
		
}
