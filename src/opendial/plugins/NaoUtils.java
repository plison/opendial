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
import opendial.Settings;

import com.aldebaran.qi.Application;
import com.aldebaran.qi.Session;

public class NaoUtils {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");
	
	private static String ip;
	private static Session sess;
	
	public static Session grabSession(Settings settings) {
		if (!settings.params.containsKey("nao_ip")) {
			throw new RuntimeException("Missing parameter: nao_ip");
		}
		if (sess == null) {
			ip = settings.params.getProperty("nao_ip");
			log.info("Creating new Nao session with " + ip);
			String address = "tcp://" + ip +  ":9559";
			Application a = new Application(new String[0], address);
			a.start();
			sess = a.session();
			log.info("Session successfully created!");
		}
		return sess;
	}

}
