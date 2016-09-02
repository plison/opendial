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

import java.awt.Component;
import java.awt.Font;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.gui.GUIFrame;
import opendial.modules.Module;

/**
 * Small module to show the results of the form-filling process in a table.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class FormDisplay implements Module {

	DialogueSystem system;
	
	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	JFrame frame;
	
	boolean running = false;
	
	public FormDisplay(DialogueSystem system) {
		this.system = system;
	}
	
	@Override
	public void start() {
		running = true;
	}

	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		if (running && updatedVars.contains("u_m") && state.hasChanceNode("a_m") 
				&& state.queryProb("a_m").getBest().toString().equals("EndDialogue")) {
			
			String[] columns = {"Slot name", "Slot value"};
			String[] groundedSlots = (state.queryProb("grounded").getBest()).getSubValues().stream()
										.map(v -> v.toString()).toArray(size -> new String[size]);
			
			String[] slotValues = new String[groundedSlots.length];
			for (int i = 0 ; i < groundedSlots.length; i++) {
				String slot = groundedSlots[i];
				Value slotValue = state.queryProb(slot).getBest();
				slotValues[i] = slotValue.toString();
			}
			String[][] data = new String[slotValues.length][2];
			for (int i=0 ; i < slotValues.length ; i++) {
				for (int j = 0 ; j < 2 ; j++) {
					data[i][0] = groundedSlots[i];
					data[i][1] = slotValues[i];
				}
			}
			JTable table = new JTable(data, columns);
		    table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 16));
			table.setFont(new Font("SansSerif",Font.PLAIN, 16));
			table.setRowHeight(table.getRowHeight()+5);
			JScrollPane scrollPane = new JScrollPane(table);
			table.setFillsViewportHeight(true);

			Component parentC = (system.getSettings().showGUI)? system.getModule(GUIFrame.class).getFrame() : null;
			
			new Thread(() -> JOptionPane.showMessageDialog(parentC, scrollPane, 
					"Results of the form-filling process", JOptionPane.INFORMATION_MESSAGE)).start();

		}
	}

	@Override
	public void pause(boolean toPause) {
		running = !toPause;
	}

	@Override
	public boolean isRunning() {
		return running;
	}


public static final class WatsonConvert implements Function<List<String>,Value> {

	String[] months = {"January", "February", "March", "April", "May", "June",
			"July", "August", "September", "October", "November", "December"};
	
	@Override
	public Value apply(List<String> args) {
		if (args.size()!=1) {
			throw new RuntimeException("Illegal number of arguments: " + args.size());
		}
		String arg = args.get(0);
		if (arg.startsWith("$")) {
		    return ValueFactory.create(arg.replace(",","").replace(" ", ""));
		}
		else if (arg.length() == 10 && (arg.substring(0,2).equals("19") || arg.substring(0,2).equals("20"))) {
			String year = arg.substring(0, 4);
			String month = arg.substring(5, 7);
			String day = (arg.charAt(8)=='0')? arg.substring(9, 10) : arg.substring(8,10);
			String monthName = months[Integer.parseInt(month)-1];
			return ValueFactory.create(monthName + " " + day + " " + year);
		}
		else if (arg.startsWith("P") && arg.length()==3) {
			char nb = arg.charAt(1);
			if (arg.charAt(2)=='D') {
				return ValueFactory.create(nb + " days");
			}
			else if (arg.charAt(2)=='W') {
				return ValueFactory.create(nb + " weeks");
			}
			return ValueFactory.create(nb + " " + arg.charAt(2));
		}
		System.out.print("creating the value " + ValueFactory.create(arg));
		return ValueFactory.create(arg);
	}
	
}
}


