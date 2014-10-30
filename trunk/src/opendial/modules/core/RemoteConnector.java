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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jfree.util.Log;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import opendial.DialogueSystem;
import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.modules.Module;
import opendial.state.DialogueState;

public class RemoteConnector implements Module {

	// logger
	public static Logger log = new Logger("RemoteConnector", Logger.Level.DEBUG);

	public static int PORT = 2111;
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
		writeToSocket(MessageType.INIT, local.getInetAddress().getHostAddress()+":" + local.getLocalPort());
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {		
		if (!paused) {
			writeToSocket(MessageType.MISC,"updated variables:" + updatedVars);
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
			new Thread(() -> readFromSocket()).start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void writeToSocket(MessageType messageType, String content) {
		try {
		String connect = system.getSettings().params.getProperty("connect");
		if (connect!= null) {
			int port = (connect.contains(":"))? Integer.parseInt(connect.split(":")[1]) : PORT;
			Socket socket = new Socket(connect.split(":")[0],port);
			OutputStream out = socket.getOutputStream();
			out.write(messageType.ordinal());
			IOUtils.write(content, out);
			socket.close();
		}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readFromSocket() {
		while (true) {
			Socket connection;
			try {
				connection = local.accept();
				InputStream in = connection.getInputStream();
				MessageType type = MessageType.values()[in.read()];
				byte[] message = IOUtils.toByteArray(in);
				if (type == MessageType.INIT) {
					String content = new String(message);
					log.info("Connecting to " + content);
					system.getSettings().params.setProperty("connect", content);
				}
				else if (type == MessageType.MISC) {
					String content = new String(message);
					log.info("received message: " + content);
				}
				Thread.sleep(100);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


}
