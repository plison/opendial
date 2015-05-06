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

package opendial.modules.core;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import opendial.DialogueSystem;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.SpeechData;
import opendial.modules.Module;
import opendial.state.DialogueState;
import opendial.utils.AudioUtils;

/**
 * Module used to take care of all audio processing functionalities in OpenDial. The module
 * is employed both to record audio data from the microphone and to play audio data on the 
 * system speakers.
 * 
 * <p>Two modes are available to record audio data: <ol>
 * <li> a manual mode when the user explicitly click on the "Press and hold to record speech"
 * to indicate the start and end points of speech data.
 * <li> an automatic mode relying on (energy-based) Voice Activity Recognition to determine
 * when speech is present in the audio stream.
 * </ol>.
 * 
 * <p>When speech is detected using one of the two above methods, the module creates a 
 * SpeechData object containing the captured audio stream and updates the dialogue state 
 * with a new value for the variable denoting the user speech (by default s_u). This data 
 * is then presumably picked up by a speech recogniser for further processing.
 * 
 * <p>The module is also used for the reverse operation, namely playing audio data (generated
 * via e.g. speech synthesis) on the target audio line. When the module detects a new
 * value for the variable denoting the system speech (by default s_m), it plays the corresponding
 * audio on the target line. 
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class AudioModule implements Module {


	// logger
	public static Logger log = new Logger("AudioModule", Logger.Level.DEBUG);

	/** the dialogue system */
	DialogueSystem system;

	/** The audio line for capturing audio */
	TargetDataLine audioLine;

	/** The recorded speech (null if the input audio is not currently speech) */
	SpeechData inputSpeech;

	/** The queue of output speech to play */
	Queue<SpeechData> outputSpeech;

	/** whether the module is paused or not */
	boolean isPaused = true;

	/** whether the speech is to be automatically detected or not */
	boolean voiceActivityDetection = false;

	/** current audio level */ 
	double currentVolume = 5.0;

	/** background audio level */
	double backgroundVolume = 5.0;

	/** Threshold for the difference between the current and background audio
	 * level above which the audio is considered as speech
	 */
	public static final double VOLUME_THRESHOLD = 200;

	public static final int MIN_DURATION = 300;

	/** file used to save the speech input (leave empty to avoid recording) */
	public static String SAVE_SPEECH = "";


	/**
	 * Creates a new audio recorder connected to the dialogue system.
	 * 
	 * @param system the dialogue system
	 */
	public AudioModule(DialogueSystem system) {
		this.system = system;
		audioLine = AudioUtils.selectAudioLine(system.getSettings().inputMixer);
		outputSpeech = new LinkedList<SpeechData>();
	}


	/**
	 * Starts the audio recording
	 */
	@Override
	public void start() {
		isPaused = false;
		if (audioLine != null) {
			audioLine.close();
		}
		audioLine = AudioUtils.selectAudioLine(system.getSettings().inputMixer);	
		(new Thread(new AudioRecorder())).start();
	}


	/**
	 * Activates or deactivates voice activity detection (VAD).  If VAD is
	 * deactivated, the GUI button "press and hold to record speech" is used
	 * to mark the start and end points of speech data.
	 * 
	 * @param activateVAD true if VAD should be activated, false otherwise
	 */
	public void activateVAD(boolean activateVAD) {
		this.voiceActivityDetection = activateVAD;
	}


	/**
	 * Starts the recording of a new speech segment, and adds its content
	 * to the dialogue state. The new speech segment is only inserted after
	 * waiting a duration of MIN_DURATION, in order to avoid inserting many
	 * spurious short noises into the dialogue state.
	 */
	public void startRecording() {
		if (!isPaused) {
			outputSpeech.clear();
			inputSpeech = new SpeechData(audioLine.getFormat());
			new Thread(() -> {
				try {Thread.sleep(MIN_DURATION);} catch (Exception e) {} 
				if (inputSpeech != null && !inputSpeech.isFinal()) {
					system.addContent(new Assignment(system.getSettings().userSpeech, inputSpeech));					
				}
			}).start();
		}
		else {
			log.info("Audio recorder is currently paused");
		}
	}

	/**
	 * Stops the recording of the current speech segment.
	 */
	public void stopRecording() {
		inputSpeech.setAsFinal();
		if (SAVE_SPEECH.length() > 0 && inputSpeech.duration() > MIN_DURATION) {
			AudioUtils.generateFile(inputSpeech.toByteArray(), new File(SAVE_SPEECH));			
		}
		inputSpeech = null;
		system.removeContent(system.getSettings().userSpeech);
	}

	
	/**
	 * Checks whether the dialogue state contains a updated value for the system speech
	 * (by default denoted as s_m). If yes, plays the audio on the target line.
	 */
	@Override
	public synchronized void trigger(DialogueState state, Collection<String> updatedVars) {
		String systemSpeech = system.getSettings().systemSpeech;
		if (updatedVars.contains(systemSpeech) && state.hasChanceNode(systemSpeech)) {
			Value v = state.queryProb(systemSpeech).getBest();
			if (v instanceof SpeechData) {
				outputSpeech.add((SpeechData)v);
				if (outputSpeech.size() == 1) {
					(new Thread(new AudioPlayer())).start();
				}
			}
		}
	}


	/**
	 * Pauses the recorder
	 */
	@Override
	public void pause(boolean toPause) {
		isPaused = toPause;
	}

	/**
	 * Returns true if the recorder is currently running, and false otherwise
	 */
	@Override
	public boolean isRunning() {
		return !isPaused;
	}



	/**
	 * Returns the current level for the recording
	 * @return
	 */
	public double getVolume() {
		return currentVolume;
	}


	/**
	 * Recorder for the stream, based on the captured audio data.
	 */
	final class AudioRecorder implements Runnable {

		public AudioRecorder() {
			Runtime.getRuntime().addShutdownHook(
					new Thread(() -> { audioLine.stop(); audioLine.close(); }));
		}

		/**
		 * Captures the audio data in the audio line and updates the
		 * data array.
		 */
		@Override
		public void run() {

			try {
				audioLine.open();
				audioLine.start();
				audioLine.flush();
				byte[] buffer = new byte[audioLine.getBufferSize()/20];
				while (audioLine.isOpen()) {
					// Read the next chunk of data from the TargetDataLine.
					int numBytesRead = audioLine.read(buffer, 0, buffer.length);
					// Save this chunk of data.
					if (numBytesRead > 0) {
						processBuffer(buffer);
					}				
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	
	/**
	 * Processes the recorded buffer of audio data. The metho first updates
	 * the estimates for the current and background volume. Then, if VAD is activated,
	 * it determines whether the current audio contains speech. Finally, if the
	 * current audio contains speech, write the buffer data to the SpeechData object.
	 * 
	 * @param buffer the buffer to process
	 */
	private void processBuffer(byte[] buffer) {

		// step 1: update the volume estimates
		double newVolume = AudioUtils.getRMS(buffer, audioLine.getFormat());
		currentVolume = (currentVolume + newVolume) /2;	
		if (newVolume < backgroundVolume) {
			backgroundVolume = newVolume;
		}
		else {
			backgroundVolume += (newVolume - backgroundVolume) * 0.003;
		}

		// step 2: check if speech is detected
		if (voiceActivityDetection) {
			double volumeDiff = currentVolume - backgroundVolume;
			if (inputSpeech == null && outputSpeech.isEmpty() 
					&& volumeDiff > VOLUME_THRESHOLD) {
				startRecording();
			}
			else if (inputSpeech != null && volumeDiff < VOLUME_THRESHOLD / 5) {
				stopRecording();
			}
		}

		// step 3: write the buffer to the speech data
		if (inputSpeech != null) {
			inputSpeech.write(buffer);
		}
	}


	/**
	 * Audio player. The player takes a queue of SpeechData objects to play
	 * (in sequential order) and plays it on the line corresponding to the
	 * selected output mixer.
	 *
	 */
	final class AudioPlayer implements Runnable {

		/**
		 * Plays the audio.
		 */
		@Override
		public void run() {
			try {
				if (outputSpeech.isEmpty()) {
					return;
				}
				Mixer.Info outputMixer = system.getSettings().outputMixer;
				AudioFormat format = outputSpeech.peek().getFormat();
				SourceDataLine line = AudioSystem.getSourceDataLine(format, outputMixer);
				line.open(format);
				line.start();

				out: while (!outputSpeech.isEmpty()) {
					SpeechData curSpeech = outputSpeech.peek();
					int nBytesRead = 0;
					byte[] abData = new byte[256 * 16];
					while (nBytesRead != -1) {
						nBytesRead = curSpeech.read(abData, 0, abData.length);
						if (nBytesRead >= 0) {
							line.write(abData, 0, nBytesRead);
						}
						if (outputSpeech.isEmpty()) {
							break out;
						}
					}
					outputSpeech.poll();
				}

				outputSpeech.clear();
				system.removeContent(system.getSettings().systemSpeech);
				line.drain();
				if (line.isOpen()) {
					line.close();
				}
			}
			catch (LineUnavailableException e) {
				log.warning("Audio line is unavailable: " + e);
			}	
		}


	}




}
