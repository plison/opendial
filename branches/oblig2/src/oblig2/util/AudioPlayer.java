/*
 *	AudioPlayer.java
 *
 *	This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999, 2000 by Matthias Pfisterer
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;



/**
 * Class for playing audio files.
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class AudioPlayer extends Thread
{
	
	public static Logger log = new Logger("AudioPlayer", Logger.Level.NORMAL);

	/**	Flag for debugging messages.
	 *	If true, some messages are dumped to the console
	 *	during operation.	
	 */
	private static boolean	DEBUG = false;

	private static int	DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

	String filename;


	public AudioPlayer(String filename)   {
		this.filename = filename;
	}
	
	public void run() {
		try {
		/** Determines if command line arguments are intereted as URL.
		    If true, filename arguments on the command line are
		    interpreted as URL. If false, they are interpreted as
		    filenames. This flag is set by the command line
		    option "-u". It is reset by the command line option
		    "-f".
		*/
		boolean	bInterpretFilenameAsUrl = false;

		/** Flag for forcing a conversion.
		    If set to true, a conversion of the AudioInputStream
		    (AudioSystem.getAudioInputStream(..., AudioInputStream))
		    is done even if the format of the original AudioInputStream
		    would be supported for SourceDataLines directly. This
		    flag is set by the command line options "-E" and "-S".
		*/
		boolean	bForceConversion = false;

		/** Endianess value to use in conversion.
		    If a conversion of the AudioInputStream is done,
		    this values is used as endianess in the target AudioFormat.
		    The default value can be altered by the command line
		    option "-B".
		*/
		boolean	bBigEndian = false;

		/** Sample size value to use in conversion.
		    If a conversion of the AudioInputStream is done,
		    this values is used as sample size in the target
		    AudioFormat.
		    The default value can be altered by the command line
		    option "-S".
		*/
		int	nSampleSizeInBits = 16;


		String	strMixerName = null;

		int	nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;

		int	nInternalBufferSize = AudioSystem.NOT_SPECIFIED;


		AudioInputStream audioInputStream = null;
		if (bInterpretFilenameAsUrl)
		{
			URL url = new URL(filename);
			audioInputStream = AudioSystem.getAudioInputStream(url);
		}
		else
		{
			// Are we requested to use standard input?
			if (filename.equals("-"))
			{
				InputStream inputStream = new BufferedInputStream(System.in);
				audioInputStream = AudioSystem.getAudioInputStream(inputStream);
			}
			else
			{
				File file = new File(filename);
				audioInputStream = AudioSystem.getAudioInputStream(file);
			}
		}
	
		if (DEBUG) out("AudioPlayer.main(): primary AIS: " + audioInputStream);

		/*
		 *	From the AudioInputStream, i.e. from the sound file,
		 *	we fetch information about the format of the
		 *	audio data.
		 *	These information include the sampling frequency,
		 *	the number of
		 *	channels and the size of the samples.
		 *	These information
		 *	are needed to ask Java Sound for a suitable output line
		 *	for this audio stream.
		 */
		AudioFormat	audioFormat = audioInputStream.getFormat();
		if (DEBUG) out("AudioPlayer.main(): primary format: " + audioFormat);
		DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
							 audioFormat, nInternalBufferSize);
		boolean	bIsSupportedDirectly = AudioSystem.isLineSupported(info);
		if (!bIsSupportedDirectly || bForceConversion)
		{
			AudioFormat	sourceFormat = audioFormat;
			AudioFormat	targetFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				sourceFormat.getSampleRate(),
				nSampleSizeInBits,
				sourceFormat.getChannels(),
				sourceFormat.getChannels() * (nSampleSizeInBits / 8),
				sourceFormat.getSampleRate(),
				bBigEndian);
			if (DEBUG)
			{
				out("AudioPlayer.main(): source format: " + sourceFormat);
				out("AudioPlayer.main(): target format: " + targetFormat);
			}
			audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
			audioFormat = audioInputStream.getFormat();
			if (DEBUG) out("AudioPlayer.main(): converted AIS: " + audioInputStream);
			if (DEBUG) out("AudioPlayer.main(): converted format: " + audioFormat);
		}

		SourceDataLine	line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
		if (line == null)
		{
			out("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
			System.exit(1);
		}
		if (DEBUG) out("AudioPlayer.main(): line: " + line);
		if (DEBUG) out("AudioPlayer.main(): line format: " + line.getFormat());
		if (DEBUG) out("AudioPlayer.main(): line buffer size: " + line.getBufferSize());


		/*
		 *	Still not enough. The line now can receive data,
		 *	but will not pass them on to the audio output device
		 *	(which means to your sound card). This has to be
		 *	activated.
		 */
		line.start();

		/*
		 *	Ok, finally the line is prepared. Now comes the real
		 *	job: we have to write data to the line. We do this
		 *	in a loop. First, we read data from the
		 *	AudioInputStream to a buffer. Then, we write from
		 *	this buffer to the Line. This is done until the end
		 *	of the file is reached, which is detected by a
		 *	return value of -1 from the read method of the
		 *	AudioInputStream.
		 */
		int	nBytesRead = 0;
		byte[]	abData = new byte[nExternalBufferSize];
		if (DEBUG) out("AudioPlayer.main(): starting main loop");
		while (nBytesRead != -1)
		{
			try
			{
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			if (DEBUG) out("AudioPlayer.main(): read from AudioInputStream (bytes): " + nBytesRead);
			if (nBytesRead >= 0)
			{
				int	nBytesWritten = line.write(abData, 0, nBytesRead);
				if (DEBUG) out("AudioPlayer.main(): written to SourceDataLine (bytes): " + nBytesWritten);
			}
		}

		if (DEBUG) out("AudioPlayer.main(): finished main loop");

		/*
		 *	Wait until all data is played.
		 *	This is only necessary because of the bug noted below.
		 *	(If we do not wait, we would interrupt the playback by
		 *	prematurely closing the line and exiting the VM.)
		 *
		 *	Thanks to Margie Fitch for bringing me on the right
		 *	path to this solution.
		 */
		if (DEBUG) out("AudioPlayer.main(): before drain");
		line.drain();

		/*
		 *	All data are played. We can close the shop.
		 */
		if (DEBUG) out("AudioPlayer.main(): before close");
		line.close();
		}
		catch (Exception e) {
			log.severe("unable to play sound file, aborting.  Error: " + e.toString());
		}

	}


	// TODO: maybe can used by others. AudioLoop?
	// In this case, move to AudioCommon.
	private static SourceDataLine getSourceDataLine(String strMixerName,
							AudioFormat audioFormat,
							int nBufferSize)
	{
		/*
		 *	Asking for a line is a rather tricky thing.
		 *	We have to construct an Info object that specifies
		 *	the desired properties for the line.
		 *	First, we have to say which kind of line we want. The
		 *	possibilities are: SourceDataLine (for playback), Clip
		 *	(for repeated playback)	and TargetDataLine (for
		 *	 recording).
		 *	Here, we want to do normal playback, so we ask for
		 *	a SourceDataLine.
		 *	Then, we have to pass an AudioFormat object, so that
		 *	the Line knows which format the data passed to it
		 *	will have.
		 *	Furthermore, we can give Java Sound a hint about how
		 *	big the internal buffer for the line should be. This
		 *	isn't used here, signaling that we
		 *	don't care about the exact size. Java Sound will use
		 *	some default value for the buffer size.
		 */
		SourceDataLine	line = null;
		DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
							 audioFormat, nBufferSize);
		try
		{
			if (strMixerName != null)
			{
				Mixer.Info	mixerInfo = AudioCommon.getMixerInfo(strMixerName);
				if (mixerInfo == null)
				{
					out("AudioPlayer: mixer not found: " + strMixerName);
					System.exit(1);
				}
				Mixer	mixer = AudioSystem.getMixer(mixerInfo);
				line = (SourceDataLine) mixer.getLine(info);
			}
			else
			{
				line = (SourceDataLine) AudioSystem.getLine(info);
			}

			/*
			 *	The line is there, but it is not yet ready to
			 *	receive audio data. We have to open the line.
			 */
			line.open(audioFormat, nBufferSize);
		}
		catch (LineUnavailableException e)
		{
			if (DEBUG) e.printStackTrace();
		}
		catch (Exception e)
		{
			if (DEBUG) e.printStackTrace();
		}
		
		return line;
	}




	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}

