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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.Value;
import opendial.utils.AudioUtils;

/**
 * Representation of a input audio stream used to record speech.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 */
public class SpeechStream extends InputStream implements Value {

	// logger
	public static Logger log = new Logger("SpeechStream", Logger.Level.NORMAL);

	/** The audio line that is recorded */
	TargetDataLine audioLine;
	
	/** The current position in the stream */
	int currentPos = 0;
	
	/** The recorded data */
	byte[] data;
	
	/** Whether the stream has been closed or not */
	boolean isClosed = false;

	/** Duration of padding silence before and after the speech (in milliseconds) */
	int silenceDuration = 0;
	
	/**
	 * Creates a new speech stream on a particular input audio mixer
	 * 
	 * @param inputMixer the audio mixer to use
	 * @throws DialException if the stream could not be captured from the mixer
	 */
	public SpeechStream(Mixer.Info inputMixer) throws DialException {
		audioLine = AudioUtils.selectAudioLine(inputMixer);
		log.debug("start recording on " + inputMixer.getName() + "...\t");
		(new Thread(new StreamRecorder())).start();
		data = new byte[0];
	}
	
	/**
	 * Creates a speech stream copied from another one
	 * 
	 * @param stream the speech stream to copy

	 */
	private SpeechStream(SpeechStream stream) {
		this.audioLine = stream.audioLine;
		this.currentPos = stream.currentPos;
		this.data = stream.data;
		this.isClosed = stream.isClosed;
	}
	
	/**
	 * Sets a padding silence before and after the speech stream.
	 * 
	 * @param duration duration of the silence (in milliseconds)
	 */
	public void setSilence(int silenceDuration) {
		this.silenceDuration = silenceDuration;
	}


	/**
	 * Reads one byte of the stream
	 * 
	 * @return the read byte
	 */
	@Override
	public int read() {
		if (currentPos < data.length) {
			return data[currentPos++];
		}
		else {
			if (!isClosed) {
				try {Thread.sleep(100); }
				catch (InterruptedException e) { }
				return read();
			}
		return -1;
		}
	}
	
	/**
	 * Reads a buffer from the stream.
	 * 
	 * @param buffer the buffer in which to write
	 * @param offset the offset in buffer from which the data should be written
	 * @param length the maximum number of bytes to read 
	 * @return the number of bytes written into the buffer
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) {
		if (currentPos >= data.length) {
			if (!isClosed) {
				try {Thread.sleep(100); }
				catch (InterruptedException e) { }
				return read(buffer, offset, length);
			}
			return -1;
		}
		int i = 0;
		for (i = 0 ; i < length & (currentPos+i) < data.length ; i++) {
			buffer[offset+i] = data[currentPos+i];
		}
		currentPos += i;
		return i;
	}
 
	/**
	 * Closes the stream, and notifies all waiting threads.
	 * @throws IOException if the stream could not be closed
	 */
	@Override
	public synchronized void close() throws IOException {
		isClosed = true;
		notifyAll();
	}

	/**
	 * Generates an audio file from the stream.  The file must be a WAV file.
	 * 
	 * @param outputFile the file in which to write the audio data
	 * @throws DialException if the audio could not be written onto the file
	 */
	public void generateFile(File outputFile) throws DialException {
		try {
			AudioInputStream audioStream = new AudioInputStream(new ByteArrayInputStream(data), audioLine.getFormat(),
					(data.length / audioLine.getFormat().getFrameSize()));
			if (outputFile.getName().endsWith("wav")) {
				int nb = AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, new FileOutputStream(outputFile));
				log.debug("WAV file written to " + outputFile.getCanonicalPath() + " ("+(nb/1000) + " kB)");
			}
			else {
				throw new DialException("Unsupported encoding " + outputFile);
			}
		}
		catch (Exception e) {
			throw new DialException("could not generate file: " + e);
		}
	}

	
	/**
	 * Returns true if the stream is closed, and false otherwise
	 * 
	 * @return true if the stream is closed, else false
	 */
	public boolean isClosed() {
		return isClosed;
	}


	/**
	 * Returns the byte array for the stream
	 * 
	 * @param includeSilence whether to include the padding silence
	 * @return the byte array
	 */
	public byte[] toByteArray(boolean includeSilence) {
		if (includeSilence || data.length == 0) {
		return data;
		}
		else {
			int offset = silenceDuration*((int)getFormat().getFrameRate())/1000;
			byte[] speechData = new byte[data.length-2*offset];
			System.arraycopy(data, offset, speechData, 0, speechData.length);
			return speechData;
		}
	}
	

	/**
	 * Returns the audio format that encodes the stream
	 * 
	 * @return the audio format
	 */
	public AudioFormat getFormat() {
		return audioLine.getFormat();
	}

	
	/**
	 * Recorder for the stream, based on the captured audio data.
	 */
	final class StreamRecorder implements Runnable {
		
		@Override
		public void run() {

			try {
				audioLine.open();
				audioLine.start();
				audioLine.flush();
				int frameRate = (int)getFormat().getFrameRate();
				// we limit the stream buffer to a maximum of 20 seconds
				ByteArrayOutputStream stream = new ByteArrayOutputStream(320000);
				byte[] buffer = new byte[audioLine.getBufferSize()/20];
				while (!isClosed) {
					// Read the next chunk of data from the TargetDataLine.
					int numBytesRead =  audioLine.read(buffer, 0, buffer.length);
					// Save this chunk of data.
					if (numBytesRead > 0) {
						stream.write(buffer, 0, numBytesRead);
						byte[] content = stream.toByteArray();
						int offset = silenceDuration*frameRate/1000;
						data = new byte[content.length + 2*offset];
						System.arraycopy(content, 0, data, offset, content.length);
					}
				}
				audioLine.stop();
				audioLine.close();
				stream.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Returns the hashcode difference.
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}


	/**
	 * Returns a copy of the stream
	 */
	@Override
	public SpeechStream copy() {
		return new SpeechStream(this);
	}


	/**
	 * Returns false
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}


	/**
	 * Returns a none value.
	 */
	@Override
	public Value concatenate(Value value) {
		return copy();
	}


}

