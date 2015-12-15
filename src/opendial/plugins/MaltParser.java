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

import java.util.logging.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.distribs.ConditionalTable;
import opendial.bn.values.NoneVal;
import opendial.bn.values.RelationalVal;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.datastructs.Assignment;
import opendial.modules.Module;
import opendial.templates.Template;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;
import org.maltparser.core.exception.MaltChainedException;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Plugin module to perform grammatical analysis of the user inputs via:
 * <ol>
 * <li>Part-of-speech tagging with the Stanford tagger
 * (http://nlp.stanford.edu/software/tagger.shtml)
 * <li>Dependency parsing with the Maltparser (http:www.maltparser.org)
 * </ol>
 * 
 * <p>
 * The module requires two parameters to be set: a parameter "taggingmodel" referring
 * to the file containing the POS-tagger model, and a parameter "parsingmodel"
 * referring to the file containing the Maltparser parsing model.
 * 
 * <p>
 * The module is triggered upon each new user input, and outputs a set of alternative
 * parses in the variable "p_u". The parses are represented as RelationalVal objects.
 * Probabilistic rules can be applied to such RelationalVal objects by checking the
 * existence of particular dependency relations in the parse. For instance, the
 * following condition will check whether the parse contains a dependency relation
 * between the head word "move" and the dependent "to" of label "prep": {@code 
 * <if var="p_u" value="move prep> to" relation="contains"/>}
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class MaltParser implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	// the dialogue system
	DialogueSystem system;

	// the Stanford part-of-speech tagger
	MaxentTagger tagger;

	// the MaltParser
	ConcurrentMaltParserModel maltparser;

	// whether the parser is paused or not
	boolean paused = true;

	// name (or template) of the variable that includes the string to parse
	// Default is system.getSettings().userInput.
	Template trigger;

	/**
	 * Creates a new module connected to the dialogue system. The two parameters
	 * "taggingmodel" and "parsingmodel" must be specified in the settings.
	 * 
	 * @param system the dialogue system
	 */
	public MaltParser(DialogueSystem system) {
		this.system = system;
		List<String> missingParams = new LinkedList<String>(
				Arrays.asList("taggingmodel", "parsingmodel"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}
		String taggingModel =
				system.getSettings().params.getProperty("taggingmodel");
		tagger = new MaxentTagger(taggingModel);

		String parsingModel =
				system.getSettings().params.getProperty("parsingmodel");
		try {
			maltparser = ConcurrentMaltParserService
					.initializeParserModel(new File(parsingModel));
		}
		catch (Exception e) {
			throw new RuntimeException("cannot initialise MaltParser: " + e);
		}

		if (system.getSettings().params.containsKey("trigger")) {
			trigger = Template
					.create(system.getSettings().params.getProperty("trigger"));
		}
		else {
			trigger = Template.create(system.getSettings().userInput);
		}

		log.fine("Initialisation of MaltParser module completed");

	}

	/**
	 * Starts the parser.
	 */
	@Override
	public void start() {
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
		for (String updatedVar : updatedVars) {
			if (!paused && state.hasChanceNode(updatedVar)
					&& trigger.match(updatedVar).isMatching()) {

				ConditionalTable.Builder builder =
						new ConditionalTable.Builder("parse(" + updatedVar + ")");
				for (Value v : state.queryProb(updatedVar).toDiscrete()
						.getValues()) {
					if (!(v instanceof NoneVal)) {
						RelationalVal parse = parse(v.toString());
						builder.addRow(new Assignment(updatedVar, v), parse, 1.0);
					}
					else {
						builder.addRow(new Assignment(updatedVar, v),
								ValueFactory.none(), 1.0);
					}
				}
				system.addContent(builder.build());
			}
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
	 * Parses the provided input and returns its corresponding RelationalVal after
	 * tokenisation, POS-tagging and dependency parsing.
	 * 
	 * @param raw_input the raw input to analyse
	 * @return the corresponding RelationalVal
	 */
	public RelationalVal parse(String raw_input) {
		raw_input = raw_input.trim();
		if (Character.isLetter(raw_input.charAt(raw_input.length() - 1))) {
			raw_input += ".";
		}
		String[] taggedInput = tagger.tagString(raw_input).split(" ");
		List<String[]> taggedTokens = Stream.of(taggedInput).map(t -> t.split("_"))
				.collect(Collectors.toList());

		String[] lines = new String[taggedTokens.size()];
		for (int i = 0; i < taggedTokens.size(); i++) {
			String[] token = taggedTokens.get(i);
			lines[i] = (i + 1) + "\t" + token[0] + "\t_\t" + token[1] + "\t"
					+ token[1] + "\t_";
		}
		try {
			ConcurrentDependencyGraph graph = maltparser.parse(lines);
			Map<Integer, FactoredNode> nodes = new HashMap<Integer, FactoredNode>();
			for (int i = 1; i < graph.nTokenNodes(); i++) {
				ConcurrentDependencyNode node = graph.getTokenNode(i);
				int index = node.getIndex();
				String word = node.getLabel(1);
				String posTag = node.getLabel(3);
				nodes.put(index, new FactoredNode(word, posTag));
			}
			FactoredNode root = null;
			for (int i = 1; i < graph.nTokenNodes(); i++) {
				ConcurrentDependencyNode node = graph.getTokenNode(i);
				int index = node.getIndex();
				int head = node.getHeadIndex();
				String headRelation = node.getLabel(7);
				if (head == 0) {
					root = nodes.get(index);
				}
				else {
					nodes.get(head).addChild(nodes.get(index), headRelation);
				}
			}

			if (root != null) {
				return new RelationalVal(root.toString());
			}
			else {
				throw new RuntimeException("root could not be found");
			}
		}
		catch (MaltChainedException e) {
			throw new RuntimeException("Could not start the malt parser: " + e);
		}
	}

	final class FactoredNode {

		public List<FactoredNode> children;
		public List<String> relations;
		public String content;

		public FactoredNode(String word, String posTag) {
			content = word + "|pos:" + posTag;
			children = new ArrayList<FactoredNode>();
			relations = new ArrayList<String>();
		}

		public void addChild(FactoredNode child, String relation) {
			children.add(child);
			relations.add(relation);
		}

		@Override
		public String toString() {
			String str = content;
			if (children.isEmpty()) {
				return str;
			}
			else {
				for (int i = 0; i < relations.size(); i++) {
					str += " " + relations.get(i) + ">";
					str += children.get(i).toString();
				}
				return "[" + str + "]";
			}
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
