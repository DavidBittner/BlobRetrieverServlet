package blobservlet.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import blobservlet.db.BlobSet;
import blobservlet.db.Tree;

public class QueryFactory {	
	private final static Logger LOGGER = Logger.getLogger(Querier.class.getName());
	static ArrayList<String[]> currentResults; 
	
	static String stDate;
	static String enDate;
	
	private static Querier queryMaster = null;
	
	/*
	 * TODO: Write proper headers for file transfer
	 * TODO: Find out how to export to JSP
	 * TODO: Get a tomcat server running for testing
	 * TODO: Change the query destinations in the js
	 */
	
	public static String GetTree( String start, String end ) {
		/*
		 * Expect sanitized input here. 
		 * Expected format: dd-MMM-yyyy
		 * Example: 07-JAN-2017 
		*/
		queryMaster = new Querier();
		
		String useQuery = Config.getQuery().replaceAll("%sd", start);
		useQuery =  useQuery.replaceAll("%ed", end);
		ArrayList<String[]> results = queryMaster.normalQuery( useQuery );
		if( results == null )
		{
			return "No results retrieved due to error.";
		}
		
		Collections.sort(results, new Comparator<String[]>() {
			public String toSingleString( String[] in ){
				StringBuilder str = new StringBuilder(in[0]);
				for( int i = 1; i < in.length; i++ ) {
					str.append("," + in[i]);
				}
				return str.toString();
			}
			
			@Override
			public int compare(String[] o1, String[] o2) {
				return (toSingleString(o1).compareTo(toSingleString(o2)));
			}
		});
		
		Tree rootNode = new Tree(null, null, null);
		createTree( results, rootNode );
		
		Tree rooterNode = new Tree("", rootNode);
		rootNode.setTitle("EXPORTS (Root folder)");
		
		Gson rootObject = new Gson();
		String json = rootObject.toJson( rooterNode, Tree.class );

		return json;
	}
	
	//Change the FileOutputStream on line 87 to the output stream supplied by the Servlet
	public static String CreateZip( String keys, boolean singleDir, String inPath ) {
		final int MAX_LIST_SIZE = 999;
		
		Querier blobQuery = new Querier();
		ArrayList<BlobSet> blobs = new ArrayList<>();
		
		ArrayList<String> keyList = new ArrayList<>(Arrays.asList(keys.split(",")));
		while( keyList.size() > 0 ) {
			StringBuilder queryReplace = new StringBuilder("");
			int curSize = keyList.size();
			for( int i = 0; i < Math.min(MAX_LIST_SIZE,curSize); i++ ) {
				queryReplace.append(keyList.get(0));
				keyList.remove(0);
				
				if( i < Math.min(MAX_LIST_SIZE, curSize)-1 ) {
					queryReplace.append(",");
				}
			}
			String useQuery = Config.getBlobQuery().replaceAll("%keys", queryReplace.toString() );
			ArrayList<BlobSet> temp = blobQuery.queryBlobs(useQuery, singleDir);
			blobs.addAll(temp);
		}
		
		
		if( blobs.size() == 0 ) {
			System.err.println("No blobs retrieved.");
			return null;
		}
		
		Date rnow = Date.from(Instant.now());
		String sessionKey = String.format("%x", rnow.hashCode() + keys.hashCode());
		
		ZipOutputStream zipper = null;
		try {
			zipper = new ZipOutputStream(new FileOutputStream(new File(inPath, sessionKey+".zip")));
			LOGGER.log(Level.INFO, "Writing file to: " + inPath);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		int counter = 0;
		for( BlobSet i : blobs ) {
			counter++;
			File path = new File( i.getPath() );
			
			boolean accepted = false;
			for( int iter = 0; !accepted; iter++ ) {
				try {
					File combinedPath = new File( path.toString(), i.getName() );
					combinedPath = new File( createName(combinedPath.toString(), iter) );
					
					ZipEntry e = new ZipEntry(combinedPath.toString());
					zipper.putNextEntry(e);
					
					CopyToStream(i.getBlob().getBinaryStream(), zipper);
					LOGGER.log(Level.INFO, "File written: " + combinedPath.toString());
					LOGGER.log(Level.INFO, counter+"/"+blobs.size());
		
					zipper.closeEntry();
					accepted = true;
					
				} catch (IOException | SQLException e) {
					if( e.getMessage().startsWith("duplicate") ) {
						continue;
					}else {
						System.err.println(e.getMessage());
						break;
					}
				}
			}
		}
		
		try {
			zipper.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		blobQuery.closeConn();
		
		return sessionKey;
	}
	
	public static void SendZip( String sessionid, HttpServletResponse response ) {
		response.addHeader("Content-Disposition","attachment;filename=\"exports.zip\"");
		response.setContentType("application/zip");
		
		File zipFile = new File( sessionid );

		response.setContentLength((int)zipFile.length());  
		
		if( !zipFile.exists() ) {
			LOGGER.log(Level.SEVERE, "Invalid session ID!");
			try {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getStackTrace().toString());
			}
			return;
		}else {
			FileInputStream fileReader = null;
			try {
				fileReader = new FileInputStream(zipFile);
				CopyToStream(fileReader, response.getOutputStream());
			} catch (IOException e) {
				//Already handled
			} finally {
				try {
					fileReader.close();
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Failed to close zip file.");
				}
				zipFile.delete();
			}
		}
	}
	
	public static void CopyToStream( InputStream in, OutputStream out )
	{
		final int BUFFER_SIZE = 4096;
		try {
			while( in.available() > 0 ) {
				byte []read = new byte[BUFFER_SIZE];
				int readAmount = in.read(read, 0, BUFFER_SIZE);
				
				out.write(read, 0, readAmount);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String createName( String path, int iter ) {
		if( iter == 0 ) {
			return path;
		}else{
			int dot = path.lastIndexOf(".");
			return path.substring(0, dot) + "(" + iter + ")" + path.substring(dot, path.length());
		}
	}
	
	static void createTree( ArrayList<String[]> data, Tree parent ) {
		
		final int CAP = 5;
		
		if( CAP == parent.getDepth() ) {
			return;
		}
				
		String currentAdd = "";
		boolean found = false;
		for( String []row : data ) {			
			if( !row[parent.getDepth()].equals(currentAdd) ) {
				
				if( parent.getTitle() != null && !row[parent.getDepth()-1].equals(parent.getTitle()) ) {
					if( found ) {
						return;
					}
					continue;
				}
				
				found = true;
				currentAdd = row[parent.getDepth()];
				
				//Second parameter is the location of the key
				Tree newChild = new Tree( currentAdd, row[CAP], parent );
				parent.add( newChild );
								
				createTree( data, newChild );
			}
		}
	}
	
	public static void SingleQuery( String key, HttpServletResponse response ) {
		Querier blobQuerier = new Querier();
		
		String useQuery = Config.getBlobQuery();
		useQuery = useQuery.replace("%keys", "'"+key+"'");
		ArrayList<BlobSet> blob = blobQuerier.queryBlobs(useQuery, true);
		if( blob.size() == 0 ) {
			return;
		}
		
		response.addHeader("Content-Disposition", "inline;filename="+blob.get(0).getName()+";");
		
		try {
			CopyToStream( blob.get(0).getBlob().getBinaryStream(), response.getOutputStream() );
		} catch (SQLException | IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
		blobQuerier.closeConn();
	}
}