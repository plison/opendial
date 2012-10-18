/*
 * Copyright (c) 1999 - 2003 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package oblig2.util;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFileFormat;

import oblig2.ConfigParameters;
import oblig2.gui.SoundLevelMeter;


/**
 * Class for recording 8000 Hz, 16 bit audio from the microphone.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AudioRecorder extends Thread {

	public static Logger log = new Logger("AudioRecorder", Logger.Level.NORMAL);

	// ugly hard-coding of the audio format (sampling rate of 8000 Hz, 16 bits, 1 channel)
	public static AudioFormat	AUDIO_FORMAT = new AudioFormat(8000.0F, 16, 1, true, false);
	private TargetDataLine		audioLine;
	private AudioInputStream	audioStream;

	private ByteArrayOutputStream	outputStream;

	SoundLevelMeter levelMeter;


	/**
	 * Creates a new recorder, and sets up the audio line. If the audioMixer setting
	 * in the parameter specifies an audio mixer, that one is used.  Else, the default
	 * line is selected
	 * 
	 * @param parameters configuration parameters
	 * @throws LineUnavailableException if the line is unavailable
	 */
	public AudioRecorder(ConfigParameters parameters) throws LineUnavailableException {

		Info[] mixers = AudioSystem.getMixerInfo();

		// printing out the list of possible mixers (in debug mode)
		for (int i = 0 ; i < mixers.length ; i++) {
				log.debug("mixer " + mixers[i].getName());
		}
		
		/* Now, we are trying to get a TargetDataLine. The
		   TargetDataLine is used later to read audio data from it. */
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);

		// default settings
		if (parameters.audioMixer == null || parameters.audioMixer.equals("")) {
			audioLine = (TargetDataLine) AudioSystem.getLine(info);
		}
		
		// use of a specific mixer
		else {
			for (int i = 0 ; i < mixers.length ; i++) {
				if (mixers[i].getName().contains(parameters.audioMixer)) {
					Mixer mixer = AudioSystem.getMixer(mixers[i]);
					audioLine = (TargetDataLine) mixer.getLine(info);
					log.debug("selecting mixer " + mixers[i].getName());
				}
			}
			if (audioLine ==null) {
				log.debug("mixer " + parameters.audioMixer + " was not found, using default line");
				audioLine = (TargetDataLine) AudioSystem.getLine(info);
			}
		}

	}

	/**
	 * Attaches a level meter to the recorder
	 * 
	 * @param meter the meter to attach
	 */
	public void attachLevelMeter(SoundLevelMeter meter) {
		this.levelMeter = meter;
	}


	/**
	 * Starts the recording on a suitable sound line (e.g. microphone), and 
	 * writes the result in the sound file provided as argument.
	 * The sound file can be either .AU or .WAV.
	 * 
	 * @param outputFile file in which to write the sound data
	 * @throws LineUnavailableException 
	 * @throws IOException 
	 * @throws Exception if the sound cannot be recorded
	 */
	public void startRecording() throws LineUnavailableException {
		log.debug("start recording...\t");

		audioLine.open(AUDIO_FORMAT);

		audioStream = new AudioInputStream(audioLine);

		/* Starting the TargetDataLine. It tells the line that
		   we now want to read data from it. If this method
		   isn't called, we won't
		   be able to read data from the line at all.
		 */
		audioLine.start();

		outputStream = new ByteArrayOutputStream();				


		/* Starting the thread. This call results in the
		   method 'run()' (see below) being called. There, the
		   data is actually read from the line.
		 */
		Thread t = new Thread() { public void run() {
			try {
				AudioSystem.write(audioStream, AudioFileFormat.Type.AU, outputStream);
			}
			catch (IOException e) {
				log.debug("IOException while recording file: " + e.toString());
			}  
		}
		};
		t.start();


		if (levelMeter != null) {
			levelMeter.monitorVolume(outputStream);
		} 
	}


	/**
	 * Returns the input stream associated with the audio capture
	 * 
	 * @return the input stream
	 */
	public InputStream getInputStream() { 
		ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
		return stream;
	}

	/** Stops the recording.

	    Note that stopping the thread explicitly is not necessary. Once
	    no more data can be read from the TargetDataLine, no more data
	    be read from our AudioInputStream. And if there is no more
	    data from the AudioInputStream, the method 'AudioSystem.write()'
	    (called in 'run()' returns. Returning from 'AudioSystem.write()'
	    is followed by returning from 'run()', and thus, the thread
	    is terminated automatically.

	    It's not a good idea to call this method just 'stop()'
	    because stop() is a (deprecated) method of the class 'Thread'.
	    And we don't want  to override this method.
	 */
	public void stopRecording() {
		log.debug("stopped...\t");
		audioLine.flush();
		audioLine.stop();
		audioLine.close();
		if (levelMeter != null) {
			levelMeter.stopMonitoring();
		}
	}



}


