// =================================================================                                                                   
// Copyright (C) 2009-2011 Pierre Lison (plison@ifi.uio.no)                                                                            
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

package opendial.common.io;

import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Error handler for XML syntax errors.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLErrorHandler extends DefaultHandler {

	public void error (SAXParseException e) { 
		System.out.println("Parsing error: "+e.getMessage());
	}
	
	public void warning (SAXParseException e) { 
		System.out.println("Parsing problem: "+e.getMessage());
	}
	
	public void fatalError (SAXParseException e) { 
		System.out.println("Parsing error: "+e.getMessage()); 
		System.out.println("Cannot continue."); 
		System.exit(1);
	}
	
}
