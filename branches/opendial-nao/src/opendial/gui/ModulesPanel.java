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

package opendial.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import opendial.arch.Settings;
import opendial.arch.Logger;
import opendial.modules.DialogueRecorder;
import opendial.modules.ForwardPlanner;
import opendial.modules.Module;
import opendial.modules.WizardControl;
import opendial.modules.WizardLearner;
import opendial.utils.ReflectionUtils;


/**
 * Panel to modify the system preferences.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-01-16 02:21:14 #$
 *
 */
public class ModulesPanel extends JDialog {

	DefaultTableModel dataModel;
	JTable table;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// logger
	public static Logger log = new Logger("ModulesPanel", Logger.Level.DEBUG);
			
	
	public ModulesPanel(final GUIFrame frame) {
		super(frame.getFrame(),Dialog.ModalityType.DOCUMENT_MODAL);
		final Settings settings = frame.getSystem().getSettings();
		setTitle("Module Settings");
		
		Container contentPane = getContentPane();
		
		contentPane.setLayout(new BorderLayout());

		Container moduleOptions = new Container();
		moduleOptions.setLayout(new BoxLayout(moduleOptions, BoxLayout.PAGE_AXIS));

		final CheckBoxList listBox = new CheckBoxList();
		
		listBox.setLayoutOrientation(JList.VERTICAL_WRAP);
		listBox.setVisibleRowCount(4);
		final List<Class<Module>> classes = ReflectionUtils.findImplementingClasses(Module.class, Package.getPackage("opendial"));
		
		JCheckBox[] newList = new JCheckBox[classes.size()];
	       
		for (int i = 0 ; i < classes.size() ; i++) {
			Class<Module> cls = classes.get(i);
			newList[i] = new JCheckBox(cls.getSimpleName());
			newList[i].setSelected(frame.getSystem().getModule(cls) != null);
			newList[i].setEnabled(false);
			for (int j = 0 ; j < cls.getConstructors().length ; j++) {
				if (cls.getConstructors()[j].getParameterTypes().length == 0 
						&& !cls.equals(GUIFrame.class) && !cls.equals(DialogueRecorder.class) 
						&& !cls.equals(ForwardPlanner.class) && !cls.equals(WizardLearner.class) 
						&& !cls.equals(WizardControl.class)) {
					newList[i].setEnabled(true);
				}
			}
		}
		listBox.setListData(newList);

		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setPreferredSize(new Dimension(450, 180));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Loaded modules: " ));		
	
		moduleOptions.add(scrollPane);

	    String[] columnNames = {"Parameter", "Value"};
	    Object[][] data = new Object[settings.params.size() + 3][2];
	    int i = 0;
	    for (String param : settings.params.keySet()) {
	    	data[i][0] = param;
	    	data[i][1] = settings.params.get(param);
	    	i++;
	    }

	    dataModel = new DefaultTableModel();
	    for (int col = 0; col < columnNames.length; col++) {
	        dataModel.addColumn(columnNames[col]);
	    }
	    for (int row = 0; row < settings.params.size() + 3; row++) {
	        dataModel.addRow(data[row]);
	    }

	    table = new JTable(dataModel);
	    table.setPreferredScrollableViewportSize(new Dimension(500, 120));
	    table.setFillsViewportHeight(true);


	    //Create the scroll pane and add the table to it.
	    JScrollPane scrollPane2 = new JScrollPane(table);
	    scrollPane2.setBorder(BorderFactory.createTitledBorder("Module-specific parameters"));
	    //Add the scroll pane to this panel.
	    moduleOptions.add(scrollPane2);

		contentPane.add(moduleOptions);
		
		Container okcancelBox1 = new Container();
		okcancelBox1.setLayout(new BorderLayout());
		okcancelBox1.add(new JLabel("  "), BorderLayout.NORTH);
		Container okcancelBox = new Container();
		okcancelBox.setLayout(new BorderLayout());
		JButton cancelButton = new JButton("  Cancel  ");
		cancelButton.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent e) { setVisible(false); } });
		okcancelBox.add(cancelButton, BorderLayout.WEST);
		final JButton okButton = new JButton("     OK     ");
		table.addPropertyChangeListener(new PropertyChangeListener() {
	        public void propertyChange(PropertyChangeEvent evt) {
	            if ("tableCellEditor".equals(evt.getPropertyName())) {
	            	okButton.setEnabled(!table.isEditing());    
	            }
	        }
	    });
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { 

				Settings settings = frame.getSystem().getSettings().copy();
				settings.modules.clear();
				for (int i = 0 ; i < listBox.getModel().getSize() ; i++) {
					JCheckBox checkbox = (JCheckBox)listBox.getModel().getElementAt(i);
					if (checkbox.isSelected() && checkbox.isEnabled()) {
						for (Class<Module> cls : classes) {
							if (cls.getSimpleName().equals(checkbox.getText())) {
								try {
									settings.modules.add(cls.newInstance());
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						}
					}					
				}
				settings.params.clear();
				for (int i = 0 ; i < table.getModel().getRowCount() ; i++) {
					if (table.getModel().getValueAt(i, 0) == null 
							|| table.getModel().getValueAt(i, 1) == null) {
						continue;
					}
					String param = table.getModel().getValueAt(i, 0).toString().trim();
					String value = table.getModel().getValueAt(i, 1).toString().trim();
					if (param.length() > 0 && value.length() > 0) {
						settings.params.put(param, value);
					}
				}
				
				frame.getSystem().changeSettings(settings);
				setVisible(false);
				 } 
			});
		okcancelBox.add(okButton, BorderLayout.CENTER);
		okcancelBox.add(new JLabel("  "), BorderLayout.EAST);
		okcancelBox1.add(okcancelBox, BorderLayout.EAST);
		okcancelBox1.add(new JLabel("  "), BorderLayout.SOUTH);
		contentPane.add(okcancelBox1, BorderLayout.SOUTH);
		
		getRootPane().setDefaultButton(okButton);
		
		setLocation(new Point(250, 250));
		setMinimumSize(new Dimension(500,350));
		setPreferredSize(new Dimension(500,350));
		pack();
		setVisible(true);
	}
	
	

}


class CheckBoxList extends JList {
  
   public CheckBoxList() {
      setCellRenderer(new CellRenderer());
      addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
               int index = locationToIndex(e.getPoint());
               if (index != -1) {
                  JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                  if (checkbox != null && checkbox.isEnabled()) {
                  checkbox.setSelected(!checkbox.isSelected());
                  repaint();
                  }
               }
            }
         }
      );

      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   }

   protected class CellRenderer implements ListCellRenderer  {
      public Component getListCellRendererComponent(
                    JList list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
         JCheckBox checkbox = (JCheckBox) value;
         checkbox.setBackground(isSelected && checkbox.isEnabled() ?
                 getSelectionBackground() : getBackground());
         checkbox.setForeground(isSelected && checkbox.isEnabled() ?
                 getSelectionForeground() : getForeground());
     //    checkbox.setEnabled(isEnabled());
         checkbox.setFont(getFont());
         checkbox.setFocusPainted(false);
         checkbox.setBorderPainted(true);
         checkbox.setBorder(isSelected ?
          UIManager.getBorder( "List.focusCellHighlightBorder") : new EmptyBorder(1, 1, 1, 1));
         return checkbox;
      }
   }
}

