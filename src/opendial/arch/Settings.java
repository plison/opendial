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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import opendial.arch.Logger;
import opendial.arch.Settings.PlanSettings;
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
	
	
	static Settings settings ;
	
	public static Settings getInstance() {
		if (settings == null) {
			settings = new Settings();
		}
		return settings;
	}
	
	
	public static void loadSettings (Settings newSettings) {
		settings = newSettings;
	}
	
	public Class<? extends InferenceAlgorithm> inferenceAlgorithm = SwitchingAlgorithm.class;;
	
	public int nbSamples = 5000;
	
	// maximum sampling time (in milliseconds)
	public long maximumSamplingTime = 2500;
	
	public int nbDiscretisationBuckets = 100;
			 
	public boolean activatePlanner = true;
	
	public boolean activatePruning = true;
	
	public PlanSettings planning = new PlanSettings();
	
	public GUISettings gui = new GUISettings();
		
	
	public class PlanSettings {	
		
		public int horizon = 1;
		public double discountFactor = 0.8;
		
		Map<String,PlanSettings> specifics = new HashMap<String,PlanSettings>();

		public PlanSettings() { }
		
		public PlanSettings (int horizon, double discountFactor) {
			this.horizon = horizon;
			this.discountFactor = discountFactor;
		}	
		
		public int getHorizon(Collection<String> actionVars) {
			for (String actionVar : actionVars) {
				String actionVar2 = actionVar.replace("'", "");
				if (specifics.containsKey(actionVar2)) {
					return specifics.get(actionVar2).horizon;
				}
			}
			return horizon;
		}
		
		public double getDiscountFactor(Collection<String> actionVars) {
			for (String actionVar : actionVars) {
				String actionVar2 = actionVar.replace("'", "");
				if (specifics.containsKey(actionVar2)) {
					return specifics.get(actionVar2).discountFactor;
				}
			}
			return discountFactor;
		}

		public void addSpecific(String variable, PlanSettings planSettings) {
			specifics.put(variable, planSettings);
		}
	}
	
	
	
	
	public class GUISettings {
		
		public boolean showGUI = false;

		public String userUtteranceVar = "u_u";
		
		public String systemUtteranceVar = "u_m";
		
		public List<String> varsToMonitor;

		public GUISettings() {
			varsToMonitor = new LinkedList<String>();
		}
		
		public void setUserUtteranceVar (String userUtteranceVar) {
			this.userUtteranceVar = userUtteranceVar;
		}
		
		public void setSystemUtteranceVar (String systemUtteranceVar) {
			this.systemUtteranceVar = systemUtteranceVar;
		}
		
		public void addVariableToMonitor(String variableToMonitor) {
			varsToMonitor.add(variableToMonitor);
		}
	}
}
