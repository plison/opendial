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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
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

	DialogueSystem system;
	boolean paused = true;
	
	HttpServer server;
	HttpClient remoteClient;
	URI remoteURI;
	
	public RemoteConnector(DialogueSystem system) {
		this.system = system;
	}
	
	@Override
	public void start() throws DialException {
		paused = false;
		createServer();
		if (system.getSettings().params.getProperty("connectto") != null) {
			createClient();
		}
	}
	
	
	private void createClient() {
		String host = system.getSettings().params.getProperty("connectto");
		try {
			remoteClient = HttpClientBuilder.create().build();
			URIBuilder builder = new URIBuilder();
			builder.setHost(host);
			remoteURI = builder.build();
			HttpPost post = new HttpPost(remoteURI);
			post.setEntity(new StringEntity("this is the first message", "UTF-8"));
			HttpResponse response = remoteClient.execute(post);
			String responseStr = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
	        log.info("response: " + responseStr);
			
		} catch (URISyntaxException | IOException e) {
			log.info("cannot start connection with remote machine " + host +": " + e );
		}
	}
	
	
	private void createServer() {
		try {
		server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/opendial", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
		}
		catch (IOException e) {
			log.info("cannot start server: " + e );			
		}
	}
 
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (!paused) {
			
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


    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
        	
        	String message = IOUtils.toString(t.getRequestBody(), "UTF-8");
        	log.info("message was: " + message);
        	
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
