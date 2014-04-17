// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import opendial.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.modules.DialogueRecorder;
import opendial.modules.ForwardPlanner;
import opendial.modules.Module;
import opendial.modules.RewardLearner;
import opendial.modules.WizardControl;
import opendial.modules.WizardLearner;
import opendial.utils.ReflectionUtils;


/**
 * Panel to load or unload modules.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-01-16 02:21:14 #$
 *
 */
@SuppressWarnings("serial")
public class ModulesPanel extends JDialog {

	public static Logger log = new Logger("ModulesPanel", Logger.Level.DEBUG);

	GUIFrame frame;

	CheckBoxList listBox;
	JTable table;	
	JButton okButton;

	protected Map<String,Class<Module>> classes;

	Properties shownParams = new Properties();


	/**
	 * Creates a new module panel attached to the GUI frame.
	 * 
	 * @param frame the GUI frame.
	 */
	public ModulesPanel(final GUIFrame frame) {
		super(frame.getFrame(),Dialog.ModalityType.DOCUMENT_MODAL);
		this.frame = frame;

		setTitle("Module Settings");
		//	shownParams.putAll(settings.params);

		Container contentPane = getContentPane();

		contentPane.setLayout(new BorderLayout());

		Container moduleOptions = new Container();
		moduleOptions.setLayout(new BoxLayout(moduleOptions, BoxLayout.PAGE_AXIS));

		listBox = new CheckBoxList();
		listBox.setLayoutOrientation(JList.VERTICAL_WRAP);
		listBox.setVisibleRowCount(4);
		JScrollPane scrollPane = new JScrollPane(listBox);
		scrollPane.setPreferredSize(new Dimension(450, 180));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Loaded modules: " ));		
		moduleOptions.add(scrollPane);

		table = new JTable();
		table.setPreferredScrollableViewportSize(new Dimension(500, 120));
		table.setFillsViewportHeight(true);
		JScrollPane scrollPane2 = new JScrollPane(table);
		scrollPane2.setBorder(BorderFactory.createTitledBorder("Module-specific parameters"));
		moduleOptions.add(scrollPane2);

		Container okcancelBox1 = new Container();
		okcancelBox1.setLayout(new BorderLayout());
		okcancelBox1.add(new JLabel("  "), BorderLayout.NORTH);
		Container okcancelBox = new Container();
		okcancelBox.setLayout(new BorderLayout());
		JButton cancelButton = new JButton("  Cancel  ");
		okButton = new JButton("     OK     ");
		okcancelBox.add(cancelButton, BorderLayout.WEST);
		okcancelBox.add(okButton, BorderLayout.CENTER);
		okcancelBox.add(new JLabel("  "), BorderLayout.EAST);
		okcancelBox1.add(okcancelBox, BorderLayout.EAST);
		okcancelBox1.add(new JLabel("  "), BorderLayout.SOUTH);
		contentPane.add(okcancelBox1, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(okButton);
		contentPane.add(moduleOptions);

		fillListBox();
		updateParamModel();
		updateButtonStatus();

		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { setVisible(false); } });


