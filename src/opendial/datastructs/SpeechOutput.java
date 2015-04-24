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

package opendial.datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.LineEvent.Type;

import opendial.arch.Logger;
import opendial.utils.AudioUtils;

/**
 * Representation of a speech output to play by the system.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 *
 */
public class SpeechOutput {
	public static Logger log = new Logger("SpeechOutput", Logger.Level.DEBUG);

	// the audio stream to play
	AudioInputStream stream;
	
	// the stream player
	StreamPlayer player;

	/**
	 * Creation of a new speech output based on the audio input stream
	 * 
	 * @param stream the audio stream.
	 */
	public SpeechOutput(AudioInputStream stream)  {	
		this.stream = stream;
	}

	/**
	 * Creation of a new speech output based on the input stream (which needs
	 * to be read and converted into an audio stream).
	 * 
	 * @param stream the input stream.
	 */
	public SpeechOutput(InputStream stream) {
		this(readStream(stream));
	}

	/**
	 * Creation of a new speech output based on a raw array of bytes (which need
	 * to be converted into an audio stream).
	 * 
	 * @param byteArray the byte array
	 */
	public SpeechOutput(byte[] byteArray) {
		this(AudioUtils.getAudioStream(byteArray));
	}


	/**
	 * Plays the stream onto the given audio mixer.  Note that this method can
	 * only be called once per SpeechOutput, as the stream is closed once the
	 * audio has been played.
	 * 
	 * @param outputMixer the audio mixer to use
	 */
	public void play(Mixer.Info outputMixer) {
		try {
			player = new StreamPlayer(outputMixer);
			(new Thread(player)).start();		
		}
		catch (LineUnavailableException e) {
			log.warning("could not play speech output: " + e);
		}
	}
	
	/**
	 * Blocks until the audio has finished playing.
	 */
	public void waitUntilPlayed() {
		try { 
			if (player != null) {
				synchronized (player) {
					player.wait(); 
				} 
			}
		}
		catch (InterruptedException e) { e.printStackTrace(); }
	}

	/**
	 * Stops the audio play.
	 */
	public void stop() {
		if (player != null) {
			player.close();
		}
	}



	/**
	 * Reads the input stream and returns the corresponding array of bytes
	 * @param stream the initial stream
	 * @return the array of bytes from the stream
	 */
	private static byte[] readStream(InputStream stream) {
		ByteArrayOutputStream rawBuffer = new ByteArrayOutputStream();
		try {
			int nRead;
			byte[] data = new byte[1024 * 16];
			while ((nRead = stream.read(data, 0, data.length)) != -1) {
				rawBuffer.write(data, 0, nRead);
			}
			rawBuffer.flush();
			rawBuffer.close();
		}
		catch (IOException e) {
			log.warning("Error reading audio stream: " + e);
		}
		return rawBuffer.toByteArray();	
	}



	/**
	 * Audio player.
	 *
	 */
	final class StreamPlayer implements Runnable {

		// the audio clip
		Clip clip;

		/**
		 * Creates a new player for the given audio mixer.
		 * 
		 * @param outputMixer the audio mixer to use
		 * @throws LineUnavailableException if the audio line is unavailable
		 */
		public StreamPlayer(Mixer.Info outputMixer) throws LineUnavailableException {
			clip = (outputMixer!=null)? AudioSystem.getClip(outputMixer) : AudioSystem.getClip();
			clip.addLineListener(e -> {
				if (e.getType() == Type.STOP || e.getType() == Type.CLOSE) 
					{synchronized (this) { notifyAll();}}
				});
		}

		/**
		 * Closes the player
		 */
		public void close() {
			log.debug("close has been triggered");
			try {
				if (clip.isOpen()) {
					clip.close();
				}
				stream.close();	
			} 
			catch (Exception e) {
				log.warning("unable to close output, aborting.  Error: " + e.toString());
			} 
		}

		/**
		 * Plays the audio.
		 */
		@Override
		public synchronized void run() {
			try {
				log.debug("opening stream...");
				clip.open(stream);
				log.debug("starting...");
				clip.start();
				log.debug("started! now waiting...");
				wait();
				Thread.sleep(3000);
				log.debug("finished the wait");
				close();
				log.debug("closing");
			}  
			catch (Exception e) {
				log.warning("unable to play sound file, aborting.  Error: " + e.toString());
			} 
		}
	}

}



