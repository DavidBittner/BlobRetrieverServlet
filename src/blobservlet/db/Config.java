package blobservlet.db;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Config {
	static private InputStream settingsFile;
	static private HashMap<String, String> data;
	
	public Config() {
		if( settingsFile == null ) {
			
			data = new HashMap<>();
			settingsFile = QueryFactory.class.getResourceAsStream("resources/config.xml");	

			if( settingsFile == null ) {
				System.out.println("Could not find config file.");
				return;
			}
			
			XMLInputFactory xmlDoc = XMLInputFactory.newInstance();
			try {
				XMLStreamReader xmlReader = xmlDoc.createXMLStreamReader(settingsFile);
				while( xmlReader.hasNext() ) {
					xmlReader.next();
					
					switch( xmlReader.getEventType() ) {
						case XMLStreamReader.START_ELEMENT: {
							addItems(xmlReader);
							break;
						}
						case XMLStreamReader.END_ELEMENT: {
							xmlReader.close();
							return;
						}
					}
				}
				
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}
		}
	}

	private static void addItems( XMLStreamReader in ) throws XMLStreamException {
		String lastName = "";
		StringBuilder curResult = new StringBuilder();
		while(in.hasNext()) {
			in.next();
			
			switch( in.getEventType() ) {
				case XMLStreamReader.START_ELEMENT: {
					lastName = in.getLocalName();
					break;
				}
				case XMLStreamReader.CHARACTERS: {
					curResult.append(in.getText());				
					break;
				}
				case XMLStreamReader.END_ELEMENT: {
					if( !lastName.isEmpty() ) {
						data.put(lastName, curResult.toString().trim());
						lastName = "";
						curResult.setLength(0);
					}
				}
			}
		}
	}
	
	public static String getEntry( String name ) {
		if( data.containsKey(name) ) {
			return data.get(name);
		}else {
			System.out.println("Invalid entry name: " + name);
			return null;
		}
	}
}
