/*
 *	AudioCommon.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
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

package opendial.utils;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Mixer.Info;

import opendial.arch.DialException;
import opendial.arch.Logger;

/**
 * Utility methods for processing audio data.
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class AudioUtils {

	public static Logger log = new Logger("AudioUtils", Logger.Level.DEBUG);

	/** Audio format for higher-quality speech recognition (frame rate: 16 kHz) */
	static AudioFormat IN_HIGH = new AudioFormat(16000.0F, 16, 1, true, false);

	/** Audio format for lower-quality speech recognition (frame rate: 8 kHz) */
	static AudioFormat IN_LOW = new AudioFormat(8000.0F, 16, 1, true, false);
	
	/** Audio format for the speech synthesis */
	static AudioFormat OUT = new AudioFormat(16000.0F, 16, 1, true, false);


	/**
	 * Selects an target data line for a particular audio mixer.
	 * 
	 * @param mixer the name of the audio mixer
	 * @return the selected line
	 * @throws DialException if no line could be selected
	 */
	public static TargetDataLine selectAudioLine(Mixer.Info mixer) throws DialException {

		for (AudioFormat format : Arrays.asList(IN_HIGH, IN_LOW)) {
			try {
			DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
			if (AudioSystem.getMixer(mixer).isLineSupported(lineInfo)) {
				return AudioSystem.getTargetDataLine(format, mixer);
			}
			}
			catch (LineUnavailableException e) {
				log.warning(" line for mixer " + mixer + " is not available");
				log.info("Available audio mixers: " + getMixers());
			}
		}
		throw new DialException("Cannot obtain audio line for mixer " + mixer);
	}



	/**
	 * Plays the input stream on the standard audio input.
	 * 
	 * @param stream the stream to play
	 */
	public static void playAudio(final InputStream stream, Mixer.Info outputMixer) {
		(new Thread(new AudioPlayer(stream, outputMixer))).start();
	}

	/**
	 * Returns the list of all audio mixers
	 * 
	 * @return the list of mixers
	 */
	public static List<String> getMixers() {
		Info[] mixers = AudioSystem.getMixerInfo();

		List<String> mixersStr = new LinkedList<String>();
		for (int i = 0 ; i < mixers.length ; i++) {
			mixersStr.add(mixers[i].getName());
		}
		return mixersStr;

	}

	/**
	 * Returns a list with all audio mixers whose input are compatible with the
	 * two audio format IN_HIGH or IN_LOW.  
	 * 
	 * @return the list of all input mixers
	 */
	public static List<Mixer.Info> getInputMixers() {

		List<Mixer.Info> mixers = new ArrayList<Mixer.Info>();
		Mixer.Info defaultMixer = null;

		Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (int i = 0 ; i < mixerInfos.length ; i++) {
			for (AudioFormat format: Arrays.asList(IN_HIGH, IN_LOW)) {
				if (AudioSystem.getMixer(mixerInfos[i]).isLineSupported(
						new DataLine.Info(TargetDataLine.class, format))) {

					mixers.add(mixerInfos[i]);

					try { if (AudioSystem.getTargetDataLine(format).getLineInfo().matches(
							AudioSystem.getTargetDataLine(format, mixerInfos[i]).getLineInfo())) {
						defaultMixer = mixerInfos[i];
					} }
					catch (Exception e) { e.printStackTrace(); }

					break;
				}
			}
		}
		if (defaultMixer != null) {
			mixers.remove(defaultMixer);
			mixers.add(0, defaultMixer);
		}
		return mixers;
	}
	
	
	/**
	 * Returns the list of all audio mixers whose output is compatible with the audio
	 * format OUT.
	 * 
	 * @return the list of all compatible output mixers
	 */
	public static List<Mixer.Info> getOutputMixers() {

		List<Mixer.Info> mixers = new ArrayList<Mixer.Info>();
		Mixer.Info defaultMixer = null;

		Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (int i = 0 ; i < mixerInfos.length ; i++) {
			if (AudioSystem.getMixer(mixerInfos[i]).isLineSupported(
						new DataLine.Info(SourceDataLine.class, OUT))) {

					mixers.add(mixerInfos[i]);
					try { if (AudioSystem.getSourceDataLine(OUT).getLineInfo().matches(
							AudioSystem.getSourceDataLine(OUT, mixerInfos[i]).getLineInfo())) {
						defaultMixer = mixerInfos[i];
					} }
					catch (Exception e) { e.printStackTrace(); }
				}
			
		}
		if (defaultMixer != null) {
			mixers.remove(defaultMixer);
			mixers.add(0, defaultMixer);
		}
		return mixers;
	}


	/**
	 * Thread used to play audio streams.
	 */
	final static class AudioPlayer implements Runnable {

		InputStream stream;
		Mixer.Info outputMixer;

		public AudioPlayer(InputStream stream, Mixer.Info outputMixer) {
			this.stream = stream;
			this.outputMixer = outputMixer;
		}

		@Override
		public void run() {
			try {
				AudioInputStream input = (stream instanceof AudioInputStream)? 
						(AudioInputStream) stream: 
					AudioSystem.getAudioInputStream(stream);
		        Clip clip = (outputMixer != null)? AudioSystem.getClip(outputMixer) : AudioSystem.getClip();
		        clip.open(input);
			    clip.start();
			}
			catch (Exception e) {
				log.severe("unable to play sound file, aborting.  Error: " + e.toString());
			} 
		}
	}


}
