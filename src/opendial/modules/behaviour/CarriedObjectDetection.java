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

package opendial.modules.behaviour;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.net.ftp.FTPClient;

import com.aldebaran.qimessaging.CallError;

import opendial.arch.Logger;
import opendial.arch.Settings;
import opendial.bn.Assignment;
import opendial.bn.distribs.discrete.SimpleTable;
import opendial.bn.values.ValueFactory;
import opendial.modules.NaoSession;
import opendial.state.DialogueState;

public class CarriedObjectDetection {

	public static void initialise() {
		try {
			log.debug("initialise object detection");
		FTPClient client = new FTPClient();
	    client.connect(Settings.getInstance().nao.ip);
	    client.login("nao", "nao");
	    String remoteFile1 = "recordings/cameras/pickup_end.jpg";
        client.noop();    
        client.deleteFile(remoteFile1);
	    client.logout();
	    log.debug("finished initialisation");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// logger
	public static Logger log = new Logger("CarriedObjectDetection",
			Logger.Level.DEBUG);
	
	public static boolean detectCarriedObject() {
		log.debug("START DETECTION!!");
		return isCarried();
	}
	
	private static BufferedImage retrieveImage(String filename) throws CallError, Exception {
		log.debug("start FTP connection with robot");
		FTPClient client = new FTPClient();

	    client.connect(Settings.getInstance().nao.ip);
	    client.login("nao", "nao");

	    String remoteFile1 = "recordings/cameras/" + filename;
        File downloadFile1 = new File("bin/"+filename);
        OutputStream outputStream1 = new FileOutputStream(downloadFile1);
        client.retrieveFile(remoteFile1, outputStream1);
        outputStream1.close();
    
        client.noop();
        
        client.deleteFile(remoteFile1);
	    client.logout();

	    BufferedImage img= ImageIO.read(new File("bin/"+filename));
	    
	    img= img.getSubimage(125, 0, 75, 125);
		log.debug("finished retrieving and cutting image");
	    return img;
	}
	
	
	private static boolean isCarried() {
		try {
			BufferedImage img = retrieveImage("pickup_end.jpg");
			boolean isCarried = isCarried(img);
			log.debug("carried after: " + isCarried);
			return isCarried;
		}
		catch (Exception e) {
			log.info("could not determine carried prob: " + e);
			return false;
		}
	}

	
	private static boolean isCarried(BufferedImage img2) {
	
		int nbBluePixels = 0;
		int nbRedPixels = 0;
		int nbTablePixels = 0;

		log.debug("image width: " + img2.getWidth());
		
		for (int i = 0  ; i < img2.getWidth();i=i+2) {
			for (int j = 0 ; j < img2.getHeight();j=j+2) {
				Color c = new Color(img2.getRGB(i, j));
				if (c.getRed() > 20 && c.getRed() < 100 && c.getGreen() > 60 
						&& c.getGreen() < 130 && c.getBlue() > 90 && c.getBlue() < 220) {
					nbBluePixels++;
				}
				if (c.getRed() > 140 && c.getRed() < 250 && c.getGreen() > 60 
						&& c.getGreen() < 130 && c.getBlue() > 30 && c.getBlue() < 130) {
					nbRedPixels++;
				}
				else if (c.getRed() > 140 && c.getRed() < 250 && c.getGreen() > 130
						&& c.getGreen() < 220 && c.getBlue() > 140 && c.getBlue() < 230) {
					nbTablePixels++;
				}
			}
		}
		log.debug("nbBluepixels: " + nbBluePixels + " nbRedPixels: " 
		+ nbRedPixels + " nbTablePixels: " + nbTablePixels);
		if (nbBluePixels > 150 || nbRedPixels > 150 || nbTablePixels < 2000) {
			return true;
		}
		return false;
	}
	

}

