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
