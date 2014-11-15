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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.BNetwork;
import opendial.bn.values.Value;
import opendial.datastructs.SpeechStream;
import opendial.gui.GUIFrame;
import opendial.modules.Module;
import opendial.readers.XMLStateReader;
import opendial.state.DialogueState;
import opendial.utils.AudioUtils;
import opendial.utils.XMLUtils;

/**
 * Module used to connect OpenDial to other remote clients (for instance,
 * in order to conduct Wizard-of-Oz experiments).
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $ *
 */
public class RemoteConnector implements Module {

	// logger
	public static Logger log = new Logger("RemoteConnector", Logger.Level.DEBUG);

	// the local dialogue system
	DialogueSystem system;

	// whether the connector is paused or not
	boolean paused = true;

	// whether to skip the next trigger (to avoid infinite loops)
	boolean skipNextTrigger = false;

	// types of messages that can be sent through the connector
	private static enum MessageType {INIT, XML, STREAM, MISC, CLOSE}

	// local server socket
	ServerSocket local;

	// ===================================
	//  CONSTRUCTION
	// ===================================

	/**
	 * A server socket is created, using an arbitrary open port (NB: the port can be 
	 * read in the "About" page in the GUI).
	 * 
	 * @param system the local dialogue system
	 * @throws DialException if the server socket could not be opened
	 */
	public RemoteConnector(DialogueSystem system) throws DialException {
		this.system = system;
		try {
			local = new ServerSocket(0); 
			new Thread(() -> readContent()).start();
		}
		catch (IOException e) {
			throw new DialException("cannot initialise remote connector: " + e);
		}
	}

