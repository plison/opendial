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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import oblig2.state.VisualObject;
import oblig2.state.VisualObject.Colour;
import oblig2.state.VisualObject.Shape;
import oblig2.state.RobotPosition;
import oblig2.state.WorldState;
import oblig2.state.WorldStateListener;
import oblig2.util.Logger;



/**
 * Simulation panel for the world state, containing a set of cells, which can 
 * either have a robot, an object or be empty.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class SimulationPanel  extends JPanel implements WorldStateListener {

	public static Logger log = new Logger("SimulationPanel", Logger.Level.DEBUG);

	// images for the simulation
	Image basicCell;
	Image naoCell;
	Image cubeCell;
	Image cylinderCell;
	Image naoWithCubeCell;
	Image naoWithCylinderCell;

	// world state to reflect on the panel
	WorldState state;

	/**
	 * Creates a new simulation panel reflecting the current world state
	 * 
	 * @param state the state 
	 * @throws IOException if the panel could not be created
	 */
	public SimulationPanel(WorldState state) throws IOException {

		// yes, using a grid layout for this simulation panel is most likely
		// not the best option... but it's fast to code
		super(new GridLayout(state.getGridSizeX(),state.getGridSizeY()));

		this.state = state; 

		// ugly hardcoding
		basicCell = ImageIO.read(new File("resources/cell.png"));
		naoCell = ImageIO.read(new File("resources/cell_nao.png"));
		cubeCell = ImageIO.read(new File("resources/cell_cube.png"));
		cylinderCell = ImageIO.read(new File("resources/cell_cylinder.png"));
		naoWithCubeCell = ImageIO.read(new File("resources/cell_nao_cube.png"));
		naoWithCylinderCell = ImageIO.read(new File("resources/cell_nao_cylinder.png"));

		updatedWorldState();

		this.setPreferredSize(new Dimension(510,400));

		TitledBorder border = BorderFactory.createTitledBorder("Simulation model");
		this.setBorder(border);
		this.setVisible(true);

		state.addListener(this);
	}


	/**
	 * Updates the simulation panel with the current world state
	 */
	@Override
	public void updatedWorldState() {
		log.debug("updating simulation panel");
		this.removeAll();

		Image[][] table = new Image[state.getGridSizeX()][state.getGridSizeY()];

		for (int i = 0 ; i < state.getGridSizeX(); i++) {
			for (int j = 0 ; j < state.getGridSizeY() ; j++) {
				table[i][j] = basicCell;
			}
		}

		// adding the objects
		table = addObjectsToTable(table);

		// adding the robot
		table = addRobotToTable(table);


		for (int j = state.getGridSizeY()-1 ; j >= 0 ; j--) {
			for (int i = 0 ; i <  state.getGridSizeX(); i++) {
				add(new JLabel(new ImageIcon(table[i][j])));
			}
		}

		repaint();
	}


	/**
	 * Adds the objects on the panel
	 * 
	 * @param table current table of images
	 * @return updated table
	 */
	private Image[][] addObjectsToTable(Image[][] table) {
		for (VisualObject obj : state.getAllObjects()) {
			if (obj.getX() < state.getGridSizeX() && obj.getY() < state.getGridSizeY()) {
				if (obj.getShape() == Shape.CUBE && obj.getColour() == Colour.BLUE) {
					table[obj.getX()][obj.getY()] = cubeCell;
				}
				else if (obj.getShape() == Shape.CYLINDER && obj.getColour() == Colour.GREEN) {
					table[obj.getX()][obj.getY()] = cylinderCell;
				}
			}
		}
		return table;
	}

	/**
	 * Adds the robot on the panel
	 * 
	 * @param table current table of images
	 * @return updated table
	 */
	private Image[][] addRobotToTable (Image[][] table) {
		RobotPosition robotPos = state.getRobotPosition();
		if (robotPos.getX() < state.getGridSizeX() && robotPos.getY() < state.getGridSizeY()) {

			if (table[robotPos.getX()][robotPos.getY()] == basicCell) {
				table[robotPos.getX()][robotPos.getY()] = naoCell;
			}
			else if (table[robotPos.getX()][robotPos.getY()] == cubeCell) {
				table[robotPos.getX()][robotPos.getY()] = naoWithCubeCell;
			}
			else if (table[robotPos.getX()][robotPos.getY()] == cylinderCell) {
				table[robotPos.getX()][robotPos.getY()] = naoWithCylinderCell;
			}

			switch (robotPos.getOrientation()) {
			case SOUTH: table[robotPos.getX()][robotPos.getY()] = rotate (table[robotPos.getX()][robotPos.getY()], Math.PI); break;
			case WEST: table[robotPos.getX()][robotPos.getY()] = rotate (table[robotPos.getX()][robotPos.getY()], 3 * Math.PI / 2.0); break;
			case EAST: table[robotPos.getX()][robotPos.getY()] = rotate (table[robotPos.getX()][robotPos.getY()], Math.PI / 2.0); break;
			}
		}
		return table;
	}


	/**
	 * Rotates the image by a certain angle
	 * 
	 * @param img the image
	 * @param angle the angle
	 * @return the rotated image
	 */
	private static Image rotate(Image img, double angle){
		int width = img.getWidth(null);
		int height = img.getHeight(null);
		BufferedImage temp = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = temp.createGraphics();
		g2.rotate(angle, height / 2, height / 2);
		g2.drawImage(img, 0, 0, Color.WHITE, null);
		g2.dispose();
		return temp;
	}


}
