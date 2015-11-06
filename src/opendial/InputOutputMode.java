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

import java.io.Console;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import opendial.bn.BNetwork;
import opendial.bn.nodes.ChanceNode;
import opendial.readers.XMLStateReader;
import opendial.utils.XMLUtils;

/**
 * Small process to grab an input state (in XML format) from the standard input, run
 * the rules, and produce the resulting dialogue state onto the standard output.
 * 
 * <p>
 * NB: the XML input must be of the surrounded by the XML tag <state>".
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class InputOutputMode implements Runnable {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	DialogueSystem ds;

	/**
	 * Creates the new I/O process
	 * 
	 * @param ds
	 */
	public InputOutputMode(DialogueSystem ds) {
		this.ds = ds;
	}

	/**
	 * Extracts the dialogue state to import from the standard input, update the
	 * dialogue state on the basis of the rules, and generates the resulting dialogue
	 * state in XML format onto the standard output.
	 */
	@Override
	public void run() {

		Console console = System.console();
		while (true) {
			try {
				String line = console.readLine();
				if (line != null) {
					if (!line.startsWith("<state>") || !line.endsWith("</state>")) {
						log.warning("Line is not well-formed (must start with <state> and end with </state>)");
						continue;
					}
					InputProcessor ip = new InputProcessor(line);
					(new Thread(ip)).start();
				}

			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
	}

	/**
	 * Thread to process a given state input and generate the resulting output
	 * (a lock is set on the dialogue state during that process).
	 *
	 */
	class InputProcessor implements Runnable {

		String input;

		public InputProcessor(String input) {
			this.input = input;
		}

		@Override
		public void run() {
			synchronized (ds.curState) {
				BNetwork bn = XMLStateReader.extractBayesianNetworkFromString(input);
				for (ChanceNode cn : new ArrayList<ChanceNode>(bn.getChanceNodes())) {
					cn.setId(cn.getId().replace("^n", "'"));
				}
				ds.curState = new DialogueState(bn);
				ds.update();

				Document output = XMLUtils.newXMLDocument();
				Element el = ds.getState().generateXML(output, ds.getState().getNodeIds());
				output.appendChild(el);
				System.out.println(XMLUtils.writeXMLString(output).replace("\n",""));
			}
		}
	}


}
