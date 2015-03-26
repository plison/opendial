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

package opendial.plugins;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.core.exception.MaltChainedException;
import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.distribs.CategoricalTable;
import opendial.bn.values.Value;
import opendial.modules.Module;
import opendial.state.DialogueState;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Plugin module to perform grammatical analysis of the user inputs via: <ol>
 * <li>Part-of-speech tagging with the Stanford tagger (http://nlp.stanford.edu/software/tagger.shtml)
 * <li>Dependency parsing with the Maltparser (http:www.maltparser.org)
 * </ol>
 * 
 * The module requires two parameters to be set: a parameter "taggingmodel" referring
 * to the file containing the POS-tagger model, and a parameter "parsingmodel" referring 
 * to the file containing the Maltparser parsing model. 
 * 
 * The module is triggered upon each new user input, and outputs a set of alternative parses
 * in the variable "p_u". The parses are represented as ParseValue objects. Probabilistic rules
 * can be applied to such ParseValue objects by checking the existence of particular
 * dependency relations in the parse.  For instance, the following condition will check whether
 * the parse contains a dependency relation between the head word "move" and the dependent
 * "to" of label "prep":
 *  {@code <if var="p_u" value="(move,prep,to)" relation="contains"/>}
 *  
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
public class MaltParser implements Module {

	// logger
	public static Logger log = new Logger("MaltParser", Logger.Level.DEBUG);

	// the dialogue system
	DialogueSystem system;

	// the Stanford part-of-speech tagger
	MaxentTagger tagger;
	
	// the MaltParser
	ConcurrentMaltParserModel maltparser;
	
	// whether the parser is paused or not
	boolean paused = true;
	
	/**
	 * Creates a new module connected to the dialogue system.  The two parameters
	 * "taggingmodel" and "parsingmodel" must be specified in the settings.
	 * 
	 * @param system the dialogue system
	 */
	public MaltParser(DialogueSystem system) {
		this.system = system;
		List<String> missingParams = new LinkedList<String>(Arrays.asList("taggingmodel", "parsingmodel"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new DialException("Missing parameters: " + missingParams);
		}
		String taggingModel = system.getSettings().params.getProperty("taggingmodel");
		tagger = new MaxentTagger(taggingModel);
		
		String parsingModel = system.getSettings().params.getProperty("parsingmodel");
		try {
			maltparser = ConcurrentMaltParserService.initializeParserModel(new File(parsingModel));
		} catch (Exception e) {
			throw new DialException("cannot initialise MaltParser: " + e);
		}
		log.info("Initialisation of MaltParser module completed");

	}
	
	/**
	 * Starts the parser.
	 */
	@Override
	public void start() throws DialException {
		paused = false;
	}
	

	/**
	 * If the user input variable is updated, parse the utterance and adds a new
	 * variable "p_u" with the parsing results.
	 * 
	 * @param state the dialogue state
	 * @param updatedVars the list of updated variables in the state
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		String userInput = system.getSettings().userInput;
		if (!paused && updatedVars.contains(userInput) && state.hasChanceNode(userInput)) {
			CategoricalTable table = state.queryProb(userInput).toDiscrete();
			CategoricalTable parseTable = new CategoricalTable("p_u");
			for (Value v : table.getValues()) {
				ParseValue parse = parse(v.toString());
				parseTable.addRow(parse, table.getProb(v));
			}
			system.getState().addToState(parseTable);
		}
	}

	/**
	 * Pauses or restarts the module
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}
	
	
	/**
	 * Parses the provided input and returns its corresponding ParseValue after
	 * tokenisation, POS-tagging and dependency parsing.
	 * 
	 * @param raw_input the raw input to analyse
	 * @return the corresponding ParseValue
	 */
	public ParseValue parse(String raw_input) {
		raw_input = raw_input.trim();
		if (Character.isLetter(raw_input.charAt(raw_input.length()-1))) {
			raw_input += ".";
		}
		String[] taggedInput = tagger.tagString(raw_input).split(" ");
		List<String[]> taggedTokens = Stream.of(taggedInput)
				.map(t -> t.split("_")).collect(Collectors.toList());
		
		String[] lines = new String[taggedTokens.size()];
		for (int i = 0 ; i < taggedTokens.size() ; i++) {
			String[] token = taggedTokens.get(i);
			lines[i] =  (i+1) + "\t" + token[0] + "\t_\t" + token[1] 
					+ "\t" + token[1] + "\t_";
		}
        try {
		ConcurrentDependencyGraph graph = maltparser.parse(lines);
        return new ParseValue(graph);
		} catch (MaltChainedException e) {
			throw new DialException("Could not start the malt parser: " +e);
		}
	}


	/**
	 * Returns true if the module is running, and false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

}
