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

import static org.junit.Assert.*;

import java.io.IOException;

import opendial.arch.DialException;
import opendial.domains.Domain;

import org.junit.Test;
 

/**
 *  Testing for the XML Reader of dialogue domains.
 *
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class XMLDomainTest {

	public String dialDomain = "domains//testing//microdom2.xml";
	
	@Test
	public void test1() throws IOException {
		
		boolean isValidated = XMLDomainValidator.validateXML(dialDomain);
		assertTrue(isValidated);
	}
	
	
	@Test
	public void test2() throws IOException, DialException {
		
		Domain domain = XMLDomainReader.extractDomain(dialDomain);
		assertTrue(domain.getEntityTypes().size() == 3);
	}

}
