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

package opendial.readers;

import java.util.logging.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import opendial.DialogueState;
import opendial.datastructs.Assignment;
import opendial.utils.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * XML reader for previously recorded dialogues.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class XMLDialogueReader {

	final static Logger log = Logger.getLogger("OpenDial");

	/**
	 * Extracts the dialogue specified in the data file. The result is a list of
	 * dialogue state (one for each turn).
	 * 
	 * @param dataFile the XML file containing the turns
	 * @return the list of dialogue states
	 */
	public static List<DialogueState> extractDialogue(String dataFile) {
		// extract the XML document
		Document doc = XMLUtils.getXMLDocument(dataFile);
		Node mainNode = XMLUtils.getMainNode(doc);

		File f = new File(dataFile);
		String rootpath = f.getParent();

		List<DialogueState> sample = new LinkedList<DialogueState>();
		for (int j = 0; j < mainNode.getChildNodes().getLength(); j++) {

			Node node = mainNode.getChildNodes().item(j);
			if (node.getNodeName().contains("Turn")) {
				DialogueState state =
						new DialogueState(XMLStateReader.getBayesianNetwork(node));
				sample.add(state);
				if (node.getNodeName().equals("systemTurn")
						&& state.hasChanceNode("a_m")) {
					Assignment assign =
							new Assignment("a_m", state.queryProb("a_m").getBest());
					state.addEvidence(assign);
				}
			}
			else if (node.getNodeName().equals("wizard")) {
				Assignment assign = Assignment.createFromString(
						node.getFirstChild().getNodeValue().trim());
				sample.get(sample.size() - 1).addEvidence(assign);
			}
			else if (node.getNodeName().equals("import")) {
				String fileName =
						mainNode.getAttributes().getNamedItem("href").getNodeValue();
				List<DialogueState> points =
						extractDialogue(rootpath + "/" + fileName);
				sample.addAll(points);
			}

		}

		return sample;
	}

}
