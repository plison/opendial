// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            

package oblig2.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import oblig2.ConfigParameters;
import oblig2.DialogueSystem;
import oblig2.NBest;
import oblig2.actions.DialogueAction;
import oblig2.state.DialogueStateListener;
import oblig2.util.AudioRecorder;
import oblig2.util.Logger;


/**
 * Graphical User Interface for the dialogue system.  The interface consists
 * of a chat window with the interaction history, a push&speak button
 * to record the user utterances, and a simulation panel.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class DialogueSystemGUI extends JFrame implements DialogueStateListener {

	// logging
	public static Logger log = new Logger("DialogueSystemGUI", Logger.Level.NORMAL);

	// parameters
	ConfigParameters parameters;
	
	// the dialogue system
	DialogueSystem owner;
	
	// history window
	JTextArea history;
	
	// the simulation panel
	SimulationPanel simulation;

	/**
	 * Creates a new GUI with the given system owner
	 * 
	 * @param owner the dialogue system
	 * @throws IOException 
	 */
	public DialogueSystemGUI (DialogueSystem owner) throws Exception {
		this.parameters = owner.getParameters();
		this.owner = owner;
		owner.getDialogueState().addListener(this);
		simulation = new SimulationPanel(owner.getWorldState());
		createWidgets();
		
		if (owner.getParameters().doTesting) {
			testRecorder();
		}
	}

	/**
	 * Creates the widgets
	 * @throws IOException 
	 * @throws LineUnavailableException 
	 */
	private void createWidgets() throws IOException, LineUnavailableException {
		setTitle("Dialogue system GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 630);
		setLayout(new BorderLayout());
		history = new JTextArea("", 30, 80);
		history.setEditable(false);
		TitledBorder  border = BorderFactory.createTitledBorder("Dialogue history");
		history.setBorder(border);
		add(history, BorderLayout.CENTER);
		add(simulation, BorderLayout.EAST);

		Container container = new Container();
		container.setLayout(new BorderLayout());
		JButton  button = new JButton("Press & hold to record speech");
		button.setPreferredSize(new Dimension(200, 50));
		RecorderListener rl = new RecorderListener(owner);
		button.addMouseListener(rl);
		container.add(new JLabel("  "), BorderLayout.NORTH);
		container.add(new JLabel("   Control:      "), BorderLayout.WEST);
		container.add(button, BorderLayout.CENTER);
		
		Container container2 = new Container();
		container2.setLayout(new BorderLayout());
		container2.add(new JLabel("        Microphone sound level:   "), BorderLayout.WEST);
	 	SoundLevelMeter slm = new SoundLevelMeter();
	 	rl.getRecorder().attachLevelMeter(slm);
	 	slm.setPreferredSize(new Dimension(150, 20));
	 	slm.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		container2.add(new JLabel("   \n "), BorderLayout.NORTH); 
		container2.add(slm, BorderLayout.CENTER); 
		container2.add(new JLabel("   \n "), BorderLayout.SOUTH); 
		container2.add(new JLabel("         "), BorderLayout.EAST); 
		container.add(container2, BorderLayout.EAST);  
		container.add(new JLabel("  "), BorderLayout.SOUTH);
		add(container, BorderLayout.SOUTH);
		
		setVisible(true);
	}
	
	
	private void testRecorder() throws Exception {
	    log.debug("testing functions for recording and playing sounds...");
	    AudioRecorder recorder = new AudioRecorder(owner.getParameters());
		recorder.startRecording();
		try {
			Thread.sleep(800);
		} catch (InterruptedException e) {	}
		recorder.stopRecording();	
		
		InputStream stream = recorder.getInputStream();
		int length = 0 ;
		byte[] buffer = new byte[4096];  
		for (@SuppressWarnings("unused")
		int n; (n = stream.read(buffer)) != -1; )  { 
			length += buffer.length;
		}
		if (length < 5000) {
			throw new Exception("sound was not properly recorded");
		}
	    log.debug("testing successful");
	}


	
	/**
	 * Adds a new user utterance to the chat history
	 * 
	 * @param u_U the nbest list corresponding to the utterance
	 */
	@Override
	public void processUserInput(NBest u_u) {
		history.append(" "  + parameters.username + ":");
		for (NBest.Hypothesis hyp : u_u.getHypotheses()) {

			history.append("\t" + hyp.getString());
			if (!hyp.getSem().equals("")) {
				history.append("\t[" + hyp.getSem() + "]");
			}
			if (hyp.getConf() < 1.0) {
				history.append("\t(" +Math.round(hyp.getConf()*1000)/1000.0+")");
			}
			history.append("\n");
		}
		history.repaint();
	}

	
	/**
	 * Adds a new system utterance to the chat history
	 * 
	 * @param action the dialogue action containing the utterance
	 */
	@Override
	public void processSystemOutput(DialogueAction action) {
		history.append(" "  + parameters.machinename +  ":");
		history.append("\t" + action.getUtterance());
		history.append("\n");
		history.repaint();
	}

	
	/**
	 * Does nothing (simply there to implement the dialogue listener interface)
	 */
	@Override
	public void newSpeechSignal(InputStream istream) { }
	
}
