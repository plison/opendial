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

package opendial;

import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer;

import opendial.modules.Module;
import opendial.utils.AudioUtils;
import opendial.utils.StringUtils;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * System-wide settings for OpenDial.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Settings {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** Default settings */
	public static final String SETTINGS_FILE = "resources//settings.xml";

	/** maximum number of samples to use for likelihood weighting */
	public static int nbSamples = 3000;

	/** maximum sampling time (in milliseconds) */
	public static long maxSamplingTime = 250;

	/** Number of discretisation buckets to convert continuous distributions */
	public static int discretisationBuckets = 50;

	/** Whether to show the GUI */
	public boolean showGUI;

	/** Variable label for the conversational floor */
	public String floor;

	/** Variable label for the user speech signal */
	public String userSpeech;

	/** Variable label for the system speech signal */
	public String systemSpeech;

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
	public static enum Recording {
		NONE, LAST_INPUT, ALL
	}

	/** Whether to record intermediate dialogue state */
	public Recording recording = Recording.LAST_INPUT;

	/** (When relevant) Which audio mixer to use for speech recognition */
	public Mixer.Info inputMixer;

	/** (When relevant) Which audio mixer to use for speech synthesis */
	public Mixer.Info outputMixer;

	/** Other parameters */
	public Properties params = new Properties();

	/** Domain-specific modules to run */
	public List<Class<Module>> modules = new ArrayList<Class<Module>>();

	/** addresses of remote clients to connect to (key=IP address, value=port) */
	public Map<String, Integer> remoteConnections = new HashMap<String, Integer>();

	/** settings that have been explicitly specified (i.e. non-default) */
	Set<String> explicitSettings;

	/** whether the role of user and system are inverted */
	public boolean invertedRole = false;

	/**
	 * Creates new settings with the default values
	 */
	public Settings() {
		explicitSettings = new HashSet<String>();
		fillSettings(XMLUtils.extractMapping(SETTINGS_FILE));
		explicitSettings.clear();

		// formatter for the system logs
		System.getProperties().setProperty(
				"java.util.logging.SimpleFormatter.format", "[%3$s] %4$s: %5$s %n");

	}

	public void selectAudioMixers() {
		List<Mixer.Info> inputMixers = AudioUtils.getInputMixers();
		inputMixer = (!inputMixers.isEmpty()) ? inputMixers.get(0) : null;
		List<Mixer.Info> outputMixers = AudioUtils.getOutputMixers();
		outputMixer = (!outputMixers.isEmpty()) ? outputMixers.get(0) : null;
	}

	/**
	 * Creates a new settings with the values provided as argument. Values that are
	 * not explicitly specified in the mapping are set to their default values.
	 * 
	 * @param mapping the properties
	 */
	public Settings(Properties mapping) {
		explicitSettings = new HashSet<String>();
		fillSettings(XMLUtils.extractMapping(SETTINGS_FILE));
		explicitSettings.clear();
		fillSettings(mapping);
	}

	/**
	 * Fills the current settings with the values provided as argument. Existing
	 * values are overridden.
	 * 
	 * @param mapping the properties
	 */
	@SuppressWarnings("unchecked")
	public void fillSettings(Properties mapping) {

		for (String key : mapping.stringPropertyNames()) {
			if (key.equalsIgnoreCase("horizon")) {
				horizon = Integer.parseInt(mapping.getProperty(key));
			}
			else if (key.equalsIgnoreCase("discount")) {
				discountFactor = Double.parseDouble(mapping.getProperty(key));
			}

			else if (key.equalsIgnoreCase("gui")) {
				showGUI = Boolean.parseBoolean(mapping.getProperty(key));
			}
			else if (key.equalsIgnoreCase("user")) {
				userInput = mapping.getProperty(key);
			}
			else if (key.equalsIgnoreCase("speech_user")) {
				userSpeech = mapping.getProperty(key);
			}
			else if (key.equalsIgnoreCase("speech_system")) {
				systemSpeech = mapping.getProperty(key);
			}
			else if (key.equalsIgnoreCase("floor")) {
				floor = mapping.getProperty(key);
			}
			else if (key.equalsIgnoreCase("system")) {
				systemOutput = mapping.getProperty(key);
			}
			else if (key.equalsIgnoreCase("monitor")) {
				String[] split = mapping.getProperty(key).split(",");
				for (int i = 0; i < split.length; i++) {
					if (split[i].trim().length() > 0) {
						varsToMonitor.add(split[i].trim());
					}
				}
			}
			else if (key.equalsIgnoreCase("samples")) {
				nbSamples = Integer.parseInt(mapping.getProperty(key));
			}
			else if (key.equalsIgnoreCase("timeout")) {
				maxSamplingTime = Integer.parseInt(mapping.getProperty(key));
			}
			else if (key.equalsIgnoreCase("discretisation")) {
				discretisationBuckets = Integer.parseInt(mapping.getProperty(key));
			}

			else if (key.equalsIgnoreCase("recording")) {
				if (mapping.getProperty(key).trim().equalsIgnoreCase("last")) {
					recording = Recording.LAST_INPUT;
				}
				else if (mapping.getProperty(key).trim().equalsIgnoreCase("all")) {
					recording = Recording.ALL;
				}
				else {
					recording = Recording.NONE;
				}
			}
			else if (key.equalsIgnoreCase("connect")) {
				String[] splits = mapping.getProperty(key).split(",");
				for (String split : splits) {
					if (split.contains(":")) {
						String address = split.split(":")[0];
						int port = Integer.parseInt(split.split(":")[1]);
						remoteConnections.put(address, port);
					}
					else {
						log.warning(
								"address of remote connection must contain port");
					}
				}
			}
			else if (key.equalsIgnoreCase("modules")) {
				String[] split = mapping.getProperty(key).split(",");
				for (int i = 0; i < split.length; i++) {
					if (split[i].trim().length() > 0) {
						Class<?> clazz;
						try {
							clazz = Class.forName(split[i].trim());
							for (int j = 0; j < clazz.getInterfaces().length; j++) {
								if (Module.class
										.isAssignableFrom(clazz.getInterfaces()[j])
										&& !modules.contains(clazz)) {
									modules.add((Class<Module>) clazz);
								}
							}
							if (!modules.contains(clazz)) {
								log.warning("class " + split[i].trim()
										+ " is not a module");
								log.fine("interfaces "
										+ Arrays.asList(clazz.getInterfaces()));
							}
						}
						catch (ClassNotFoundException e) {
							log.warning("class not found: " + split[i].trim());
						}
					}
				}
			}
			else {
				params.put(key, mapping.getProperty(key));
			}
		}
		explicitSettings.addAll(mapping.stringPropertyNames());
	}

	/**
	 * Returns a representation of the settings in terms of a mapping between
	 * property labels and values
	 * 
	 * @return the corresponding mapping
	 */
	public Properties getFullMapping() {
		Properties mapping = new Properties();
		mapping.putAll(params);
		mapping.setProperty("horizon", "" + horizon);
		mapping.setProperty("discount", "" + discountFactor);
		mapping.setProperty("gui", "" + showGUI);
		mapping.setProperty("speech_user", "" + userSpeech);
		mapping.setProperty("speech_system", "" + systemSpeech);
		mapping.setProperty("floor", "" + floor);
		mapping.setProperty("user", "" + userInput);
		mapping.setProperty("system", "" + systemOutput);
		mapping.setProperty("inputmixer", "" + inputMixer);
		mapping.setProperty("outputmixer", "" + outputMixer);
		mapping.setProperty("monitor", StringUtils.join(varsToMonitor, ","));
		mapping.setProperty("samples", "" + nbSamples);
		mapping.setProperty("timeout", "" + maxSamplingTime);
		mapping.setProperty("discretisation", "" + discretisationBuckets);
		mapping.setProperty("modules", "" + modules.stream()
				.map(m -> m.getCanonicalName()).collect(Collectors.joining(",")));
		mapping.setProperty("connect",
				"" + remoteConnections.keySet().stream()
						.map(i -> i + ":" + remoteConnections.get(i))
						.collect(Collectors.joining(",")));
		return mapping;
	}

	public Properties getSpecifiedMapping() {
		Properties fullMapping = getFullMapping();

		Properties mapping = new Properties();
		fullMapping.stringPropertyNames().stream()
				.filter(p -> explicitSettings.contains(p))
				.forEach(p -> mapping.setProperty(p, fullMapping.getProperty(p)));
		return mapping;
	}

	/**
	 * Generates an XML element that encodes the settings
	 * 
	 * @param doc the document to which the element must comply
	 * @return the resulting XML element
	 */
	public Element generateXML(Document doc) {

		Element root = doc.createElement("settings");

		Properties mapping = getFullMapping();
		for (String otherParam : mapping.stringPropertyNames()) {
			Element otherParamE = doc.createElement(otherParam);
			otherParamE.setTextContent("" + mapping.get(otherParam));
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
	@Override
	public String toString() {
		return getFullMapping().toString();
	}

}
