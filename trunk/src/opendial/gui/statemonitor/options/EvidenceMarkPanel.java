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

package opendial.gui.statemonitor.options;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import opendial.arch.Settings;
import opendial.arch.Logger;
import opendial.bn.Assignment;
import opendial.bn.distribs.continuous.ContinuousProbDistribution;
import opendial.bn.nodes.ChanceNode;
import opendial.bn.values.Value;
import opendial.gui.ToolkitMenu;
import opendial.inference.NaiveInference;
import opendial.state.DialogueState;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class EvidenceMarkPanel extends JDialog {

	// logger
	public static Logger log = new Logger("InferencePanel", Logger.Level.NORMAL);
	
	JPanel samplingOptionBox;
		
	public EvidenceMarkPanel(Window owner, final ChanceNode node, final DialogueState state) {
		super(owner,Dialog.ModalityType.DOCUMENT_MODAL);
		setTitle("Mark node as evidence");
		
		Container contentPane = getContentPane();
		
		contentPane.setLayout(new BorderLayout());

		Container evidenceValueBox0 = new Container();
		evidenceValueBox0.setLayout(new BorderLayout());
		evidenceValueBox0.add(new JLabel("  "), BorderLayout.NORTH);
		evidenceValueBox0.add(new JLabel("     "), BorderLayout.WEST);
		evidenceValueBox0.add(new JLabel("     "), BorderLayout.EAST);
		Container evidenceValue = new Container();
		evidenceValue.setLayout(new GridLayout(0,2));
		evidenceValue.add(new JLabel("Evidence value for " + node.getId()+": "));
		
		final JComboBox possibleVals = new JComboBox();
		if (!(node.getDistrib() instanceof ContinuousProbDistribution)) {
			Set<Value> values = node.getValues();
			int i = 0;
			for (Value value : values) {
				possibleVals.addItem(value);
			}
		}
		// TODO: take care of continuous distributions as well
		
		evidenceValue.add(possibleVals);			

		evidenceValueBox0.add(evidenceValue, BorderLayout.CENTER);
		contentPane.add(evidenceValueBox0, BorderLayout.NORTH);
		contentPane.add(new JLabel(" "), BorderLayout.CENTER);
		
		Container okcancelBox1 = new Container();
		okcancelBox1.setLayout(new BorderLayout());
		okcancelBox1.add(new JLabel("  "), BorderLayout.NORTH);
		Container okcancelBox = new Container();
		okcancelBox.setLayout(new BorderLayout());
		JButton cancelButton = new JButton("  Cancel  ");
		cancelButton.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent e) { setVisible(false); } });
		okcancelBox.add(cancelButton, BorderLayout.WEST);
		JButton okButton = new JButton("     OK     ");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { 
				Value selectedValue = (Value)possibleVals.getSelectedItem();
				state.addEvidence(new Assignment(node.getId(), selectedValue));
				setVisible(false); } 
			});
		okcancelBox.add(okButton, BorderLayout.CENTER);
		okcancelBox.add(new JLabel("  "), BorderLayout.EAST);
		okcancelBox1.add(okcancelBox, BorderLayout.EAST);
		okcancelBox1.add(new JLabel("  "), BorderLayout.SOUTH);
		contentPane.add(okcancelBox1, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(okButton);
		setLocation(new Point(300, 300));
		setMinimumSize(new Dimension(450,150));
		setPreferredSize(new Dimension(450,150));
		pack();
		setVisible(true);
	}
	
}
