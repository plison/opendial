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
import opendial.utils.AudioUtils;

/**
 * Representation of a input audio stream used to record speech.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date:: 2014-04-23 16:18:00 #$
 */
public class SpeechStream extends InputStream {

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
	 * @throws IOException 
	 */
	@Override
	public synchronized void close() throws IOException {
		log.debug("stopped...\t");
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
	 * @return the byte array
	 */
	public byte[] toByteArray() {
		return data;
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
				// we limit the stream buffer to a maximum of 20 seconds
				ByteArrayOutputStream stream = new ByteArrayOutputStream(320000);
				byte[] buffer = new byte[audioLine.getBufferSize()/20];
				while (!isClosed) {
					// Read the next chunk of data from the TargetDataLine.
					int numBytesRead =  audioLine.read(buffer, 0, buffer.length);
					// Save this chunk of data.
					if (numBytesRead > 0) {
						stream.write(buffer, 0, numBytesRead);
						data = stream.toByteArray();
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


}

