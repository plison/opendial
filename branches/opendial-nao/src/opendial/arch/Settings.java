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

package opendial.arch;


import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import opendial.arch.Logger;
import opendial.modules.Module;
import opendial.readers.XMLSettingsReader;

/**
 * System-wide settings for openDial.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class Settings {

	// logger
	public static Logger log = new Logger("Settings", Logger.Level.DEBUG);
	
	/** Default settings */
	public static final String SETTINGS_FILE = "resources//settings.xml";

	/** maximum number of samples to use for likelihood weighting */
	public static int nbSamples = 1000;
	
	/** maximum sampling time (in milliseconds) */
	public static long maxSamplingTime = 250 ;
	 
	/** Number of discretisation buckets to convert continuous distributions */
	public static int discretisationBuckets = 50;

	/** Whether to show the GUI */
	public boolean showGUI;

	/** Variable label for the user input */
	public String userInput;
	
	/** Variable label for the system output */
	public String systemOutput;
		
	/** Other variables to monitor in the chat window */
	public List<String> varsToMonitor = new ArrayList<String>();

	/** Planning horizon */
	public int horizon;

	/** Discount factor for forward planning */
	public double discountFactor;
	
	/** Recording types */
	public static enum Recording {NONE, LAST_INPUT, ALL}
	
	/** Whether to record intermediate dialogue state */
	public Recording recording;
	
	/** Other parameters */
	public Map<String,String> params = new HashMap<String, String>();

	/** Domain-specific modules to run */
	public Collection<Class<Module>> modules = new ArrayList<Class<Module>>();
	
	
	/**
	 * Creates new settings with the default values
	 */
	public Settings() {
		fillSettings(XMLSettingsReader.extractMapping(SETTINGS_FILE));
	}
	
	/**
	 * Creates a new settings with the values provided as argument.  Values
	 * that are not explicitly specified in the mapping are set to their
	 * default values.
	 * 
	 * @param mapping the properties
	 */
	public Settings(Map<String,String> mapping) {
		fillSettings(XMLSettingsReader.extractMapping(SETTINGS_FILE));
		fillSettings(mapping);	
	}
		
	
	/**
	 * Fills the current settings with the values provided as argument. 
	 * Existing values are overridden.
	 * 
	 * @param mapping the properties
	 */
	public void fillSettings(Map<String,String> mapping) {

		for (String key : mapping.keySet()) {
			if (key.equalsIgnoreCase("horizon")) {
				horizon = Integer.parseInt(mapping.get(key));
			}
			else if (key.equalsIgnoreCase("discount")) {
				discountFactor = Double.parseDouble(mapping.get(key));
			}

			else if (key.equalsIgnoreCase("gui")) {
				showGUI = Boolean.parseBoolean(mapping.get(key));
			}
			else if (key.equalsIgnoreCase("user")) {
				userInput = mapping.get(key);
			}
			else if (key.equalsIgnoreCase("system")) {
				systemOutput = mapping.get(key);
			}
			else if (key.equalsIgnoreCase("monitor")) {
				String[] split = mapping.get(key).split(",");
				for (int i = 0 ; i < split.length ; i++) {
					if (split[i].trim().length() > 0) {
					varsToMonitor.add(split[i].trim());
					}
				}
			}
			else if (key.equalsIgnoreCase("samples")) {
				nbSamples = Integer.parseInt(mapping.get(key));
			}
			else if (key.equalsIgnoreCase("timeout")) {
				maxSamplingTime = Integer.parseInt(mapping.get(key));
			}
			else if (key.equalsIgnoreCase("discretisation")) {
				discretisationBuckets = Integer.parseInt(mapping.get(key));
			}
			else if (key.equalsIgnoreCase("recording")) {
				if (mapping.get(key).trim().equalsIgnoreCase("last") ) {
					recording = Recording.LAST_INPUT;
				}
				else if (mapping.get(key).trim().equalsIgnoreCase("all")) {
					recording = Recording.ALL;
				}
				else {
					recording = Recording.NONE;
				}
			}
			else if (key.equalsIgnoreCase("modules")) {
				String[] split = mapping.get(key).split(",");
				for (int i = 0 ; i < split.length ; i++) {
					if (split[i].trim().length() > 0) {
						Class<?> clazz;
						try {
							clazz = Class.forName(split[i].trim());
							for (int j = 0 ; j < clazz.getInterfaces().length ; j++) {
								if (clazz.getInterfaces()[i].equals(Module.class)) {
									modules.add((Class<Module>)clazz);
								}
							}
						} catch (ClassNotFoundException e) {
							log.warning("class not found: " + split[i].trim());
						}
					}
				}
			}
			else {
				params.put(key, mapping.get(key));
			}
		}
	}
	
	
	/**
	 * Returns a representation of the settings in terms of a mapping
	 * between property labels and values
	 * 
	 * @return the corresponding mapping
	 */
	public Map<String,String> getFullMapping() {
		Map<String,String> mapping = new HashMap<String,String>();
		mapping.putAll(params);
		mapping.put("horizon", ""+horizon);
		mapping.put("discount", ""+discountFactor);
		mapping.put("gui", ""+showGUI);
		mapping.put("user", ""+userInput);
		mapping.put("system", ""+systemOutput);
		mapping.put("monitor", varsToMonitor.toString().replace("[", "").replace("]", ""));
		mapping.put("samples", ""+nbSamples);
		mapping.put("timeout", ""+maxSamplingTime);
		mapping.put("discretisation", ""+discretisationBuckets);
		List<String> moduleNames = new ArrayList<String>();
		for (Class<Module> m : modules) { moduleNames.add(m.getCanonicalName()); }
		mapping.put("modules", ""+moduleNames.toString().replace("[", "").replace("]", ""));
		return mapping;
	}


	/**
	 * Generates an XML element that encodes the settings
	 * 
	 * @param doc the document to which the element must comply
	 * @return the resulting XML element
	 * @throws DialException if the XML generation failed
	 */
	public Element generateXML(Document doc) throws DialException {

		Element root = doc.createElement("settings");
		
		Map<String,String> mapping = getFullMapping();
		for (String otherParam : mapping.keySet()) {
			Element otherParamE = doc.createElement(otherParam);
			otherParamE.setTextContent(""+mapping.get(otherParam));
			root.appendChild(otherParamE);
		}
			
		return root;
	}
	
	
	/**
	 * Copies the settings
	 * 
	 * @return the copy
	 */
	public Settings copy() {
		return new Settings(getFullMapping());
	}
	
	
	/**
	 * Returns a string representation of the settings
	 * 
	 * @return the settings
	 */
	public String toString() {
		return getFullMapping().toString();
	}

	
	
}
