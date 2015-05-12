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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentProbDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.SpeechData;
import opendial.domains.Domain;
import opendial.domains.Model;
import opendial.gui.GUIFrame;
import opendial.gui.TextOnlyInterface;
import opendial.modules.AudioModule;
import opendial.modules.DialogueImporter;
import opendial.modules.DialogueRecorder;
import opendial.modules.ForwardPlanner;
import opendial.modules.Module;
import opendial.modules.RemoteConnector;
import opendial.modules.simulation.Simulator;
import opendial.readers.XMLDomainReader;
import opendial.readers.XMLInteractionReader;
import opendial.readers.XMLSettingsReader;

/**
 * <p>
 * Dialogue system based on probabilistic rules. A dialogue system comprises:
 * <ul>
 * <li>the current dialogue state
 * <li>the dialogue domain with a list of rule-structured models
 * <li>the list of system modules
 * <li>the system settings.
 * </ul>
 * 
 * <p>
 * After initialising the dialogue system, the system should be started with the
 * method startSystem(). The system can be paused or resumed at any time.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class DialogueSystem {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the dialogue state
	protected DialogueState curState;

	// the dialogue domain
	protected Domain domain;

	// the set of modules attached to the system
	protected List<Module> modules;

	// the system settings
	protected Settings settings;

	// whether the system is paused or active
	protected boolean paused = true;

	// whether the system is currently updating its state
	protected volatile boolean updating = false;

	// ===================================
	// SYSTEM INITIALISATION
	// ===================================

	/**
	 * Creates a new dialogue system with an empty dialogue system
	 * 
	 * @throws RuntimeException if the system could not be created
	 */
	public DialogueSystem() throws RuntimeException {

		settings = new Settings();
		curState = new DialogueState();

		// inserting standard modules
		modules = new ArrayList<Module>();
		modules.add(new GUIFrame(this));
		modules.add(new DialogueRecorder(this));
		modules.add(new RemoteConnector(this));
		modules.add(new ForwardPlanner(this));
		domain = new Domain();

	}

	/**
	 * Creates a new dialogue system with the provided dialogue domain
	 * 
	 * @param domain the dialogue domain to employ
	 * @throws RuntimeException if the system could not be created
	 */
	public DialogueSystem(Domain domain) throws RuntimeException {
		this();
		changeDomain(domain);
	}

	/**
	 * Starts the dialogue system and its modules.
	 */
	public void startSystem() {
		paused = false;
		for (Module module : new ArrayList<Module>(modules)) {
			try {
				if (!module.isRunning()) {
					module.start();
				}
				else {
					module.pause(false);
				}
			}
			catch (RuntimeException e) {
				log.warning("could not start module "
						+ module.getClass().getCanonicalName() + ": " + e);
				modules.remove(module);
			}
		}
		synchronized (curState) {
			curState.setAsNew();
			update();
		}
	}

	/**
	 * Changes the dialogue domain for the dialogue domain
	 * 
	 * @param domain the dialogue domain to employ
	 * @throws RuntimeException if the system could not be created
	 */
	public void changeDomain(Domain domain) throws RuntimeException {
		this.domain = domain;
		changeSettings(domain.getSettings());
		curState = domain.getInitialState().copy();
		curState.setParameters(domain.getParameters());
		if (!paused) {
			startSystem();
		}
	}

	/**
	 * Attaches the module to the dialogue system.
	 * 
	 * @param module the module to add
	 */
	public void attachModule(Module module) {
		if (modules.contains(module) || getModule(module.getClass()) != null) {
			log.info("Module " + module.getClass().getCanonicalName()
					+ " is already attached");
			return;
		}
		modules.add((!modules.isEmpty()) ? modules.size() - 1 : 0, module);
		if (!paused) {
			try {
				module.start();
			}
			catch (RuntimeException e) {
				log.warning("could not start module "
						+ module.getClass().getCanonicalName());
				modules.remove(module);
			}
		}
	}

	/**
	 * Attaches the module to the dialogue system.
	 * 
	 * @param module the module class to instantiate
	 */
	public <T extends Module> void attachModule(Class<T> module) {
		try {
			Constructor<T> constructor = module.getConstructor(DialogueSystem.class);
			attachModule(constructor.newInstance(this));
			displayComment("Module " + module.getSimpleName()
					+ " successfully attached");
		}
		catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			log.warning("cannot attach " + module.getSimpleName() + ": "
					+ e.getCause());
			displayComment("cannot attach " + module.getSimpleName() + ": "
					+ e.getCause());
		}
	}

	/**
	 * Detaches the module of the dialogue system. If the module is not included in
	 * the system, does nothing.
	 * 
	 * @param moduleClass the class of the module to detach.
	 */
	public void detachModule(Class<? extends Module> moduleClass) {
		synchronized (curState) {
			Module module = getModule(moduleClass);
			if (module != null) {
				// log.info("detaching module " +
				// module.getClass().getSimpleName());
				module.pause(true);
				modules.remove(module);
			}
		}
	}

	/**
	 * Pauses or resumes the dialogue system.
	 * 
	 * @param toPause whether the system should be paused or resumed.
	 */
	public void pause(boolean toPause) {
		paused = toPause;

		for (Module module : modules) {
			module.pause(toPause);
		}
		if (!toPause && !curState.getNewVariables().isEmpty()) {
			synchronized (curState) {
				update();
			}
		}
	}

	/**
	 * Adds a comment on the GUI and the dialogue recorder.
	 * 
	 * @param comment the comment to display
	 */
	public void displayComment(String comment) {
		if (getModule(GUIFrame.class) != null
				&& getModule(GUIFrame.class).isRunning()) {
			getModule(GUIFrame.class).addComment(comment);
		}
		if (getModule(DialogueRecorder.class) != null
				&& getModule(DialogueRecorder.class).isRunning()) {
			getModule(DialogueRecorder.class).addComment(comment);
		}
	}

	/**
	 * Changes the settings of the system
	 * 
	 * @param settings the new settings
	 */
	public void changeSettings(Settings settings) {

		this.settings.fillSettings(settings.getSpecifiedMapping());

		for (Class<Module> toAttach : settings.modules) {
			if (getModule(toAttach) == null) {
				log.fine("Attaching module: " + toAttach.getSimpleName());
				attachModule(toAttach);
			}
		}
	}

	/**
	 * Enables or disables speech input for the system.
	 * 
	 * @param toEnable whether to enable or disable speech input
	 */
	public void enableSpeech(boolean toEnable) {
		if (toEnable) {
			settings.selectAudioMixers();

			attachModule(AudioModule.class);
			if (settings.showGUI) {
				getModule(GUIFrame.class).enableSpeech(true);
			}
			else {
				getModule(AudioModule.class).activateVAD(true);
			}
		}
		else {
			detachModule(AudioModule.class);
			if (getModule(GUIFrame.class) != null) {
				getModule(GUIFrame.class).enableSpeech(false);
			}
		}
	}

	// ===================================
	// STATE UPDATE
	// ===================================

	/**
	 * Adds the user input (assuming a perfect confidence score) to the dialogue
	 * state and subsequently updates it.
	 * 
	 * @param userInput the user input as a string
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated
	 */
	public Set<String> addUserInput(String userInput) throws RuntimeException {
		Assignment a = new Assignment(settings.userInput, userInput);
		return addContent(a);
	}

	/**
	 * Adds the user input (as a N-best list, where each hypothesis is associated
	 * with a probability) to the dialogue state and subsequently updates it.
	 * 
	 * @param userInput the user input as an N-best list
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated
	 */
	public Set<String> addUserInput(Map<String, Double> userInput) {
		String var =
				(!settings.invertedRole) ? settings.userInput
						: settings.systemOutput;
		CategoricalTable table = new CategoricalTable(var);
		for (String input : userInput.keySet()) {
			table.addRow(input, userInput.get(input));
		}
		return addContent(table);
	}

	/**
	 * Adds the user input as a raw speech data to the dialogue state and
	 * subsequently updates it.
	 * 
	 * @param inputSpeech the speech data containing the user utterance
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated
	 */
	public Set<String> addUserInput(SpeechData inputSpeech) {
		Assignment a = new Assignment(settings.userSpeech, inputSpeech);
		a.addPair(settings.floor, "user");
		return addContent(a);
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(String variable, String value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + variable + "="
					+ value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(String variable, boolean value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + variable + "="
					+ value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(String variable, Value value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + variable + "="
					+ value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(String variable, double value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + variable + "="
					+ value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a categorical table over variables) to the
	 * current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param distrib the (independent) probability distribution to add
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(IndependentProbDistribution distrib) {
		if (!paused) {
			curState.addToState(distrib);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + distrib);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the incremental content (expressed as a distribution over variables) to
	 * the current dialogue state, and subsequently updates it. If followPrevious is
	 * set to true, the content is concatenated with the current distribution for the
	 * variable.
	 * 
	 * @param content the content to add / concatenate
	 * @param followPrevious whether the results should be concatenated to the
	 *            previous values, or reset the content (e.g. when starting a new
	 *            utterance)
	 * @return the set of variables that have been updated
	 * @throws RuntimeException if the incremental update failed
	 */
	public Set<String> addIncrementalContent(IndependentProbDistribution content,
			boolean followPrevious) {
		if (!paused) {
			curState.addToState_incremental(content.toDiscrete(), followPrevious);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + content);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the incremental user input (expressed as an N-best list) to the current
	 * dialogue state, and subsequently updates it. If followPrevious is set to true,
	 * the content is concatenated with the current distribution for the variable.
	 * This allows (for instance) to perform incremental updates of user utterances.
	 * 
	 * 
	 * @param userInput the user input to add / concatenate
	 * @param followPrevious whether the results should be concatenated to the
	 *            previous values, or reset the content (e.g. when starting a new
	 *            utterance)
	 * @return the set of variables that have been updated
	 * @throws RuntimeException if the incremental update failed
	 */
	public Set<String> addIncrementalUserInput(Map<String, Double> userInput,
			boolean followPrevious) {
		CategoricalTable table = new CategoricalTable(settings.userInput);
		for (String input : userInput.keySet()) {
			table.addRow(input, userInput.get(input));
		}
		return addIncrementalContent(table, followPrevious);
	}

	/**
	 * Adds the content (expressed as a certain assignment over variables) to the
	 * current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param assign the value assignment to add
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(Assignment assign) {
		if (!paused) {
			curState.addToState(assign);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + assign);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a multivariate distribution over variables) to
	 * the current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param distrib the multivariate distribution to add
	 * @return the variables that were updated in the process
	 * @throws RuntimeException if the state could not be updated.
	 */
	public Set<String> addContent(MultivariateDistribution distrib) {
		if (!paused) {
			curState.addToState(distrib);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + distrib);
			return Collections.emptySet();
		}
	}

	/**
	 * Merges the Bayesian network included as argument into the current one, and
	 * updates the dialogue state.
	 * 
	 * @param network the Bayesian network to merge into the current state
	 * @return the set of variables that have been updated
	 * @throws RuntimeException if the update failed
	 */
	public Set<String> addContent(BNetwork network) throws RuntimeException {
		if (!paused) {
			curState.addToState(network);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + network);
			return Collections.emptySet();
		}
	}

	/**
	 * Merges the dialogue state included as argument into the current one, and
	 * updates the dialogue state.
	 * 
	 * @param newState the state to merge into the current state
	 * @return the set of variables that have been updated
	 * @throws RuntimeException if the update failed
	 */
	public Set<String> addContent(DialogueState newState) throws RuntimeException {
		if (!paused) {
			curState.addToState(newState);
			return update();
		}
		else {
			log.info("system is paused -- ignoring content " + newState);
			return Collections.emptySet();
		}
	}

	/**
	 * Removes the variable from the dialogue state
	 * 
	 * @param variableId the variable identifier
	 */
	public void removeContent(String variableId) {
		if (!paused) {
			curState.removeFromState(variableId);
			update();
		}
		else {
			log.info("system is paused -- ignoring removal of " + variableId);
		}
	}

	/**
	 * Performs an update loop on the current dialogue state, by triggering all the
	 * models and modules attached to the system until all possible updates have been
	 * performed. The dialogue state is pruned at the end of the operation.
	 * 
	 * <p>
	 * The method returns the set of variables that have been updated during the
	 * process.
	 * 
	 * @return the set of updated variables
	 */
	private Set<String> update() {

		// set of variables that have been updated
		Set<String> updatedVars = new HashSet<String>();

		// if the system is already being updated, stop
		if (updating) {
			return updatedVars;
		}

		updating = true;

		// finding the new variables that must be processed
		Set<String> toProcess = curState.getNewVariables();
		while (!toProcess.isEmpty()) {
			synchronized (curState) {
				// reducing the dialogue state to its relevant nodes
				curState.reduce();

				// applying the domain models
				for (Model model : domain.getModels()) {
					if (model.isTriggered(curState, toProcess)) {
						model.trigger(curState);
						if (model.isBlocking()
								&& !curState.getNewVariables().isEmpty()) {
							break;
						}
					}
				}

				// applying the external modules
				for (Module module : modules) {
					module.trigger(curState, toProcess);
				}

				updatedVars.addAll(toProcess);
				toProcess = curState.getNewVariables();
			}
		}

		updating = false;
		return updatedVars;
	}

	/**
	 * Connects to a remote client with the given IP address and port
	 * 
	 * @param address the IP address of the remote client
	 * @param port the port of the remote client
	 */
	public void connectTo(String address, int port) {
		settings.remoteConnections.put(address, port);
		getModule(RemoteConnector.class).connectTo(address, port);
		if (settings.showGUI) {
			getModule(GUIFrame.class).getMenu().update();
		}
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the current dialogue state for the dialogue system.
	 * 
	 * @return the dialogue state
	 */
	public DialogueState getState() {
		return curState;
	}

	/**
	 * Returns who holds the current conversational floor (user, system, or free)
	 * 
	 * @return a string stating who currently owns the floor
	 */
	public String getFloor() {
		if (curState.hasChanceNode(settings.floor)) {
			return getContent(settings.floor).getBest().toString();
		}
		else
			return "free";
	}

	/**
	 * Returns the probability distribution associated with the variables in the
	 * current dialogue state.
	 * 
	 * @param variable the variable to query
	 * @return the resulting probability distribution for these variables
	 */
	public IndependentProbDistribution getContent(String variable) {
		return curState.queryProb(variable);
	}

	/**
	 * Returns the probability distribution associated with the variables in the
	 * current dialogue state.
	 * 
	 * @param variables the variables to query
	 * @return the resulting probability distribution for these variables
	 */
	public MultivariateDistribution getContent(Collection<String> variables) {
		return curState.queryProb(variables);
	}

	/**
	 * Returns the module attached to the dialogue system and belonging to a
	 * particular class, if one exists. If no module exists, returns null
	 * 
	 * @param cls the class.
	 * @return the attached module of that class, if one exists.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Module> T getModule(Class<T> cls) {
		for (Module mod : new ArrayList<Module>(modules)) {
			if (cls.isAssignableFrom(mod.getClass())) {
				return (T) mod;
			}
		}
		return null;
	}

	/**
	 * Returns true is the system is paused, and false otherwise
	 * 
	 * @return true if paused, false otherwise.
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Returns the settings for the dialogue system.
	 * 
	 * @return the system settings.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Returns the domain for the dialogue system.
	 * 
	 * @return the dialogue domain.
	 */
	public Domain getDomain() {
		return domain;
	}

	/**
	 * Returns the collection of modules attached to the system
	 * 
	 * @return the modules
	 */
	public Collection<Module> getModules() {
		return new ArrayList<Module>(modules);
	}

	/**
	 * Returns the local address (IP and port) used by the dialogue system
	 * 
	 * @return the IP_address:port of the dialogue system
	 */
	public String getLocalAddress() {
		return getModule(RemoteConnector.class).getLocalAddress();
	}

	// ===================================
	// MAIN METHOD
	// ===================================

	/**
	 * Starts the dialogue system. The content of the args array is ignored.
	 * Command-line parameters can however be specified through system properties via
	 * the -D flag. All parameters are optional.
	 * 
	 * <p>
	 * Possible properties are:
	 * <ul>
	 * <li>-Ddomain=path/to/domain/file: dialogue domain file
	 * <li>-Dsettings=path/to/settings/file: settings file
	 * <li>-Ddialogue=path/to/recorded/dialogue: dialogue file to import
	 * <li>-Dsimulator=path/to/simulator/domain/file: dialogue domain file for the
	 * simulator
	 * </ul>
	 * 
	 * @param args is ignored.
	 */
	public static void main(String[] args) {
		try {
			DialogueSystem system = new DialogueSystem();
			String domainFile = System.getProperty("domain");
			String settingsFile = System.getProperty("settings");
			String dialogueFile = System.getProperty("dialogue");
			String simulatorFile = System.getProperty("simulator");

			system.getSettings().fillSettings(System.getProperties());
			if (domainFile != null) {
				system.changeDomain(XMLDomainReader.extractDomain(domainFile));
				log.info("Domain from " + domainFile + " successfully extracted");
			}
			if (settingsFile != null) {
				system.getSettings().fillSettings(
						XMLSettingsReader.extractMapping(settingsFile));
				log.info("Settings from " + settingsFile + " successfully extracted");
			}
			if (dialogueFile != null) {
				List<DialogueState> dialogue =
						XMLInteractionReader.extractInteraction(dialogueFile);
				log.info("Interaction from " + dialogueFile
						+ " successfully extracted");
				(new DialogueImporter(system, dialogue)).start();
			}
			if (simulatorFile != null) {
				Simulator simulator =
						new Simulator(system,
								XMLDomainReader.extractDomain(simulatorFile));
				log.info("Simulator with domain " + simulatorFile
						+ " successfully extracted");
				system.attachModule(simulator);
			}
			Settings settings = system.getSettings();
			system.changeSettings(settings);
			if (!settings.showGUI) {
				system.attachModule(new TextOnlyInterface(system));
			}

			system.startSystem();
			log.info("Dialogue system started!");
		}
		catch (RuntimeException e) {
			log.severe("could not start system, aborting: " + e);
		}
	}

}
