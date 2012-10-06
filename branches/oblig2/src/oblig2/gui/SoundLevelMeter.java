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

package oblig2.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JPanel;



/**
 * Small panel to indicate the current sound level captured by the microphone
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class SoundLevelMeter extends JPanel {

	// size of the buffer to calculate the volume
	public static final int BUFFER_SIZE = 1024;

	// the current volume
	private int volume = 0;

	private boolean isMonitoring = false;

	/**
	 * Monitors the volume on the sound line, updating it regularly.
	 * 
	 * @param m_line the sound line to monitor
	 */
	public void monitorVolume(final ByteArrayOutputStream streamo) {
		// update the volume level
		isMonitoring = true;
		Thread t2 = new Thread() {
			public void run() { 
				while (isMonitoring) {
					byte[] data = streamo.toByteArray();
					if (data.length > 0) {
						updateVolume(calculateRMSLevel(data));			
					}
					try { Thread.sleep(80) ; } catch (InterruptedException e) { }
				}
			}
		 } ;
		t2.start();
	}


	public void stopMonitoring() {
		isMonitoring = false;
		updateVolume(0);
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
		for(int i=0; i < audioData.length; i++)
			lSum = lSum + audioData[i];

		double dAvg = lSum / audioData.length;

		double sumMeanSquare = 0d;
		for(int j= audioData.length -1 ; j > audioData.length - BUFFER_SIZE && j > 0; j--) {
			sumMeanSquare = sumMeanSquare + Math.pow(audioData[j] - dAvg, 2d);
		}
		double averageMeanSquare = sumMeanSquare / BUFFER_SIZE;
		return Math.pow(averageMeanSquare,0.5d) + 0.5;
	}



}
