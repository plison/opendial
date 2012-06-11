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

package opendial.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import opendial.arch.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public class DialogueMonitor extends JFrame {

	public static Logger log = new Logger("DialogueMonitor", Logger.Level.NORMAL);

	JTabbedPane tabbedPane;

	List<JComponent> activatedTabs;

	private static DialogueMonitor monitor;

	public static DialogueMonitor getSingletonInstance() {
		if (monitor == null) {
			monitor = new DialogueMonitor();
		}
		return monitor;
	}

	private DialogueMonitor() {

		Container contentPane = getContentPane();

		tabbedPane = new JTabbedPane();

		contentPane.add(tabbedPane);

		setLocation(new Point(200, 200));

		addWindowListener(new WindowAdapter() 
		{
			public void windowClosing(WindowEvent e) 
			{ System.exit(0); }
		}
		); 

		activatedTabs = new LinkedList<JComponent>();

		setPreferredSize(new Dimension(1000,800));
		pack();
	}

	
	public synchronized void addComponent (GUIComponent component) {

		if (tabbedPane.getTabCount() > component.getTabPosition()) {
			tabbedPane.insertTab(component.getTabTitle(), null, component, 
					component.getTabTip(), component.getTabPosition());
		}
		else {
			tabbedPane.addTab(component.getTabTitle(), null, component, component.getTabTip());
		}

		if (component.getTabPosition() == 0) {
			tabbedPane.setSelectedComponent(component);
		}
		if (tabbedPane.getTabCount() > 0) {
			pack();
			setVisible(true);
		}

		activatedTabs.add(component);

	}



	/**
	 * 
	 * @param viewer
	 * @return
	 */
	public synchronized boolean hasTab(JComponent tab) {
		synchronized (activatedTabs) {
			return activatedTabs.contains(tab);
		}
	}

}
