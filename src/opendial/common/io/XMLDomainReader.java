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

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import opendial.common.utils.BasicConsoleLogger;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML reader utility for specification of dialogue domains.
 *
 * @author Pierre Lison (plison@ifi.uio.no)
 * @version $date::                      $
 *
 */

public class XMLDomainReader {

	public static final String dialDomain = "domains//testing//microdom1.xml"; 
	public static final String dialDomainSchema = "resources//domaindef.xsd";
	
	static Logger logger = BasicConsoleLogger.getDefaultLogger();
	
	/** 
	 * @param args
	 */
	public static void main(String[] args) {
		
	
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try {
			SchemaFactory schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			factory.setSchema(schema.newSchema(new Source[] {new StreamSource(dialDomainSchema)}));
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			Document document = builder.parse(new InputSource(dialDomain));
			logger.info("XML parsing of file: " + dialDomain + " successful!");

		}
		 catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
	}

}
