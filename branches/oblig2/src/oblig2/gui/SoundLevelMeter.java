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
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import oblig2.util.AudioRecorder;


/**
 * Small panel to indicate the current sound level captured by the microphone
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class SoundLevelMeter extends JPanel {

	private int volume = 0;
	
	AudioRecorder recorder;
		
	/**
	 * Attaches the meter to the recorder
	 * 
	 * @param recorder the recorder
	 */
	public SoundLevelMeter(AudioRecorder recorder) {
		recorder.attachLevelMeter(this);
	}
	
	/**
	 * Updates the meter volume
	 * 
	 * @param d
	 */
	public void updateVolume(double d) {
		volume =(int) d;
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

}
