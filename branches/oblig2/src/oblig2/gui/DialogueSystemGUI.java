// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            

package oblig2.gui;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

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
		this.owner = owner;
		owner.getDialogueState().addListener(this);
		simulation = new SimulationPanel(owner.getWorldState());
		createWidgets();
		testRecorder();
	}

	/**
	 * Creates the widgets
	 */
	private void createWidgets() {
		setTitle("Dialogue system GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 580);
		setLayout(new BorderLayout());
		history = new JTextArea("", 30, 80);
		history.setEditable(false);
		TitledBorder  border = BorderFactory.createTitledBorder("Dialogue history");
		history.setBorder(border);
		add(history, BorderLayout.CENTER);
		add(simulation, BorderLayout.EAST);

		JButton  button = new JButton("Press & hold to record speech");
		button.addMouseListener(new RecorderListener(owner));
		add(button, BorderLayout.SOUTH);
		setVisible(true);
	}
	
	
	private void testRecorder() throws Exception {
	    log.debug("testing functions for recording and playing sounds...");
	    AudioRecorder recorder = new AudioRecorder(owner.getParameters());
		recorder.startRecording();
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {	}
		recorder.stopRecording();	

		File f = new File(owner.getParameters().tempASRSoundFile);
		AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(f);
	    AudioFormat format = audioInputStream.getFormat();
	    long audioFileLength = f.length();
	    int frameSize = format.getFrameSize();
	    float frameRate = format.getFrameRate();
	    float duration = (audioFileLength / (frameSize * frameRate));
	    if (duration < 0.1) {
	    	throw new Exception ("recorded file was shorter than 0.1 seconds, there's a problem");
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
		history.append(" user:");
		for (NBest.Hypothesis hyp : u_u.getHypotheses()) {

			history.append("\t" + hyp.getString());
			if (hyp.getConf() < 1.0) {
				history.append("\t(" +hyp.getConf()+")");
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
		history.append(" robot:");
		history.append("\t" + action.getUtterance());
		history.append("\n");
		history.repaint();
	}

	
	/**
	 * Does nothing (simply there to implement the dialogue listener interface)
	 */
	@Override
	public void newSpeechSignal(File audioFile) { }
	
}
