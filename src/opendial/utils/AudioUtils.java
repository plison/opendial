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

package opendial.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility methods for processing audio data.
 * 
 * @author Pierre Lison (plison@ifi.uio.no)
 */
public class AudioUtils {

	final static Logger log = Logger.getLogger("OpenDial");

	/** Audio format for higher-quality speech recognition (frame rate: 16 kHz) */
	static AudioFormat IN_HIGH = new AudioFormat(16000.0F, 16, 1, true, false);

	/** Audio format for lower-quality speech recognition (frame rate: 8 kHz) */
	static AudioFormat IN_LOW = new AudioFormat(8000.0F, 16, 1, true, false);

	/** Audio format for the speech synthesis */
	static AudioFormat OUT = new AudioFormat(16000.0F, 16, 1, true, false);

	/**
	 * Maximum number of samples to consider for the calculation of the root
	 * mean-square
	 */
	static int MAX_SIZE_RMS = 100;

	/**
	 * Selects an target data line for a particular audio mixer.
	 * 
	 * @param mixer the name of the audio mixer
	 * @return the selected line
	 */
	public static TargetDataLine selectAudioLine(Mixer.Info mixer) {

		for (AudioFormat format : Arrays.asList(IN_HIGH, IN_LOW)) {
			try {
				DataLine.Info lineInfo =
						new DataLine.Info(TargetDataLine.class, format);
				if (AudioSystem.getMixer(mixer).isLineSupported(lineInfo)) {
					return AudioSystem.getTargetDataLine(format, mixer);
				}
			}
			catch (LineUnavailableException e) {
				log.warning(" line for mixer " + mixer + " is not available");
				log.info("Available audio mixers: " + getMixers());
			}
		}
		throw new RuntimeException("Cannot obtain audio line for mixer " + mixer);
	}

	/**
	 * Returns the list of all audio mixers
	 * 
	 * @return the list of mixers
	 */
	public static List<String> getMixers() {
		Info[] mixers = AudioSystem.getMixerInfo();

		List<String> mixersStr = new LinkedList<String>();
		for (int i = 0; i < mixers.length; i++) {
			mixersStr.add(mixers[i].getName());
		}
		return mixersStr;

	}

