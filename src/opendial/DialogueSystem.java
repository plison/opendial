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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import opendial.bn.BNetwork;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.distribs.IndependentDistribution;
import opendial.bn.distribs.MultivariateDistribution;
import opendial.bn.distribs.ProbDistribution;
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
import opendial.readers.XMLDialogueReader;
import sun.util.logging.resources.logging;

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

	// ===================================
	// SYSTEM INITIALISATION
	// ===================================

	/**
	 * Creates a new dialogue system with an empty dialogue system
	 * 
	 */
	public DialogueSystem() {
		//SS adding this for logging..? how was it done before?
		log.setLevel(Level.INFO);		
		log.setUseParentHandlers(false);
		
		FileHandler fh;
		try {
			fh = new FileHandler("./log/od.log");
	        log.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter); 

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  

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
	 */
	public DialogueSystem(Domain domain) {
		this();
		changeDomain(domain);
	}

	/**
	 * Creates a new dialogue system with the provided dialogue domain
	 * 
	 * @param domainFile the dialogue domain to employ
	 */
	public DialogueSystem(String domainFile) {
		this();
		changeDomain(XMLDomainReader.extractDomain(domainFile));
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
				e.printStackTrace();
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
	 */
	public void changeDomain(Domain domain) {
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
			displayComment(
					"Module " + module.getSimpleName() + " successfully attached");
		}
		catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			log.warning(
					"cannot attach " + module.getSimpleName() + ": " + e.getCause());
			displayComment(
					"cannot attach " + module.getSimpleName() + ": " + e.getCause());
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
		else {
			log.info(comment);
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
			if (getModule(AudioModule.class) == null) {
				settings.selectAudioMixers();
				attachModule(AudioModule.class);
				if (settings.showGUI) {
					getModule(GUIFrame.class).enableSpeech(true);
				}
				else {
					getModule(AudioModule.class).activateVAD(true);
				}
			}
		}
		else {
			detachModule(AudioModule.class);
			if (getModule(GUIFrame.class) != null) {
				getModule(GUIFrame.class).enableSpeech(false);
			}
		}
	}

	/**
	 * Imports the dialogue specified in the provided file.
	 * 
	 * @param dialogueFile the file containing the dialogue to import
	 * @return the dialogue importer thread
	 */
	public DialogueImporter importDialogue(String dialogueFile) {
		List<DialogueState> turns = XMLDialogueReader.extractDialogue(dialogueFile);
		DialogueImporter importer = new DialogueImporter(this, turns);
		importer.start();
		return importer;
	}

	// ===================================
	// STATE UPDATE
	// ===================================

	/**
	 * Adds the user input (assuming a perfect confidence score) to the dialogue
	 * state and subsequently updates it.
	 * 
	 * @param userInput the user input as a string
	 * @return the variables that were updated in the process not be updated
	 */
	public Set<String> addUserInput(String userInput) {
		Assignment a = new Assignment(settings.userInput, userInput);
		return addContent(a);
	}

	/**
	 * Adds the user input (as a N-best list, where each hypothesis is associated
	 * with a probability) to the dialogue state and subsequently updates it.
	 * 
	 * @param userInput the user input as an N-best list
	 * @return the variables that were updated in the process not be updated
	 */
	public Set<String> addUserInput(Map<String, Double> userInput) {
		String var = (!settings.invertedRole) ? settings.userInput
				: settings.systemOutput;
		CategoricalTable.Builder builder = new CategoricalTable.Builder(var);
		for (String input : userInput.keySet()) {
			builder.addRow(input, userInput.get(input));
		}
		return addContent(builder.build());
	}

	/**
	 * Adds the user input as a raw speech data to the dialogue state and
	 * subsequently updates it.
	 * 
	 * @param inputSpeech the speech data containing the user utterance
	 * @return the variables that were updated in the process not be updated
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
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(String variable, String value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused, ignoring " + variable + "=" + value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(String variable, boolean value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused, ignoring " + variable + "=" + value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(String variable, Value value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused, ignoring " + variable + "=" + value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a pair of variable=value) to the current
	 * dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param variable the variable label
	 * @param value the variable value
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(String variable, double value) {
		if (!paused) {
			curState.addToState(new Assignment(variable, value));
			return update();
		}
		else {
			log.info("system is paused, ignoring " + variable + "=" + value);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a categorical table over variables) to the
	 * current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param distrib the (independent) probability distribution to add
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(IndependentDistribution distrib) {
		if (!paused) {
			curState.addToState(distrib);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + distrib);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a categorical table over variables) to the
	 * current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param distrib the (independent) probability distribution to add
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(ProbDistribution distrib) {
		if (!paused) {
			curState.addToState(distrib);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + distrib);
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
	 * @return the set of variables that have been updated update failed
	 */
	public Set<String> addIncrementalContent(IndependentDistribution content,
			boolean followPrevious) {
		if (!paused) {
			curState.addToState_incremental(content.toDiscrete(), followPrevious);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + content);
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
	 * @return the set of variables that have been updated update failed
	 */
	public Set<String> addIncrementalUserInput(Map<String, Double> userInput,
			boolean followPrevious) {
		CategoricalTable.Builder builder =
				new CategoricalTable.Builder(settings.userInput);
		for (String input : userInput.keySet()) {
			builder.addRow(input, userInput.get(input));
		}
		return addIncrementalContent(builder.build(), followPrevious);
	}

	/**
	 * Adds the content (expressed as a certain assignment over variables) to the
	 * current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param assign the value assignment to add
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(Assignment assign) {
		if (!paused) {
			curState.addToState(assign);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + assign);
			return Collections.emptySet();
		}
	}

	/**
	 * Adds the content (expressed as a multivariate distribution over variables) to
	 * the current dialogue state, and subsequently updates the dialogue state.
	 * 
	 * @param distrib the multivariate distribution to add
	 * @return the variables that were updated in the process not be updated.
	 */
	public Set<String> addContent(MultivariateDistribution distrib) {
		if (!paused) {
			curState.addToState(distrib);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + distrib);
			return Collections.emptySet();
		}
	}

	/**
	 * Merges the Bayesian network included as argument into the current one, and
	 * updates the dialogue state.
	 * 
	 * @param network the Bayesian network to merge into the current state
	 * @return the set of variables that have been updated
	 */
	public Set<String> addContent(BNetwork network) {
		if (!paused) {
			curState.addToState(network);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + network);
			return Collections.emptySet();
		}
	}

	/**
	 * Merges the dialogue state included as argument into the current one, and
	 * updates the dialogue state.
	 * 
	 * @param newState the state to merge into the current state
	 * @return the set of variables that have been updated
	 */
	public Set<String> addContent(DialogueState newState) {
		if (!paused) {
			curState.addToState(newState);
			return update();
		}
		else {
			log.info("system is paused, ignoring content " + newState);
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
			log.info("system is paused, ignoring removal of " + variableId);
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
		Map<String, Integer> updatedVars = new HashMap<String, Integer>();

		while (!curState.getNewVariables().isEmpty()) {

			// finding the new variables that must be processed
			Set<String> toProcess = curState.getNewVariables();
			synchronized (curState) {

				// reducing the dialogue state to its relevant nodes
				curState.reduce();

				// applying the domain models
				for (Model model : domain.getModels()) {
					if (model.isTriggered(curState, toProcess)) {
						boolean change = model.trigger(curState);
						if (change && model.isBlocking()) {
							break;
						}
					}
				}

				// triggering the domain modules
				modules.forEach(m -> m.trigger(curState, toProcess));

				// checking for recursive update loops
				for (String v : toProcess) {
					int count = updatedVars.compute(v,
							(x, y) -> (y == null) ? 1 : y + 1);
					if (count > 10) {
						displayComment("Warning: Recursive update of variable " + v);
						return updatedVars.keySet();
					}
				}
			}
		}

		return updatedVars.keySet();
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

	/**
	 * Refreshes the dialogue domain by rereading its source file (in case it has
	 * been changed by the user).
	 * 
	 */
	public void refreshDomain() {
		if (domain.isEmpty()) {
			return;
		}
		String srcFile = domain.getSourceFile().getAbsolutePath();
		try {
			domain = XMLDomainReader.extractDomain(srcFile);
			changeSettings(domain.getSettings());
			displayComment("Dialogue domain successfully updated");
		}
		catch (RuntimeException e) {
			// e.printStackTrace();
			log.severe("Cannot refresh domain: " + e.getMessage());
			displayComment("Syntax error: " + e.getMessage());
			domain = new Domain();
			domain.setSourceFile(new File(srcFile));
		}

		if (getModule(GUIFrame.class) != null) {
			getModule(GUIFrame.class).refresh();
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
	public IndependentDistribution getContent(String variable) {
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
		return modules.stream().filter(m -> cls.isAssignableFrom(m.getClass()))
				.map(m -> (T) m).findFirst().orElse(null);
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
	 * Some of the possible properties are:
	 * <ul>
	 * <li>-Ddomain=path/to/domain/file: dialogue domain file
	 * <li>-Ddialogue=path/to/recorded/dialogue: dialogue file to import
	 * <li>-Dsimulator=path/to/simulator/file: domain file for the simulator
	 * <li>--Dgui=true or false: activates or deactives the GUI
	 * </ul>
	 * 
	 * @param args is ignored.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		DialogueSystem system = new DialogueSystem();
		String domainFile = System.getProperty("domain");
		String dialogueFile = System.getProperty("dialogue");
		String simulatorFile = System.getProperty("simulator");

		System.out.println("domain=" + domainFile);
		System.out.println("dialogue=" + dialogueFile);
		system.getSettings().fillSettings(System.getProperties());
		if (domainFile != null) {
			Domain domain;
			try {
				domain = XMLDomainReader.extractDomain(domainFile);
				log.info("Domain from " + domainFile + " successfully extracted");
			}
			catch (RuntimeException e) {
				system.displayComment("Cannot load domain: " + e);
				e.printStackTrace();
				domain = XMLDomainReader.extractEmptyDomain(domainFile);
			}
			system.changeDomain(domain);
		}
		if (dialogueFile != null) {
			system.importDialogue(dialogueFile);
			log.info("Loading Dialog saved in " + dialogueFile);
		}
		if (simulatorFile != null) {
			Simulator simulator = new Simulator(system,
					XMLDomainReader.extractDomain(simulatorFile));
			log.info("Simulator with domain " + simulatorFile
					+ " successfully extracted");
			system.attachModule(simulator);
		}
		Settings settings = system.getSettings();
		system.changeSettings(settings);

		if (!settings.showGUI) {
			log.info("No GUI, start text only interface");
			system.attachModule(new TextOnlyInterface(system));
		}

		system.startSystem();
		log.info("Dialogue system started!");
	}

}
