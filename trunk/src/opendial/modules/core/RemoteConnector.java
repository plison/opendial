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

package opendial.modules.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.modules.Module;
import opendial.readers.XMLStateReader;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

public class RemoteConnector implements Module {

	// logger
	public static Logger log = new Logger("RemoteConnector", Logger.Level.DEBUG);

	public static int PORT = 2222;
	DialogueSystem system;
	boolean paused = true;
	
	public static enum MessageType {INIT, XML, STREAM, MISC}

	ServerSocket local;

	public RemoteConnector(DialogueSystem system) {
		this.system = system;
	}

	@Override
	public void start() throws DialException {
		paused = false;
		setupServer();
		try {
			String message = InetAddress.getLocalHost().getHostAddress()+":" + local.getLocalPort();
			forwardContent(MessageType.INIT, message);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		
		if (!paused) {
			try {
			Document xmlDoc = XMLUtils.newXMLDocument();
			Element root = xmlDoc.createElement("update");
			xmlDoc.appendChild(root);
			updatedVars.stream()
					.filter(v -> state.hasChanceNode(v))
					.map(v -> {try {return state.queryProb(v).generateXML(xmlDoc); } 
					catch (DialException e) {return null; }})
					.filter(v -> v!=null)
					.forEach(n -> root.appendChild(n));
			if (root.hasChildNodes()) {
				forwardContent(MessageType.XML,XMLUtils.serialise(xmlDoc));
			}
			}
			catch (DialException e) {
				log.warning("cannot update remote connector: " +e);
			}
		}
	}

	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	@Override
	public boolean isRunning() {
		return !paused;
	}

	private void setupServer() {
		try {
			try { local = new ServerSocket(PORT); }
			catch (BindException e) { local = new ServerSocket(PORT+1);}
			new Thread(() -> readContent()).start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void forwardContent(MessageType messageType, String content) {
		try {
		String connect = system.getSettings().params.getProperty("connect");
		if (connect!= null) {
			int port = (connect.contains(":"))? Integer.parseInt(connect.split(":")[1]) : PORT;
			Socket socket = new Socket(connect.split(":")[0],port);
			log.debug("forwarding content to "+ connect.split(":")[0] + ": " + port);
			OutputStream out = socket.getOutputStream();
			out.write(messageType.ordinal());
			IOUtils.write(content, out);
			socket.close();
		}
		}
		catch (Exception e) {
			log.warning("cannot forward content: " + e);
		}
	}

	private void readContent() {
		while (true) {
			Socket connection;
			try {
				connection = local.accept();
				InputStream in = connection.getInputStream();
				MessageType type = MessageType.values()[in.read()];
				byte[] message = IOUtils.toByteArray(in);
				String content = new String(message);
				if (type == MessageType.INIT) {
					log.info("Connected to " + content);
					system.getSettings().params.setProperty("connect", content);
				}
				else if (type == MessageType.XML) {
					Document doc = XMLUtils.loadXMLFromString(content);
					BNetwork nodes = XMLStateReader.getBayesianNetwork(doc);
					system.addContent(nodes);
				}
				else if (type == MessageType.MISC) {
					log.info("received message: " + content);
				}
				Thread.sleep(100);
			} catch (IOException | InterruptedException | ParserConfigurationException 
					| SAXException | DialException e) {
				e.printStackTrace();
			}
		}
	}


}
