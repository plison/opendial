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

package opendial.domains;

import java.io.File;
import java.util.logging.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import opendial.DialogueState;
import opendial.Settings;
import opendial.bn.BNetwork;

/**
 * Representation of a dialogue domain, composed of (1) an initial dialogue state and
 * (2) a list of probability and utility models employed to update the dialogue state
 * upon relevant changes.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class Domain {

	final static Logger log = Logger.getLogger("OpenDial");

	// path to the source XML file (and its imports)
	File xmlFile;
	List<File> importedFiles;

	// initial dialogue state
	DialogueState initState;

	BNetwork parameters;

	// list of models
	List<Model> models;

	// settings
	Settings settings;

	/**
	 * Creates a new domain with an empty dialogue state and list of models.
	 */
	public Domain() {
		settings = new Settings();
		models = new LinkedList<Model>();
		initState = new DialogueState();
		parameters = new BNetwork();
		importedFiles = new ArrayList<File>();
	}

	/**
	 * Associate the given source XML files to the domain
	 * 
	 * @param xmlFile the file to associate
	 */
	public void setSourceFile(File xmlFile) {
		if (xmlFile.exists()) {
			this.xmlFile = xmlFile;
		}
		else {
			this.xmlFile = new File("resources/" + xmlFile);
		}
	}

	/**
	 * Returns true if the domain is empty
	 * 
	 * @return true if empty, false otherwise
	 */
	public boolean isEmpty() {
		return this.xmlFile == null;
	}

	/**
	 * Adds the given XML files to the list of imported source files
	 * 
	 * @param xmlFile the file to add
	 */
	public void addImportedFiles(File xmlFile) {
		if (xmlFile.exists()) {
			importedFiles.add(xmlFile);
		}
		else {
			importedFiles.add(new File("resources/" + xmlFile));
		}
	}

	/**
	 * Returns the source file containing the domain specification
	 * 
	 * @return the source file
	 */
	public File getSourceFile() {
		return xmlFile;
	}

	/**
	 * Returns the (possibly empty) list of imported files
	 * 
	 * @return the imported files
	 */
	public List<File> getImportedFiles() {
		return importedFiles;
	}

	/**
	 * Sets the initial dialogue state
	 * 
	 * @param initState the initial state
	 */
	public void setInitialState(DialogueState initState) {
		this.initState = initState;
	}

	/**
	 * Adds a model to the domain
	 * 
	 * @param model the model to add
	 */
	public void addModel(Model model) {
		models.add(model);
	}

	/**
	 * Returns the initial dialogue state
	 * 
	 * @return the initial state
	 */
	public DialogueState getInitialState() {
		return initState;
	}

	/**
	 * Returns the models for the domain
	 * 
	 * @return the models
	 */
	public List<Model> getModels() {
		return models;
	}

	/**
	 * Replaces the domain-specific settings
	 * 
	 * @param settings the settings
	 */
	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	/**
	 * Returns the domain-specific settings
	 * 
	 * @return the settings for the domain
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * Returns the domain name.
	 */
	@Override
	public String toString() {
		return xmlFile.getName();
	}

	/**
	 * Sets the prior distribution for the domain parameters
	 * 
	 * @param parameters the parameters
	 */
	public void setParameters(BNetwork parameters) {
		this.parameters = parameters;
	}

	/**
	 * Returns the prior distribution for the domain parameters
	 * 
	 * @return the prior distribution for the parameters
	 */
	public BNetwork getParameters() {
		return parameters;
	}

	/**
	 * Returns true if o is a domain with the same source file, and false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof Domain) {
			File src1 = ((Domain) o).getSourceFile();
			if (src1 == xmlFile) {
				return true;
			}
			return (src1 != null && xmlFile != null && src1.equals(xmlFile));
		}
		return false;
	}

}
