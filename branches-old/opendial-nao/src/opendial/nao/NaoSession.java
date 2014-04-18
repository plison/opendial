// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.nao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.aldebaran.qimessaging.*;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.modules.Module;

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
		synchronized(fut) {
		    fut.wait(1000);
		}
		services = new HashMap<String, com.aldebaran.qimessaging.Object>();
		services.put("ALMemory", sess.service("ALMemory"));
		}
		catch (Exception e) {
			throw new DialException("could not start Nao session: " + e);
		}
	}
	
	
	public static NaoSession grabSession(Settings settings) throws DialException {
		if (!settings.params.containsKey("nao_ip")) {
			throw new Module.MissingParameterException(Arrays.asList("nao_ip"));
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


	public String getIP() {
		return ip;
	}
	
	

}
