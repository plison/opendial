// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.modules;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.aldebaran.qimessaging.*;
import com.aldebaran.qimessaging.Object;

import opendial.arch.Logger;
import opendial.arch.Settings;

public class NaoSession extends QimessagingService {

	// logger
	public static Logger log = new Logger("NaoSession", Logger.Level.DEBUG);
	
	Object memory;
	private static NaoSession session;
	
	Session sess;
			
	private NaoSession() throws Exception {
	    Application app = new Application(new String[0]);
		sess = new Session();
		Future<Void> fut = sess.connect("tcp://"+Settings.getInstance().nao.ip +":9559");
		synchronized(fut) {
		    fut.wait(1000);
		}
		memory = sess.service("ALMemory");

	}
	
	public static NaoSession grabSession() throws Exception {
		if (session == null) {
			session = new NaoSession();
		}
		return session;
	}
	
	
	public com.aldebaran.qimessaging.Object getService(String serviceName) throws Exception {
		return sess.service(serviceName);
	}

}

