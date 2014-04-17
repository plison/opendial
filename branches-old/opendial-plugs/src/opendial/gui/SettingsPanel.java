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
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import opendial.arch.Logger;
import opendial.arch.Settings;


/**
 * Panel to modify the system preferences.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class SettingsPanel extends JDialog {
 

	private static final long serialVersionUID = 1L;
	// logger
	public static Logger log = new Logger("SettingsPanel", Logger.Level.DEBUG);
			
	
	/**
	 * Creates a new settings panel attached to the GUI frame.
	 * 
	 * @param frame the GUI frame.
	 */
	public SettingsPanel(final GUIFrame frame) {
		super(frame.getFrame(),Dialog.ModalityType.DOCUMENT_MODAL);
		final Settings settings = frame.getSystem().getSettings();
		setTitle("System Settings");
		
		Container contentPane = getContentPane();
		
		contentPane.setLayout(new BorderLayout());

		Container allOptions = new Container();
		allOptions.setLayout(new BoxLayout(allOptions, BoxLayout.PAGE_AXIS));

		JPanel inference = new JPanel();
		inference.setLayout(new GridLayout(3,2));
		inference.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(10, 10, 10, 10),
				BorderFactory.createTitledBorder("Inference")));
		
		inference.add(new JLabel(" Number of samples to draw per query:     "));
		final JFormattedTextField sampleNumber = new JFormattedTextField();
		sampleNumber.setText(""+Settings.nbSamples);
		inference.add(sampleNumber);		
		
		inference.add(new JLabel(" Maximum sampling time:     "));
		final JFormattedTextField sampleTime = new JFormattedTextField();
		sampleTime.setText(""+Settings.maxSamplingTime);
		inference.add(sampleTime);	
		
		inference.add(new JLabel(" Number of discretisation buckets:     "));
		final JFormattedTextField discrete = new JFormattedTextField();
		discrete.setText(""+Settings.discretisationBuckets);
		inference.add(discrete);	
		
		allOptions.add(inference);
		
		JPanel gui = new JPanel();
		gui.setLayout(new GridLayout(3,2));
		gui.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(10, 10, 10, 10),
				BorderFactory.createTitledBorder("Graphical Interface")));
		
		gui.add(new JLabel(" Variable label for user input:     "));
		final JFormattedTextField userVar = new JFormattedTextField();
		userVar.setText(""+ settings.userInput);
		gui.add(userVar);		
		
		gui.add(new JLabel(" Variable label for system output:     "));
		final JFormattedTextField systemVar = new JFormattedTextField();
		systemVar.setText(""+settings.systemOutput);
		gui.add(systemVar);	
		
		gui.add(new JLabel(" Other variables to monitor in chat window:     "));
		final JFormattedTextField toMonitor = new JFormattedTextField();
		toMonitor.setText(""+settings.varsToMonitor.toString().replace("[", "").replace("]", ""));
		gui.add(toMonitor);	
				
		allOptions.add(gui);
		
		JPanel planningBox = new JPanel();
		planningBox.setLayout(new GridLayout(2,2));
		planningBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(10, 10, 10, 10),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(" Planning "), 
						BorderFactory.createEmptyBorder(10, 0, 10, 0))));
			 		
		planningBox.add(new JLabel(" Planning horizon:     "));
		final JFormattedTextField horizon = new JFormattedTextField();
		horizon.setText(""+ settings.horizon);
		planningBox.add(horizon);		
		
		planningBox.add(new JLabel(" Discount factor:     "));
		final JFormattedTextField discount = new JFormattedTextField();
		discount.setText(""+settings.discountFactor);
		planningBox.add(discount);
				
		allOptions.add(planningBox);
		
		
		contentPane.add(allOptions, BorderLayout.NORTH);
		contentPane.add(new JLabel(" "), BorderLayout.CENTER);
		
		Container okcancelBox1 = new Container();
		okcancelBox1.setLayout(new BorderLayout());
		okcancelBox1.add(new JLabel("  "), BorderLayout.NORTH);
		Container okcancelBox = new Container();
		okcancelBox.setLayout(new BorderLayout());
		JButton cancelButton = new JButton("  Cancel  ");
		cancelButton.addActionListener(new ActionListener() {@Override
		public void actionPerformed(ActionEvent e) { setVisible(false); } });
		okcancelBox.add(cancelButton, BorderLayout.WEST);
		JButton okButton = new JButton("     OK     ");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { 
						try {
						Settings.nbSamples = Integer.parseInt(sampleNumber.getText());
						Settings.maxSamplingTime = Integer.parseInt(sampleTime.getText());
						Settings.discretisationBuckets = Integer.parseInt(discrete.getText());
						settings.horizon = Integer.parseInt(horizon.getText());
						settings.discountFactor = Double.parseDouble(discount.getText());
						}
						catch (Exception f) {
							log.warning("invalid number format in settings" );
						}
						settings.userInput = userVar.getText();
						settings.systemOutput = systemVar.getText();
						settings.varsToMonitor.clear();
						
						Properties otherStuff = new Properties();
						otherStuff.setProperty("monitor", toMonitor.getText().trim());
						
						settings.fillSettings(otherStuff);
						
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
		setMinimumSize(new Dimension(650,480));
		setPreferredSize(new Dimension(650,480));
		pack();
		setVisible(true);
	}
	
}
