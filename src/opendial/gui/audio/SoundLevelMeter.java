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

package opendial.gui.audio;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import opendial.arch.Logger;
import opendial.datastructs.SpeechStream;



/**
 * Small panel to indicate the current sound level captured by the microphone
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 *
 */
@SuppressWarnings("serial")
public class SoundLevelMeter extends JPanel {

	public static Logger log = new Logger("AudioStream", Logger.Level.DEBUG);

	// size of the buffer to calculate the volume
	public static final int BUFFER_SIZE = 256;

	// the current volume
	private int volume = 0;
	
	/**
	 * Monitors the volume on the sound line, updating it regularly.
	 * 
	 * @param recording the speech stream to record
	 */
	public void monitorVolume(final SpeechStream recording) { 
		(new Thread(new VolumeMonitor(recording))).start();
	}


	/**
	 * Updates the volume on the meter
	 * 
	 * @param volume the new volume
	 */
	private void updateVolume(double volume) {
		this.volume = (int) volume;
		repaint();
	}

	/**
	 * Repaint
	 *
	 * @param gg the graphics
	 */
	@Override
	public void paintComponent(Graphics gg) {
		gg.setColor(Color.GREEN);
		gg.clearRect(0, 0, 150, 20);
		gg.fillRect(0,0, volume*2, 20);
	}


	/**
	 * Calculate the noise level on the microphone
	 * 
	 * @param audioData buffer of audio date
	 * @return the RMS sound level
	 */
	private double calculateRMSLevel(byte[] audioData)
	{ 
		// audioData might be buffered data read from a data line
		long lSum = 0;
		for(int i= audioData.length -1 ; i > audioData.length - BUFFER_SIZE && i > 0; i--) {
			lSum = lSum + audioData[i];
		}

		double dAvg = lSum / audioData.length;

		double sumMeanSquare = 0d;
		for(int j= audioData.length -1 ; j > audioData.length - BUFFER_SIZE && j > 0; j--) {
			sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);
		}
		double averageMeanSquare = sumMeanSquare / Math.min(BUFFER_SIZE, audioData.length);
		return Math.pow(averageMeanSquare,0.5d) + 0.5;
	}


	/**
	 * Thread employed to update the sound level meter as the stream is evolving.
	 */
	final class VolumeMonitor implements Runnable {
		
		SpeechStream stream;
		boolean active = true;
		
		public VolumeMonitor(SpeechStream stream) {
			this.stream = stream;
		}
		
		@Override
		public void run() { 
			while (active && !stream.isClosed()) {
				byte[] data = stream.toByteArray(false);
				if (data != null && data.length > 0) {
					updateVolume(calculateRMSLevel(data));			
				}
				try { Thread.sleep(100) ; } catch (InterruptedException e) { }
			}
			updateVolume(0);
		}
	}

}