	/**
	 * Returns a list with all audio mixers whose input are compatible with the two
	 * audio format IN_HIGH or IN_LOW.
	 * 
	 * @return the list of all input mixers
	 */
	public static List<Mixer.Info> getInputMixers() {

		List<Mixer.Info> mixers = new ArrayList<Mixer.Info>();

		Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < mixerInfos.length; i++) {
			for (AudioFormat format : Arrays.asList(IN_HIGH, IN_LOW)) {
				if (!mixers.contains(mixerInfos[i])
						&& AudioSystem.getMixer(mixerInfos[i]).isLineSupported(
								new DataLine.Info(TargetDataLine.class, format))) {
					mixers.add(mixerInfos[i]);
				}
			}
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
		Line.Info lineInfo = new DataLine.Info(SourceDataLine.class, OUT);
		for (int i = 0; i < mixerInfos.length; i++) {
			if (AudioSystem.getMixer(mixerInfos[i]).isLineSupported(lineInfo)) {

				mixers.add(mixerInfos[i]);
				try {
					if (mixerInfos[i].getName().equals("Default Audio Device")) {
						defaultMixer = mixerInfos[i];
					}
				}
				catch (Exception e) {
					e.printStackTrace();
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
	 * Returns the audio stream corresponding to the array of bytes
	 * 
	 * @param byteArray the byte array
	 * @return the converted audio stream
	 */
	public static AudioInputStream getAudioStream(byte[] byteArray) {
		try {
			try {
				ByteArrayInputStream byteStream =
						new ByteArrayInputStream(byteArray);
				return AudioSystem.getAudioInputStream(byteStream);
			}
			catch (UnsupportedAudioFileException e) {
				byteArray = addWavHeader(byteArray);
				ByteArrayInputStream byteStream =
						new ByteArrayInputStream(byteArray);
				return AudioSystem.getAudioInputStream(byteStream);
			}
		}
		catch (IOException | UnsupportedAudioFileException e) {
			throw new RuntimeException("cannot convert bytes to audio stream: " + e);
		}
	}

	/**
	 * Reads the input stream and returns the corresponding array of bytes
	 * 
	 * @param stream the initial stream
	 * @return the array of bytes from the stream
	 */
	public static byte[] readStream(InputStream stream) {
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
	 * Generates an audio file from the stream. The file must be a WAV file.
	 * 
	 * @param data the byte array
	 * @param outputFile the file in which to write the audio data could not be
	 *            written onto the file
	 */
	public static void generateFile(byte[] data, File outputFile) {
		try {
			AudioInputStream audioStream = getAudioStream(data);
			if (outputFile.getName().endsWith("wav")) {
				int nb = AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE,
						new FileOutputStream(outputFile));
				log.fine("WAV file written to " + outputFile.getCanonicalPath()
						+ " (" + (nb / 1000) + " kB)");
			}
			else {
				throw new RuntimeException("Unsupported encoding " + outputFile);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("could not generate file: " + e);
		}
	}

	/**
	 * Adds a WAV header to the byte array
	 * 
	 * @param bytes the original array of bytes
	 * @return the new array with the header
	 * @throws IOException if the byte array is ill-formatted
	 */
	private static byte[] addWavHeader(byte[] bytes) throws IOException {

		ByteBuffer bufferWithHeader = ByteBuffer.allocate(bytes.length + 44);
		bufferWithHeader.order(ByteOrder.LITTLE_ENDIAN);
		bufferWithHeader.put("RIFF".getBytes());
		bufferWithHeader.putInt(bytes.length + 36);
		bufferWithHeader.put("WAVE".getBytes());
		bufferWithHeader.put("fmt ".getBytes());
		bufferWithHeader.putInt(16);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putShort((short) 1);
		bufferWithHeader.putInt(16000);
		bufferWithHeader.putInt(32000);
		bufferWithHeader.putShort((short) 2);
		bufferWithHeader.putShort((short) 16);
		bufferWithHeader.put("data".getBytes());
		bufferWithHeader.putInt(bytes.length);
		bufferWithHeader.put(bytes);
		return bufferWithHeader.array();
	}

	/**
	 * Calculate the Root-Mean Square of the audio data
	 * 
	 * @param audioData the audio data
	 * @param format the audio format
	 * @return the corresponding RMS value
	 */
	public static double getRMS(byte[] audioData, AudioFormat format) {

		byte[] sample = new byte[Math.min(50, audioData.length)];
		System.arraycopy(audioData, audioData.length - sample.length, sample, 0,
				sample.length);

		// we must first convert the raw array of bytes into integers
		int[] samples = convertByteArray(audioData, format);

		long sumOfSquares = Arrays.stream(samples).mapToLong(i -> i * i).sum();
		double rootMeanSquare = Math.sqrt(sumOfSquares / samples.length);
		return rootMeanSquare;
	}

	/**
	 * Converts the byte array into an array of integers where each integer
	 * corresponds to an audio sample.
	 * 
	 * @param audioData the audio data
	 * @param format the audio format
	 * @return the corresponding array of integers
	 */
	private static int[] convertByteArray(byte[] audioData, AudioFormat format) {

		if (format.getFrameSize() == 2) {
			int[] samples = new int[Math.min(audioData.length / 2, MAX_SIZE_RMS)];
			int offset = audioData.length - 2 * samples.length;
			for (int i = 0; i < samples.length; i++) {
				if (format.isBigEndian()) {
					samples[i] = ((audioData[offset + i * 2] << 8)
							| (audioData[offset + i * 2 + 1] & 0xFF));
				}
				else {
					samples[i] = ((audioData[offset + i * 2 + 0] & 0xFF)
							| (audioData[offset + i * 2 + 1] << 8));
				}
			}
			return samples;
		}
		else if (format.getFrameSize() == 1) {
			int[] samples = new int[Math.min(audioData.length, MAX_SIZE_RMS)];
			int offset = audioData.length - samples.length;
			for (int i = 0; i < samples.length; i++) {
				samples[i] = (audioData[offset + i] << 8);
			}
			return samples;
		}
		else {
			throw new RuntimeException(
					"unsupported frame size: " + format.getFrameSize());
		}

	}

}
