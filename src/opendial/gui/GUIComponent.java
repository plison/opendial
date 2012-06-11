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

import javax.swing.JComponent;

import opendial.arch.Logger;

/**
 * 
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
@SuppressWarnings("serial")
public abstract class GUIComponent extends JComponent {
	
	String tabTitle;
	int tabPosition;
	String tabTip;
	
	public GUIComponent(String title, int tabPosition) {
		this.tabTitle = title;
		this.tabPosition = tabPosition;
		this.tabTip = title;
	}

	/**
	 * 
	 * @return
	 */
	public int getTabPosition() {
		return tabPosition;
	}
	
	public String getTabTitle() {
		return tabTitle;
	}
	
	public void setTabTip(String tabTip) {
		this.tabTip = tabTip;
	}
	
	public String getTabTip() {
		return tabTip;
	}

}
