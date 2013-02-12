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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import opendial.arch.Settings;
import opendial.arch.Logger;
import opendial.inference.NaiveInference;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class NodeEditPanel extends JDialog {

	// logger
	public static Logger log = new Logger("NodeEditPanel", Logger.Level.DEBUG);
	
	
	final JTextArea valueField;
	
	public NodeEditPanel(Window owner) {
		super(owner,Dialog.ModalityType.DOCUMENT_MODAL);
		setTitle("Create node content");
	
		final JTextField idField = new JTextField();

		valueField = new JTextArea(2,20);
		
		final Map<String,Float> currentDistrib = new HashMap<String,Float>();
		
		DefaultTableModel tableModel = new DefaultTableModel() {
			
		    public String getColumnName(int col) {
		        switch (col) {
		        	case 1: return idField.getText(); 
		        	default : return ""; 
		    }
		    }
		    public int getRowCount() {return getValues().size(); }
		    public int getColumnCount() { return 4; }
		    public Object getValueAt(int row, int col) {
		        if (col == 0 ) {
		        	return "P(";
		        }
		        else if (col == 2) {
		        	return ") = ";
		        }
		        else if (col == 3) {
		        	return currentDistrib.containsKey(getValues().get(row)) ? currentDistrib.get(getValues().get(row)) : "0.0f";
		        }
		        else {
		        	return getValues().get(row);
		        }
		    }
		    public boolean isCellEditable(int row, int col)
		        { return (col==3); }
		    public void setValueAt(Object value, int row, int col) {
		    	try {
			    	currentDistrib.put(getValues().get(row), Float.parseFloat(value.toString()));
		    	}
		    	catch (NumberFormatException e) {}
		    }
		   
		};
		JTable table = new JTable(tableModel);
		table.setCellSelectionEnabled(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(25);
		table.getColumnModel().getColumn(1).setPreferredWidth(60);
		table.getColumnModel().getColumn(2).setPreferredWidth(25);
		table.getColumnModel().getColumn(3).setPreferredWidth(40);
		Container contentPane = getContentPane();
		
		contentPane.setLayout(new BorderLayout());

		Container allOptions = new Container();
		allOptions.setLayout(new BorderLayout());
		
		Container idBox0 = new Container();
		idBox0.setLayout(new BorderLayout());
		idBox0.add(new JLabel("  "), BorderLayout.NORTH);
		idBox0.add(new JLabel("     "), BorderLayout.WEST);
		idBox0.add(new JLabel("     "), BorderLayout.EAST);
		JPanel idBox = new JPanel();
		idBox.setLayout(new GridLayout(0, 3));
		idBox.setBorder(BorderFactory.createTitledBorder("Identifier"));
		idBox.add(new JLabel("Node name: "));
		idBox.add(idField);
		idBox.add(new JLabel("      (must be unique) "));
		idBox0.add(idBox, BorderLayout.CENTER);
		idBox0.add(new JLabel("  "), BorderLayout.SOUTH);
		allOptions.add(idBox0, BorderLayout.NORTH);
		
		Container valueBox0 = new Container();
		valueBox0.setLayout(new BorderLayout());
		valueBox0.add(new JLabel("  "), BorderLayout.NORTH);
		valueBox0.add(new JLabel("     "), BorderLayout.WEST);
		valueBox0.add(new JLabel("     "), BorderLayout.EAST);
		JPanel valueBox = new JPanel();
		valueBox.setLayout(new GridLayout(0, 1));
		valueBox.setBorder(BorderFactory.createTitledBorder("Values"));
		valueBox.add(new JLabel("Enter the values for the nodes, separated with commas: "));
		Container valueBox2 = new Container();
		valueBox2.setLayout(new BorderLayout());
		valueField.setSize(new Dimension(300,50));
		JScrollPane valuePane = new JScrollPane(valueField); 
		valueBox2.add(valuePane, BorderLayout.WEST);
		JButton updateButton2 = new JButton(" Update");
		updateButton2.addActionListener(new UpdateActionListener(tableModel));
		valueBox2.add(updateButton2, BorderLayout.EAST);
		valueBox.add(valueBox2);
		valueBox0.add(valueBox, BorderLayout.CENTER);
		valueBox0.add(new JLabel("  "), BorderLayout.SOUTH);
		allOptions.add(valueBox0, BorderLayout.CENTER);
		
		Container tableBox0 = new Container();
		tableBox0.setLayout(new BorderLayout());
		tableBox0.add(new JLabel("  "), BorderLayout.NORTH);
		tableBox0.add(new JLabel("     "), BorderLayout.WEST);
		tableBox0.add(new JLabel("     "), BorderLayout.EAST);
		JPanel tableBox = new JPanel();
		tableBox.setLayout(new BorderLayout());
		tableBox.setBorder(BorderFactory.createTitledBorder("Probability table"));
		
		table.setModel(tableModel);
		JScrollPane scpane = new JScrollPane(table);
		scpane.setPreferredSize(new Dimension(200,160));
		table.setFillsViewportHeight(true);
		tableBox.add(scpane, BorderLayout.CENTER);
		tableBox0.add(tableBox, BorderLayout.CENTER);
		tableBox0.add(new JLabel("  "), BorderLayout.SOUTH);
		allOptions.add(tableBox0, BorderLayout.SOUTH);
	
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
			//	if (button.isSelected()) {
			//		Settings.getInstance().setInferenceAlgorithm(NaiveInference.class);
			//	}
				setVisible(false); } 
			});
		okcancelBox.add(okButton, BorderLayout.CENTER);
		okcancelBox.add(new JLabel("  "), BorderLayout.EAST);
		okcancelBox1.add(okcancelBox, BorderLayout.EAST);
		okcancelBox1.add(new JLabel("  "), BorderLayout.SOUTH);
		contentPane.add(okcancelBox1, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(okButton);
		
		setLocation(new Point(250, 250));
		setMinimumSize(new Dimension(500,520));
		setPreferredSize(new Dimension(500,520));
		pack();
		setVisible(true);
	}
	
	 private List<String> getValues() {
	    	String[] array = valueField.getText().replace(" ", "").split(",");
	    	Set<String> values = new HashSet<String>();
	    	for (int i = 0 ; i < array.length ; i++) {
	    		values.add(array[i]);
	    	}
	    	List<String> values2 = new ArrayList(values);
	    	Collections.sort(values2);
	    	return values2;
	    }
	
	final class UpdateActionListener implements ActionListener {

		DefaultTableModel model;
		
		public UpdateActionListener(DefaultTableModel model) {
			this.model = model;
		}
		/**
		 *
		 * @param arg0
		 */
		@Override
		public void actionPerformed(ActionEvent arg0) {
			 model.fireTableDataChanged();
			 model.fireTableStructureChanged();
		}
		
	}
}
