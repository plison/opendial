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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicArrowButton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import opendial.arch.DialException;
import opendial.arch.DialogueSystem;
import opendial.arch.Logger;
import opendial.arch.StateListener;
import opendial.bn.Assignment;
import opendial.modules.DialogueRecorder;
import opendial.state.DialogueState;
import opendial.utils.XMLUtils;

/**
 * Main GUI frame for the openDial toolkit, encompassing various tabs and
 * menus to control the application
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2012-06-11 18:13:11 #$
 *
 */
@SuppressWarnings("serial")
public class WOZFrame extends JFrame implements ActionListener {

	// logger
	public static Logger log = new Logger("GUIFrame", Logger.Level.DEBUG);

	// frame instance
	private static WOZFrame guiFrameInstance;

	JComboBox combo;

	DialogueSystem system;
	
	DialogueRecorder recorder;
	


	/**
	 * Constructs the GUI frame, with its title, menus, tabs etc.
	 * 
	 */
	public WOZFrame(DialogueSystem system, DialogueRecorder recorder) {

		// TODO: add " - domain name " when a domain is loaded
		setTitle("Wizard-of-Oz window");

		setLocation(new Point(800, 400));

		addWindowListener(new WindowAdapter() 
		{
			public void windowClosing(WindowEvent e) 
			{ System.exit(0); }
		}
				); 

		guiFrameInstance = this;
		
		this.recorder = recorder;
		
		this.system = system;
		setLayout(new FlowLayout(FlowLayout.CENTER, 40, 20));
		createButtons();
		setPreferredSize(new Dimension(700,550));
		pack();
		setVisible(true);
	}