		table.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("tableCellEditor".equals(evt.getPropertyName())) {
					updateButtonStatus(); 
				}
			}
		});

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { 
				updateSettings();
			} 
		});


		setLocation(new Point(250, 250));
		setMinimumSize(new Dimension(500,350));
		setPreferredSize(new Dimension(500,350));
		pack();
		setVisible(true);
	}


	/**
	 * Updates the activation status of the OK button.
	 */
	private void updateButtonStatus() {
		if (table.isEditing()) {
			okButton.setEnabled(false);
			return;
		}
		for (int r = 0; r < table.getRowCount(); r++) {
			if (table.getValueAt(r, 0) == null || table.getValueAt(r, 1) == null
					|| table.getValueAt(r, 0).toString().trim().equals("")
					|| table.getValueAt(r, 1).toString().trim().equals("")) {
				okButton.setEnabled(false);
				return;
			}
		}
		okButton.setEnabled(true);
	}


	/**
	 * Fills the list of modules for the dialogue system.
	 */
	private void fillListBox() {
		classes = ReflectionUtils.findImplementingClasses(Module.class, Package.getPackage("opendial"));

		JCheckBox[] newList = new JCheckBox[classes.size()];
		int i = 0;
		for (String className : classes.keySet()) {  
			Class<Module> cls = classes.get(className);
			newList[i] = new JCheckBox(cls.getSimpleName());
			newList[i].setSelected(frame.getSystem().getModule(cls) != null);
			newList[i].setEnabled(true);
			if (cls.equals(GUIFrame.class) || cls.equals(DialogueRecorder.class) 
					|| cls.equals(ForwardPlanner.class) || cls.equals(WizardLearner.class) 
					|| cls.equals(RewardLearner.class)  || cls.equals(WizardControl.class)) {
				newList[i].setEnabled(false);
			}
			i++;
		}

		listBox.setListData(newList);
	}


	/**
	 * Updates the table model for the module-specific parameters.
	 */
	private void updateParamModel() {
		String[] columnNames = {"Parameter", "Value"};
		Object[][] data = new Object[shownParams.size()][2];
		int  i = 0;
		for (String param : shownParams.stringPropertyNames()) {
			data[i][0] = param;
			data[i][1] = shownParams.get(param);
			i++;
		}

		DefaultTableModel dataModel = new DefaultTableModel();
		for (int col = 0; col < columnNames.length; col++) {
			dataModel.addColumn(columnNames[col]);
		}
		for (int row = 0; row < shownParams.size(); row++) {
			dataModel.addRow(data[row]);
		}
		table.setModel(dataModel);
		updateButtonStatus();
	}


	/**
	 * Updates the system settings in accordance with the current content
	 * of the panel.
	 */
	protected void updateSettings() {

		Settings settings = frame.getSystem().getSettings().copy();
		settings.modules.clear();
		for (int i = 0 ; i < listBox.getModel().getSize() ; i++) {
			JCheckBox checkbox = (JCheckBox)listBox.getModel().getElementAt(i);
			if (checkbox.isSelected() && checkbox.isEnabled()) {
				settings.modules.add(classes.get(checkbox.getText()));
				log.debug("added " + checkbox.getText() + " to the settings");
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
				log.debug("setting " + param + " = " + value);
			}
		}

		frame.getSystem().changeSettings(settings);
		setVisible(false);
	}


	/**
	 * Checkbox list for the modules
	 */
	class CheckBoxList extends JList {

		public CheckBoxList() {
			setCellRenderer(new CellRenderer());
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int index = locationToIndex(e.getPoint());
					if (index != -1) {
						JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
						if (checkbox != null && checkbox.isEnabled()) {
							checkbox.setSelected(!checkbox.isSelected());

							try {
								Constructor<Module> constructor = 
										classes.get(checkbox.getText()).getConstructor(DialogueSystem.class);
								constructor.newInstance(frame.getSystem());
							}
							catch (InvocationTargetException f) {
								if (f.getTargetException() instanceof Module.MissingParameterException) {
									for (String param : ((Module.MissingParameterException)f.
											getTargetException()).getMissingParameters()) {
										if (checkbox.isSelected()) {
											shownParams.put(param, "");
										}
										else {
											shownParams.remove(param);
										}
									}
									updateParamModel();
								}
							}
							catch (Exception f) {
								log.warning("no valid constructor for class " + checkbox.getText() + ": " + f);
								checkbox.setEnabled(false);
							}
							repaint();
						}
					}

				}
			}
					);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		
		/**
		 * Cell renderer.
		 */
		protected class CellRenderer implements ListCellRenderer  {
			@Override
			public Component getListCellRendererComponent(
					JList list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				JCheckBox checkbox = (JCheckBox) value;
				checkbox.setBackground(getBackground());
				checkbox.setForeground(getForeground());
				//    checkbox.setEnabled(isEnabled());
				checkbox.setFont(getFont());
				checkbox.setFocusPainted(false);
				checkbox.setBorder(isSelected ?
						UIManager.getBorder( "List.focusCellHighlightBorder") : new EmptyBorder(1, 1, 1, 1));
				return checkbox;
			}
		}

	}


}

