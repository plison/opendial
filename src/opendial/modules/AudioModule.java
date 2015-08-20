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

package opendial.modules;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.bn.values.Value;
import opendial.datastructs.Assignment;
import opendial.datastructs.SpeechData;
import opendial.gui.SpeechInputPanel;
import opendial.utils.AudioUtils;

/**
 * Module used to take care of all audio processing functionalities in OpenDial. The
 * module is employed both to record audio data from the microphone and to play audio
 * data on the system speakers.
 * 
 * <p>
 * Two modes are available to record audio data:
 * <ol>
 * <li>a manual mode when the user explicitly click on the
 * "Press and hold to record speech" to indicate the start and end points of speech
 * data.
 * <li>an automatic mode relying on (energy-based) Voice Activity Recognition to
 * determine when speech is present in the audio stream.
 * </ol>
 * .
 * 
 * <p>
 * When speech is detected using one of the two above methods, the module creates a
 * SpeechData object containing the captured audio stream and updates the dialogue
 * state with a new value for the variable denoting the user speech (by default s_u).
 * This data is then presumably picked up by a speech recogniser for further
 * processing.
 * 
 * <p>
 * The module is also used for the reverse operation, namely playing audio data
 * (generated via e.g. speech synthesis) on the target audio line. When the module
 * detects a new value for the variable denoting the system speech (by default s_m),
 * it plays the corresponding audio on the target line.
 * 
 * <p>
 * The module can gracefully handle user interruptions (when the user starts speaking
 * when the system is still talking).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class AudioModule implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** the dialogue system */
	DialogueSystem system;

	/** The audio line for capturing audio */
	TargetDataLine audioLine;

	/** The recorded speech (null if the input audio is not currently speech) */
	SpeechData inputSpeech;

	/** The output speech currently playing */
	SpeechData outputSpeech;

	/** whether the module is paused or not */
	boolean isPaused = true;

	/** whether the speech is to be automatically detected or not */
	boolean voiceActivityDetection = false;

	/** current audio level */
	double currentVolume = 0.0;

	/** background audio level */
	double backgroundVolume = 0.0;

	/** speech panel (used to e.g. show the current volume) */
	SpeechInputPanel speechPanel;

	/**
	 * Threshold for the difference between the current and background audio volume
	 * level above which the audio is considered as speech
	 */
	public static final double VOLUME_THRESHOLD = 250;

	/**
	 * Minimum duration for a sound to be considered as possible speech (in
	 * milliseconds)
	 */
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
		(new Thread(new SpeechRecorder())).start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			audioLine.stop();
			audioLine.close();
		}));
	}

	/**
	 * Attaches the speech panel to the module
	 * 
	 * @param speechPanel the speech input panel (containing the volume panel)
	 */
	public void attachPanel(SpeechInputPanel speechPanel) {
		this.speechPanel = speechPanel;
	}

	/**
	 * Activates or deactivates voice activity detection (VAD). If VAD is
	 * deactivated, the GUI button "press and hold to record speech" is used to mark
	 * the start and end points of speech data.
	 * 
	 * @param activateVAD true if VAD should be activated, false otherwise
	 */
	public void activateVAD(boolean activateVAD) {
		this.voiceActivityDetection = activateVAD;
	}

	/**
	 * Starts the recording of a new speech segment, and adds its content to the
	 * dialogue state. If voice activity recognition is used, the new speech segment
	 * is only inserted after waiting a minimum duration, in order to avoid inserting
	 * many spurious short noises into the dialogue state. Otherwise, the speech is
	 * inserted immediately.
	 * 
	 */
	public void startRecording() {
		if (!isPaused) {

			// creates a new SpeechData object
			inputSpeech = new SpeechData(audioLine.getFormat());

			// state update procedure
			Runnable stateUpdate = () -> {
				if (inputSpeech != null && !inputSpeech.isFinal()) {
					system.addUserInput(inputSpeech);
				}
			};

			// performs the update
			if (voiceActivityDetection) {
				new Thread(() -> {
					try {
						Thread.sleep(MIN_DURATION);
					}
					catch (Exception e) {
					}
					stateUpdate.run();
				}).start();
			}
			else {
				stateUpdate.run();
			}

		}
		else {
			log.info("Audio recorder is currently paused");
		}
	}

	/**
	 * Stops the recording of the current speech segment.
	 */
	public void stopRecording() {
		if (inputSpeech != null) {
			inputSpeech.setAsFinal();

			if (SAVE_SPEECH.length() > 0 && inputSpeech.length() > MIN_DURATION) {
				AudioUtils.generateFile(inputSpeech.toByteArray(),
						new File(SAVE_SPEECH));
			}
			inputSpeech = null;
			system.addContent(system.getSettings().floor, "free");
		}
	}

	/**
	 * Checks whether the dialogue state contains a updated value for the system
	 * speech (by default denoted as s_m). If yes, plays the audio on the target
	 * line.
	 */
	@Override
	public synchronized void trigger(DialogueState state,
			Collection<String> updatedVars) {
		String systemSpeech = system.getSettings().systemSpeech;
		if (updatedVars.contains(systemSpeech)
				&& state.hasChanceNode(systemSpeech)) {
			Value v = state.queryProb(systemSpeech).getBest();
			if (v instanceof SpeechData) {
				system.addContent(
						new Assignment(system.getSettings().floor, "system"));
				playSpeech((SpeechData) v);
			}
		}
	}

	/**
	 * Plays the speech data onto the default target line.
	 * 
	 * @param sound the sound to play
	 */
	public void playSpeech(SpeechData sound) {

		sound.rewind();

		// normal case: no previous speech is playing
		if (outputSpeech == null) {
			outputSpeech = sound;
			(new Thread(new SpeechPlayer())).start();
		}

		// if the system is already playing a sound, concatenate to the
		// existing one
		else {
			outputSpeech = outputSpeech.concatenate(sound);
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
	 * Returns the current volume for the recording.
	 * 
	 * @return the current volume
	 */
	public double getVolume() {
		return currentVolume;
	}

	/**
	 * Recorder for the stream, based on the captured audio data.
	 */
	final class SpeechRecorder implements Runnable {

		/**
		 * Captures the audio data in the audio line and updates the data array.
		 */
		@Override
		public void run() {

			try {
				audioLine.open();
				audioLine.start();
				audioLine.flush();
				AudioFormat format = audioLine.getFormat();
				byte[] buffer = new byte[4000];
				while (audioLine.isOpen()) {
					boolean systemTurnBeforeRead = outputSpeech != null;

					int numBytesRead = audioLine.read(buffer, 0, buffer.length);

					// in case the user has interrupted the system, drain the
					// line
					if (systemTurnBeforeRead && outputSpeech == null) {
						audioLine.drain();
						continue;
					}

					// if any of these apply, we do not need to process the
					// buffer
					else if (outputSpeech != null || numBytesRead == 0
							|| (!voiceActivityDetection && inputSpeech == null)) {
						if (speechPanel != null) {
							speechPanel.clearVolume();
						}
						continue;
					}

					// update the volume estimates
					double rms = AudioUtils.getRMS(buffer, format);
					currentVolume = (currentVolume + rms) / 2;
					if (rms < backgroundVolume) {
						backgroundVolume = rms;
					}
					else {
						backgroundVolume += (rms - backgroundVolume) * 0.003;
					}
					if (speechPanel != null) {
						speechPanel.updateVolume((int) currentVolume);
					}
					double difference = currentVolume - backgroundVolume;

					// try to detect voice (if VAD is activated)
					if (voiceActivityDetection && inputSpeech == null
							&& difference > VOLUME_THRESHOLD) {
						startRecording();
					}

					// write to the current speech data if the audio is speech
					if (inputSpeech != null && !inputSpeech.isFinal()) {
						inputSpeech.write(buffer);

						// stop the recording if the volume is back to normal
						if (voiceActivityDetection
								&& difference < VOLUME_THRESHOLD / 10) {
							stopRecording();
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Audio player. The player takes a queue of SpeechData objects to play (in
	 * sequential order) and plays it on the line corresponding to the selected
	 * output mixer.
	 *
	 */
	final class SpeechPlayer implements Runnable {

		/**
		 * Plays the audio.
		 */
		@Override
		public void run() {
			try {
				if (outputSpeech == null) {
					return;
				}
				if (speechPanel != null) {
					speechPanel.setSystemTalking(true);
				}
				Mixer.Info outputMixer = system.getSettings().outputMixer;
				AudioFormat format = outputSpeech.getFormat();
				SourceDataLine line =
						AudioSystem.getSourceDataLine(format, outputMixer);
				line.open(format);
				line.start();
				int nBytesRead = 0;
				byte[] abData = new byte[512 * 16];
				while (!outputSpeech.isFinal() && outputSpeech.length() < 500) {
					Thread.sleep(100);
				}
				while (nBytesRead != -1) {
					nBytesRead = outputSpeech.read(abData, 0, abData.length);

					// stop playing if user starts talking or system is paused
					if (inputSpeech != null || isPaused) {
						break;
					}
					if (nBytesRead >= 0) {
						line.write(abData, 0, nBytesRead);
					}
				}
				if (speechPanel != null) {
					speechPanel.setSystemTalking(false);
				}
				outputSpeech = null;
				line.drain();
				if (line.isOpen()) {
					line.close();
				}
			}
			catch (LineUnavailableException | InterruptedException e) {
				log.warning("Audio line is unavailable: " + e);
			}
			if (inputSpeech == null) {
				system.addContent(system.getSettings().floor, "free");
			}
		}

	}

}