	private void createButtons() {
		Container arrowCont = new Container();
		GridLayout layout = new GridLayout(5,5);
		arrowCont.setLayout(layout);
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		BasicArrowButton forward = new BasicArrowButton(BasicArrowButton.NORTH);
		forward.setText("Do(Move(Forward))");
		forward.addActionListener(this);
		arrowCont.add(forward);
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		BasicArrowButton forward2 = new BasicArrowButton(BasicArrowButton.NORTH);
		forward2.setText("Do(Move(Forward,Short))");
		forward2.addActionListener(this);
		arrowCont.add(forward2);	
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));	
		BasicArrowButton left = new BasicArrowButton(BasicArrowButton.WEST);
		left.setText("Do(Move(Left))");
		left.addActionListener(this);
		arrowCont.add(left);	
		BasicArrowButton left2 = new BasicArrowButton(BasicArrowButton.WEST);
		left2.setText("Do(Move(Left,Short))");
		left2.addActionListener(this);
		arrowCont.add(left2);	
		JButton central = new JButton("");
		central.setEnabled(false);
		arrowCont.add(central);
		BasicArrowButton right = new BasicArrowButton(BasicArrowButton.EAST);
		right.setText("Do(Move(Right,Short))");
		right.addActionListener(this);
		arrowCont.add(right);	
		BasicArrowButton right2 = new BasicArrowButton(BasicArrowButton.EAST);
		right2.setText("Do(Move(Right))");
		right2.addActionListener(this);
		arrowCont.add(right2);
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		BasicArrowButton backward = new BasicArrowButton(BasicArrowButton.SOUTH);
		backward.setText("Do(Move(Backward,Short))");
		backward.addActionListener(this);
		arrowCont.add(backward);	
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		BasicArrowButton backward2 = new BasicArrowButton(BasicArrowButton.SOUTH);
		backward2.setText("Do(Move(Backward))");
		backward2.addActionListener(this);
		arrowCont.add(backward2);
		arrowCont.add(new JLabel(""));
		arrowCont.add(new JLabel(""));
		add(arrowCont);
		Container pickCont = new Container();
		pickCont.setLayout(new BorderLayout());
		JButton button1 = new JButton("   Do(PickUp(BlueObj))   ");
		button1.addActionListener(this);
		pickCont.add(button1, BorderLayout.EAST);
		JButton button2 = new JButton("   Do(PickUp(AtFeet))   ");
		button2.addActionListener(this);
		pickCont.add(button2, BorderLayout.CENTER);
		JButton button3 = new JButton("   Do(PickUp(RedObj))   ");
		button3.addActionListener(this);
		pickCont.add(button3, BorderLayout.WEST);
		add(pickCont);
		Container otherCont = new Container();
		otherCont.setLayout(new GridLayout(1,3));
		JButton button5 = new JButton("   Do(Release)   ");
		button5.addActionListener(this);
		otherCont.add(button5);
		JButton button6 = new JButton("   Do(Move(Turn))   ");
		button6.addActionListener(this);
		otherCont.add(button6);
		JButton button4 = new JButton("   Do(Stop)   ");
		button4.addActionListener(this);
		otherCont.add(button4);
		add(otherCont);

		String[] intentions = { "Move(Left)", "Move(Left,Short)", "Move(Right)",
				"Move(Right,Short)", "Move(Forward)", "Move(Forward,Short)",
				"Move(Backward)", "Move(Backward,Short)", "Move(Turn)", "PickUp(RedObj)", 
				"PickUp(BlueObj)", "PickUp(AtFeet)", "Release"};
		Container confirmCont = new Container();
		confirmCont.setLayout(new FlowLayout());
		confirmCont.add(new JLabel(" Confirm("));
		combo = new JComboBox(intentions);
		confirmCont.add(combo);
		confirmCont.add(new JLabel(") "));
		JButton okButton = new JButton(" OK ");
		okButton.addActionListener(this);
		confirmCont.add(okButton);
		confirmCont.add(new JLabel("            "));
		JButton button7 = new JButton("          AskRepeat          ");
		button7.addActionListener(this);
		confirmCont.add(button7);
		add(confirmCont);

		Container linguiCont = new Container();
		linguiCont.setLayout(new GridLayout(0,3));

		JButton button10 = new JButton("Say(DoNotSeeObject)");
		button10.addActionListener(this);
		linguiCont.add(button10);
		JButton button11 = new JButton("Say(AlreadyCarryObject)");
		button11.addActionListener(this);
		linguiCont.add(button11);
		JButton button12 = new JButton("Say(DoNotCarryObject)");
		button12.addActionListener(this);
		linguiCont.add(button12);
		add(linguiCont);
		Container linguiCont2 = new Container();
		linguiCont2.setLayout(new GridLayout(0,2));
		JButton button13 = new JButton("Describe([])");
		button13.addActionListener(this);
		linguiCont2.add(button13);
		JButton button14 = new JButton("Describe([RedObj])");
		button14.addActionListener(this);
		linguiCont2.add(button14);
		JButton button15 = new JButton("Describe([BlueObj])");
		button15.addActionListener(this);
		linguiCont2.add(button15);
		JButton button16 = new JButton("Describe([RedObj,BlueObj])");
		button16.addActionListener(this);
		linguiCont2.add(button16);
		add(linguiCont2);

		Container linguiCont3 = new Container();
		linguiCont3.setLayout(new GridLayout(0,2));

		JButton button8 = new JButton("Say(Confirm)");
		button8.addActionListener(this);
		linguiCont3.add(button8);
		JButton button9 = new JButton("Say(Disconfirm)");
		button9.addActionListener(this);
		linguiCont3.add(button9);
		add(linguiCont3);

		Container linguiCont4 = new Container();
		linguiCont4.setLayout(new GridLayout(0,2));

		JButton button17 = new JButton("Say(Greet)");
		button17.addActionListener(this);
		linguiCont4.add(button17);
		JButton button18 = new JButton("Say(Goodbye)");
		button18.addActionListener(this);
		linguiCont4.add(button18);
		add(linguiCont4);
	}


	public static WOZFrame getInstance() {
		return guiFrameInstance;
	}


	public DialogueSystem getSystem() {
		return system;
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() instanceof JButton) {
			JButton but = (JButton)arg0.getSource();

			String content = but.getText().trim();
			if (content.equals("OK")) {
				content = "Confirm("+combo.getSelectedItem().toString()+")";
			}
			recordAction(system.getState(), content);
		}

	}

	public void recordAction(DialogueState state, String actionValue) {

		try {
			log.debug("actionValue is " + actionValue);
			Assignment selection = new Assignment("a_m", actionValue);
			recorder.removeLastSample();
			recorder.recordTrainingData(state, selection);
			state.addContent(selection, "WOZ");
		}
		catch (DialException e) {
			log.warning("could not add the following action: " + actionValue);
		}
	}

	


}
