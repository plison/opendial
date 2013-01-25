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
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
import opendial.gui.ToolkitMenu;
import opendial.inference.ImportanceSampling;
import opendial.inference.VariableElimination;
import opendial.inference.NaiveInference;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class InferenceOptionsPanel extends JDialog {

	// logger
	public static Logger log = new Logger("InferenceOptionsPanel", Logger.Level.NORMAL);
	
	JPanel samplingOptionBox;
		
	public InferenceOptionsPanel(Window owner) {
		super(owner,Dialog.ModalityType.DOCUMENT_MODAL);
		setTitle("Inference Algorithm: Settings");
		
		
		Container contentPane = getContentPane();
		
		contentPane.setLayout(new BorderLayout());

		Container allOptions = new Container();
		allOptions.setLayout(new BorderLayout());
		
		Container algorithmBox0 = new Container();
		algorithmBox0.setLayout(new BorderLayout());
		algorithmBox0.add(new JLabel("  "), BorderLayout.NORTH);
		algorithmBox0.add(new JLabel("     "), BorderLayout.WEST);
		algorithmBox0.add(new JLabel("     "), BorderLayout.EAST);
		JPanel algorithmBox = new JPanel();
		algorithmBox.setLayout(new GridLayout(8, 1));
		algorithmBox.setBorder(BorderFactory.createTitledBorder("Inference algorithm"));
		algorithmBox.add(new JLabel("Select the type of inference algorithm: "));
		final JRadioButton naiveInference = new JRadioButton("Naive inference (slowest, only for debugging)");
		naiveInference.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				samplingOptionBox.setVisible(false); repaint(); }});
		final JRadioButton variableElimination = new JRadioButton("Variable Elimination (efficient exact inference)");
		variableElimination.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				samplingOptionBox.setVisible(false); repaint(); }});
		final JRadioButton importanceSampling = new JRadioButton("Importance Sampling (efficient approximate inference)");
		importanceSampling.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				samplingOptionBox.setVisible(true); repaint(); }});
		if (Settings.getInstance().inferenceAlgorithm.equals(NaiveInference.class)) {
			naiveInference.setSelected(true);
		}
		else if (Settings.getInstance().inferenceAlgorithm.equals(VariableElimination.class)) {
			variableElimination.setSelected(true);
		}
		else if (Settings.getInstance().inferenceAlgorithm.equals(ImportanceSampling.class)) {
			importanceSampling.setSelected(true);
		}
		ButtonGroup group = new ButtonGroup();
		group.add(naiveInference);
		group.add(variableElimination);
		group.add(importanceSampling);
		algorithmBox.add(new JLabel(" "));
		algorithmBox.add(naiveInference);
		algorithmBox.add(new JLabel(" "));
		algorithmBox.add(variableElimination);
		algorithmBox.add(new JLabel(" "));
		algorithmBox.add(importanceSampling);
		algorithmBox.add(new JLabel(" "));
		algorithmBox0.add(algorithmBox, BorderLayout.CENTER);
		algorithmBox0.add(new JLabel("  "), BorderLayout.SOUTH);
		allOptions.add(algorithmBox0, BorderLayout.NORTH);
		
		Container samplingOptionBox0 = new Container();
		samplingOptionBox0.setLayout(new BorderLayout());
		samplingOptionBox0.add(new JLabel("  "), BorderLayout.NORTH);
		samplingOptionBox0.add(new JLabel("     "), BorderLayout.WEST);
		samplingOptionBox0.add(new JLabel("     "), BorderLayout.EAST);	
		samplingOptionBox = new JPanel();
		samplingOptionBox.setLayout(new BorderLayout());
		samplingOptionBox.setBorder(BorderFactory.createTitledBorder("Sampling parameters"));
		samplingOptionBox.add(new JLabel(" Number of samples to draw per query:     "), BorderLayout.WEST);
		
	//	NumberFormat format = NumberFormat.getNumberInstance();
		final JFormattedTextField sampleNumber = new JFormattedTextField();
		sampleNumber.setText(""+Settings.getInstance().nbSamples);
		samplingOptionBox.add(sampleNumber, BorderLayout.CENTER);
		samplingOptionBox.add(new JLabel("                          "),BorderLayout.EAST);
		samplingOptionBox.add(new JLabel(" "),BorderLayout.SOUTH);
		
		samplingOptionBox.setVisible(importanceSampling.isSelected());
		samplingOptionBox0.add(samplingOptionBox, BorderLayout.CENTER);	
		allOptions.add(samplingOptionBox0, BorderLayout.SOUTH);
		
		contentPane.add(allOptions, BorderLayout.NORTH);
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
				if (naiveInference.isSelected()) {
					Settings.getInstance().inferenceAlgorithm = NaiveInference.class;
				}
				else if (variableElimination.isSelected()) {
					Settings.getInstance().inferenceAlgorithm = VariableElimination.class;
				}
				else if (importanceSampling.isSelected()) {
					Settings.getInstance().inferenceAlgorithm = ImportanceSampling.class;
					try {
						int number = Integer.parseInt(sampleNumber.getText());
						Settings.getInstance().nbSamples = number;
					}
					catch (NumberFormatException e2) {
						log.warning("number of samples has an invalid format: " + sampleNumber.getText());
					}
				}
				setVisible(false); } 
			});
		okcancelBox.add(okButton, BorderLayout.CENTER);
		okcancelBox.add(new JLabel("  "), BorderLayout.EAST);
		okcancelBox1.add(okcancelBox, BorderLayout.EAST);
		okcancelBox1.add(new JLabel("  "), BorderLayout.SOUTH);
		contentPane.add(okcancelBox1, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(okButton);
		
		setLocation(new Point(250, 250));
		setMinimumSize(new Dimension(500,420));
		setPreferredSize(new Dimension(500,420));
		pack();
		setVisible(true);
	}
	
}
