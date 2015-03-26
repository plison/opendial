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

import java.util.HashMap;
import java.util.Map;

import com.aldebaran.qimessaging.*;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;

public class NaoSession extends QimessagingService {

	// logger
	public static Logger log = new Logger("NaoSession", Logger.Level.DEBUG);
	
	public static final int MAX_RESPONSE_DELAY = 6000;

	private static NaoSession nao;	
	
	String ip;
	Session sess;
	Map<String,com.aldebaran.qimessaging.Object> services;
			
	private NaoSession(String ip) throws DialException {
		try {
		this.ip = ip;
	    new Application(new String[0]);
		sess = new Session();
		Future<Void> fut = sess.connect("tcp://"+ip +":9559");
		fut.get();
		services = new HashMap<String, com.aldebaran.qimessaging.Object>();
		services.put("ALMemory", sess.service("ALMemory"));
		}
		catch (Exception e) {
			throw new DialException("could not start Nao session: " + e);
		}
	}

	
	public static NaoSession grabSession(Settings settings) throws DialException {
		if (!settings.params.containsKey("nao_ip")) {
			throw new DialException("Missing parameter: nao_ip");
		}
		if (nao == null) {
			nao = new NaoSession(settings.params.getProperty("nao_ip"));
		}
		return nao;
	}
	
	public <T> T call (String service, String method, java.lang.Object... args) throws DialException {
		try {
			if (!services.containsKey(service)) {
			services.put(service, sess.service(service));			
		}
		return services.get(service).<T>call(method, args).get();
		}
		catch (Exception e) {
			throw new DialException("could not call service " + service + ": " + e);
		}
	}
	
	public void listenToEvent(String event, NaoEventListener listener) throws DialException  {
		try {
		if (!services.containsKey("ALMemory")) {
			services.put("ALMemory", sess.service("ALMemory"));			
		}
		com.aldebaran.qimessaging.Object memory = services.get("ALMemory");
		com.aldebaran.qimessaging.Object subscriber = memory.<com.aldebaran.qimessaging.Object>call("subscriber", event).get();
		subscriber.connect("signal::()", "onEvent::(m)", new java.lang.Object() {
			@SuppressWarnings("unused")
			public void onEvent(java.lang.Object event) {
				listener.callback(event);
			}
		});
		}
		catch (Exception e) {
			throw new DialException("could not set callback " + e);
		}
	}


	public String getIP() {
		return ip;
	}
	

}

