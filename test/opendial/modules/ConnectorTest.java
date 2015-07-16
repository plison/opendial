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

package opendial.modules;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import opendial.DialogueSystem;

import org.junit.Test;

public class ConnectorTest {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	@Test
	public void remoteConnection()
			throws UnknownHostException, InterruptedException {
		DialogueSystem system1 = new DialogueSystem();
		system1.getSettings().showGUI = false;
		DialogueSystem system2 = new DialogueSystem();
		system2.getSettings().showGUI = false;
		String address = system1.getLocalAddress();
		system1.startSystem();
		system2.startSystem();
		system2.connectTo(address.split(":")[0],
				Integer.parseInt(address.split(":")[1]));
		system2.getSettings().invertedRole = true;
		Thread.sleep(200);
		system1.addUserInput("hello, world!");
		Thread.sleep(200);
		String record1 = system1.getModule(DialogueRecorder.class).getRecord();
		record1 = record1.replaceAll(system2.getLocalAddress(), "");
		String record2 = system2.getModule(DialogueRecorder.class).getRecord();
		record2 = record2.replaceAll(system1.getLocalAddress(), "");
		assertEquals(record1, record2);
		Map<String, Double> response = new HashMap<String, Double>();
		response.put("hello back", 0.7);
		response.put("elbow black", 0.1);
		system2.addUserInput(response);
		Thread.sleep(200);
		record1 = system1.getModule(DialogueRecorder.class).getRecord();
		record1 = record1.replaceAll(system2.getLocalAddress(), "");
		assertEquals(record1,
				"<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
						+ "<interaction><!--Connected to --><userTurn>"
						+ "<variable id=\"u_u\"><value>hello, world!</value></variable></userTurn>"
						+ "<systemTurn><variable id=\"u_m\"><value prob=\"0.7\">hello back</value>"
						+ "<value prob=\"0.1\">elbow black</value></variable>"
						+ "</systemTurn></interaction>");
		record2 = system2.getModule(DialogueRecorder.class).getRecord();
		record2 = record2.replaceAll(system1.getLocalAddress(), "");
		assertEquals(record1, record2);

	}
}
