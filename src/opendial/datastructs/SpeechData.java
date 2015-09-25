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

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.*;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import opendial.bn.values.Value;
import opendial.utils.AudioUtils;

/**
 * Representation of a stream of speech data (input or output). The stream can both
 * be read (using the usual methods), but can also be modified by appending new data
 * to the end of the stream.
 * 
 * <p>
 * The stream is allowed to change until it is marked as "final" (i.e. when the audio
 * capture has finished recording).
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class SpeechData extends InputStream implements Value {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** the position in the data stream */
	int currentPos = 0;

	/** the current array of speech data */
	byte[] data;

	/** whether the data is final or still expected to change */
	boolean isFinal = false;

	/** the audio format for the stream */
	AudioFormat format;

	// ===================================
	// CONSTRUCTION METHODS
	// ===================================

	/**
	 * Creates a new, empty stream of data with a given audio format
	 * 
	 * @param format the audio format to employ
	 */
	public SpeechData(AudioFormat format) {
		data = new byte[0];
		this.format = format;
	}

	/**
	 * Creates a stream of speech data based on a pre-existing byte array.
	 * 
	 * @param data the byte array
	 */
	public SpeechData(byte[] data) {
		AudioInputStream stream = AudioUtils.getAudioStream(data);
		format = stream.getFormat();
		this.data = data;
		isFinal = true;
	}

	/**
	 * Marks the speech data as final (it won't be changed anymore)
	 * 
	 */
	public synchronized void setAsFinal() {
		isFinal = true;
	}

	// ===================================
	// STREAM READING AND WRITING
	// ===================================

	/**
	 * Expands the current speech data by appending a new buffer of audio data
	 * 
	 * @param buffer the new audio data to insert
	 */
	public void write(byte[] buffer) {
		if (isFinal) {
			log.warning("attempting to write to a final SpeechData object");
			return;
		}
		byte[] newData = new byte[data.length + buffer.length];
		System.arraycopy(data, 0, newData, 0, data.length);
		System.arraycopy(buffer, 0, newData, data.length, buffer.length);
		data = newData;
	}

	/**
	 * Expands the current speech data by appending the data in the input stream.
	 * 
	 * @param stream the stream to add to the speech data
	 */
	public void write(InputStream stream) {
		if (isFinal) {
			log.warning("attempting to write to a final SpeechData object");
			return;
		}
		try {
			int nRead;
			byte[] buffer = new byte[1024 * 16];
			while ((nRead = stream.read(buffer, 0, buffer.length)) != -1) {
				byte[] newData = new byte[data.length + nRead];
				System.arraycopy(data, 0, newData, 0, data.length);
				System.arraycopy(buffer, 0, newData, data.length, nRead);
				data = newData;
			}
		}
		catch (IOException e) {
			log.warning("Cannot write the stream to the speech data");
		}
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
			if (!isFinal) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
				}
				return read();
			}
			return -1;
		}
	}

	/**
	 * Reads a buffer of data from the stream
	 * 
	 * @param buffer the buffer to fill
	 * @param offset the offset at which to start filling the buffer
	 * @param length the maximum number of bytes to read
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) {
		if (currentPos >= data.length) {
			if (isFinal) {
				return -1;
			}
			else {
				try {
					Thread.sleep(20);
				}
				catch (InterruptedException e) {
				}
			}
		}
		int i = 0;
		for (i = 0; i < length & (currentPos + i) < data.length; i++) {
			buffer[offset + i] = data[currentPos + i];
		}
		currentPos += i;
		return i;
	}

	/**
	 * Resets the current position in the stream to 0.
	 */
	public void rewind() {
		currentPos = 0;
	}

	// ===================================
	// GETTERS
	// ===================================

	/**
	 * Returns the duration of the audio data (in milliseconds)
	 * 
	 * @return the duration of the audio data
	 */
	@Override
	public int length() {
		return data.length / (format.getFrameSize() * 8);
	}

	/**
	 * Returns true if the speech data is final, false otherwise
	 * 
	 * @return true if the data is final, false otherwise
	 */
	public boolean isFinal() {
		return isFinal;
	}

	/**
	 * Returns the raw array of bytes
	 * 
	 * @return the byte array
	 */
	public byte[] toByteArray() {
		return data;
	}

	/**
	 * Returns the format of the speech data
	 * 
	 * @return the audio format
	 */
	public AudioFormat getFormat() {
		return format;
	}

	// ===================================
	// UTILITY METHODS
	// ===================================

	/**
	 * Returns the hashcode difference.
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	/**
	 * Returns a fixed number (32).
	 * 
	 * @return 32.
	 */
	@Override
	public int hashCode() {
		return 32;
	}

	/**
	 * Returns a string representation of the data
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Speech data (size: " + data.length / 1000 + " kb.)";
	}

	/**
	 * Returns itself
	 */
	@Override
	public SpeechData copy() {
		return this;
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean contains(Value subvalue) {
		return false;
	}

	/**
	 * Returns an empty list
	 * 
	 */
	@Override
	public Collection<Value> getSubValues() {
		return new ArrayList<Value>();
	}

	/**
	 * Returns the concatenation of the two audio data. If the values are not final,
	 * waits for them to be final.
	 */
	@Override
	public SpeechData concatenate(Value value) {

		if (value instanceof SpeechData) {
			while (!isFinal() || !((SpeechData) value).isFinal()) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
				}
			}

			SpeechData newData = new SpeechData(format);
			newData.currentPos = currentPos;
			newData.write(data);
			newData.write(((SpeechData) value).data);
			newData.isFinal = true;
			return newData;
		}
		else {
			throw new RuntimeException("Cannot concatenate SpeechData and "
					+ value.getClass().getCanonicalName());
		}
	}

}
