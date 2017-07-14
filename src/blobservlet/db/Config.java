package blobservlet.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Config {
	static private InputStream settingsFile;
	
	static private String query;
	static private String blobQuery;
	
	static private String url;
	static private String pass;
	static private String username;
	static private String driver;
	
	static private String basePath;
	
	public Config() {
		if( settingsFile == null ) {
			
			settingsFile = QueryFactory.class.getResourceAsStream("resources/config.xml");	

			if( settingsFile == null ) {
				System.out.println("Could not find config file.");
				return;
			}
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = null;
			try {
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(settingsFile);
				
			} catch (ParserConfigurationException e) {
				System.out.println("Unable to create xml parser. (You should not be seeing this)");
			} catch (IOException e) {
				System.out.println("Unable to read from config file.");
			} catch (SAXException e) {
				System.out.println("Unable to parse XML file. Format may be corrupt.");
			}	
			
			doc.getDocumentElement().normalize();			
			Element root = doc.getDocumentElement();
			
			setQuery(root.getElementsByTagName("sqlquery").item(0).getTextContent());
			setBlobQuery(root.getElementsByTagName("blobquery").item(0).getTextContent());
			
			setBasePath(root.getElementsByTagName("basepath").item(0).getTextContent());
			setUrl(root.getElementsByTagName("dburl").item(0).getTextContent());
			setPass(root.getElementsByTagName("dbpass").item(0).getTextContent());
			setDriver(root.getElementsByTagName("dbdriver").item(0).getTextContent());
			setUsername(root.getElementsByTagName("dbuser").item(0).getTextContent());
			
			if( getBasePath() == null ) {
				setBasePath("./");
				new File(getBasePath()).mkdir();
			}
		}
	}

	public static String getQuery() {
		return query;
	}

	public static void setQuery(String query) {
		Config.query = query;
	}

	public static String getUrl() {
		return url;
	}

	public static void setUrl(String url) {
		Config.url = url;
	}

	public static String getPass() {
		return pass;
	}

	public static void setPass(String pass) {
		Config.pass = pass;
	}

	public static String getDriver() {
		return driver;
	}

	public static void setDriver(String driver) {
		Config.driver = driver;
	}

	public static String getUsername() {
		return username;
	}

	public static void setUsername(String username) {
		Config.username = username;
	}

	public static String getBlobQuery() {
		return blobQuery;
	}

	public static void setBlobQuery(String blobQuery) {
		Config.blobQuery = blobQuery;
	}

	public static String getBasePath() {
		return basePath;
	}

	public static void setBasePath(String basePath) {
		Config.basePath = basePath;
	}
}
