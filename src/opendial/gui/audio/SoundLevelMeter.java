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
 * @version $Date:: 2012-10-07 01:29:04 #$
 *
 */
@SuppressWarnings("serial")
public class SoundLevelMeter extends JPanel {

	public static Logger log = new Logger("AudioStream", Logger.Level.DEBUG);

	// size of the buffer to calculate the volume
	public static final int BUFFER_SIZE = 1024;

	// the current volume
	private int volume = 0;
	
	/**
	 * Monitors the volume on the sound line, updating it regularly.
	 * 
	 * @param m_line the sound line to monitor
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
	 * @param gg
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
	 * 
	 * @author  Pierre Lison (plison@ifi.uio.no)
	 * @version $Date::                      $
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
				byte[] data = stream.toByteArray();
				if (data != null && data.length > 0) {
					updateVolume(calculateRMSLevel(data));			
				}
				try { Thread.sleep(100) ; } catch (InterruptedException e) { }
			}
			updateVolume(0);
		}
	}

}
