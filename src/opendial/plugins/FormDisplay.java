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
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.Value;
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
		if (running && updatedVars.contains("a_m") && state.hasChanceNode("a_m") 
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
			JOptionPane.showMessageDialog(parentC, scrollPane, "Results of the form-filling process", JOptionPane.INFORMATION_MESSAGE);

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

}
