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

package opendial.imports;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import opendial.utils.Logger;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML reader utility for specification of dialogue domains.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */

public class XMLDomainReader {

	static Logger log = new Logger("XMLDomainReader", Logger.Level.NORMAL);

	public static final String dialDomainSchema = "resources//domaindef.xsd";
	

	/**
	 * Validates a XML document containing a specification of a dialogue domain.
	 * Returns true if the XML document is valid, false otherwise.
	 * 
	 * @param dialDomain the filename of the XML document
	 * @return true if document is valid, false otherwise
	 * @throws IOException if file cannot be read
	 */
	public static boolean validateXML(String dialDomain) throws IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
 
		try {
			SchemaFactory schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			factory.setSchema(schema.newSchema(new Source[] {new StreamSource(dialDomainSchema)}));
			DocumentBuilder builder = factory.newDocumentBuilder();

			builder.setErrorHandler(new XMLErrorHandler());
			builder.parse(new InputSource(dialDomain));
			log.info("XML parsing of file: " + dialDomain + " successful!");
			return true;
		}
		 catch (SAXException e) {
			log.warning("Validation aborted");
			return false;
		} catch (ParserConfigurationException e) {
			log.warning(e.getMessage());
			return false;
		} 
	}
	

}



/**
 * Small error handler for XML syntax errors.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
final class XMLErrorHandler extends DefaultHandler {

	static Logger log = new Logger("XMLErrorHandler", Logger.Level.NORMAL);
	
	public void error (SAXParseException e) throws SAXParseException { 
		log.warning("Parsing error: "+e.getMessage());
		throw e;
	}
	
	public void warning (SAXParseException e) { 
		log.warning("Parsing problem: "+e.getMessage());
	}
	
	public void fatalError (SAXParseException e) { 
		log.severe("Parsing error: "+e.getMessage()); 
		log.severe("Cannot continue."); 
		System.exit(1);
	}
	
}