	/**
	 * Starts the connector. If the system settings include remote connections, 
	 * connects to them by sending an INIT message.
	 * 
	 */
	@Override
	public void start() {

		// connect to remote connections
		if (!system.getSettings().remoteConnections.isEmpty()) {
			InputStream content = IOUtils.toInputStream(getLocalAddress());
			forwardContent(MessageType.INIT, content);
		}
 
		// add a shutdown hook to close the remote connections
		Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
			if (!system.getSettings().remoteConnections.isEmpty()) {
				log.info("Shutting down remote connection");
				InputStream content = IOUtils.toInputStream(getLocalAddress());
				forwardContent(MessageType.CLOSE, content);
				try {Thread.sleep(100);} catch (Exception e) {e.printStackTrace();}
			}
		}));
		paused = false;
	}


	/**
	 * Connects to a new IP address and port (after startup).  Note that the
	 * remote connection must also be present in the system settings.
	 * 
	 * @param address the IP address.
	 * @param port the port to employ
	 */
	public void connectTo(String address, int port) {
		InputStream content = IOUtils.toInputStream(getLocalAddress());
		sendContent(MessageType.INIT, content, address, port);
	}



	// ===================================
	//  UPDATE
	// ===================================


	/**
	 * Sends the updated variables through the socket connection.
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {	
		if (skipNextTrigger) {
			skipNextTrigger = false;
			return;
		}
		else if (paused || system.getSettings().remoteConnections.isEmpty()) {
			return;
		}
		try {
			// creating an XML document with the updated variables
			Document xmlDoc = XMLUtils.newXMLDocument();
			Element root = xmlDoc.createElement("update");
			xmlDoc.appendChild(root);
			updatedVars.stream()
				.filter(v -> state.hasChanceNode(v))
				.filter(v -> !v.equals(system.getSettings().userSpeech))
				.map(v -> state.queryProb(v).generateXML(xmlDoc))
				.forEach(n -> root.appendChild(n));

			// if the resulting document is non-empty, forward it through the socket
			if (root.hasChildNodes()) {
				InputStream content = IOUtils.toInputStream(XMLUtils.serialise(xmlDoc));
				forwardContent(MessageType.XML, content);
				return;
			}

			// if the content is a user speech signal, send it as a stream
			String speechVar = system.getSettings().userSpeech;
			if (updatedVars.contains(speechVar) && system.getState().hasChanceNode(speechVar)) {
				Value val = system.getContent(speechVar).toDiscrete().getBest();
				if (val instanceof SpeechStream) {
					forwardContent(MessageType.STREAM, (SpeechStream)val);
				}
			}
		}
		catch (DialException e) {
			log.warning("cannot update remote connector: " +e);
		}
	}


	/**
	 * Pauses the connector
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}



	// ===================================
	//  GETTERS
	// ===================================

	/**
	 * Returns the local address of the system, in the form Ip_address:port
	 * 
	 * @return the local address of the system
	 */
	public String getLocalAddress() {
		try {
			String localIp = InetAddress.getLocalHost().getHostAddress();
			return localIp + ":" + local.getLocalPort();
		}
		catch (UnknownHostException e) {
			log.warning("cannot extract local address");
			return "";
		}
	}


	/**
	 * Returns true if the system is running, and false otherwise
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}


	// ===================================
	//  PRIVATE METHODS
	// ===================================

	/**
	 * Forwards the input stream (with the given message type) to all connected
	 * clients.
	 * 
	 * @param messageType the message type
	 * @param content the content (as a stream)
	 */
	private void forwardContent(MessageType messageType, InputStream content) {
		for (String ip : system.getSettings().remoteConnections.keySet()) {
			int port = system.getSettings().remoteConnections.get(ip);
			sendContent(messageType, content, ip, port);
		}
	}

	/**
	 * Sends the input stream to a particular remote client.
	 * 
	 * @param messageType the message type
	 * @param content the content (as a stream)
	 * @param address the address to use
	 * @param port the port
	 */
	private void sendContent(MessageType messageType, InputStream content, 
			String address, int port) {
		Runnable r = () ->  {
			try {
				Socket socket = new Socket(address, port);
				OutputStream out = socket.getOutputStream();
				out.write(messageType.ordinal());
				IOUtils.copy(IOUtils.toBufferedInputStream(content), out);
				socket.close();
			}
			catch (Exception e) {
				log.warning("cannot forward content: " + e);
			}
		};
		new Thread(r).start();
		if (messageType == MessageType.INIT) {
			system.displayComment("Connected to " + address  + ":" + port);
		}
	}

	/**
	 * Infinite loop that reads content from the server socket<ul>
	 * <li>If the message type is INIT, adds the connection to the list 
	 *     of remote connections
	 * <li>If the message type is XML, adds the new distributions to the dialogue
	 *     state
	 * <li>If the message type is STREAM, play the stream on the output mixer
	 * <li>If the message type is CLOSE, removes the connections from the list
	 * </ul>
	 */
	private void readContent() {
		while (true) {
			try {
				Socket connection = local.accept();
				InputStream in = connection.getInputStream();
				MessageType type = MessageType.values()[in.read()];
				byte[] message = IOUtils.toByteArray(in);
				if (type == MessageType.INIT) {
					String content = new String(message);
					String ip = content.split(":")[0];
					int port = Integer.parseInt(content.split(":")[1]);
					log.info("Connected to " + ip + ":" + port);
					system.displayComment("Connected to " + ip + ":" + port);
					system.getSettings().remoteConnections.put(ip, port);
					if (system.getSettings().showGUI) {
						system.getModule(GUIFrame.class).enableSpeech(true);
						system.getModule(GUIFrame.class).getMenu().update();
					}
				}
				else if (type == MessageType.XML) {
					String content = new String(message);
					Document doc = XMLUtils.loadXMLFromString(content);
					BNetwork nodes = XMLStateReader.getBayesianNetwork(XMLUtils.getMainNode(doc));
					skipNextTrigger = true;
					system.addContent(nodes);
				}
				else if (type == MessageType.MISC) {
					String content = new String(message);
					log.info("received message: " + content);
				}
				else if (type == MessageType.STREAM) {
					AudioUtils.playAudio(message, system.getSettings().outputMixer);
				}
				else if (type == MessageType.CLOSE) {
					String content = new String(message);
					log.info("Disconnecting from " + content);
					system.displayComment("Disconnecting from " + content);
					String ip = content.split(":")[0];
					system.getSettings().remoteConnections.remove(ip);
				}
				Thread.sleep(100);
			} catch (IOException | InterruptedException | ParserConfigurationException 
					| SAXException | DialException e) {
				e.printStackTrace();
			}
		}
	}


}
