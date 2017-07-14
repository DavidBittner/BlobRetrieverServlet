package blobservlet.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

import blobservlet.db.BlobSet;
import blobservlet.db.Tree;

public class QueryFactory {	
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
		
		results.remove(0);
		Tree rootNode = new Tree(null, null, null);
		createTree( results, rootNode );
		
		Tree rooterNode = new Tree("", rootNode);
		rootNode.setTitle("EXPORTS (Root folder)");
		
		Gson rootObject = new Gson();
		String json = rootObject.toJson( rooterNode, Tree.class );

		return json;
	}
	
	//Change the FileOutputStream on line 87 to the output stream supplied by the Servlet
	public static void WriteZip( String keys, HttpServletResponse response, boolean singleDir ) {
		final String useQuery = Config.getBlobQuery().replaceAll("%keys", keys );
		
		Querier blobQuery = new Querier();
		ArrayList<BlobSet> blobs = blobQuery.queryBlobs(useQuery, singleDir);
		
		if( blobs.size() == 0 ) {
			System.err.println("No blobs retrieved.");
			return;
		}else{
			response.addHeader("Content-Disposition","attachment;filename=\"exports.zip\"");
			response.setContentType("application/zip");
		}
		
		ZipOutputStream zipper = null;
		try {
			zipper = new ZipOutputStream(response.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		long fileSize = 0;
		
		for( BlobSet i : blobs ) {
			File path = new File( i.getPath() );
			
			boolean accepted = false;
			for( int iter = 0; !accepted; iter++ ) {
				try {
					File combinedPath = new File( path.toString(), i.getName() );
					combinedPath = new File( createName(combinedPath.toString(), iter) );
					
					ZipEntry e = new ZipEntry(combinedPath.toString());
					zipper.putNextEntry(e);
					
					CopyToStream(i.getBlob().getBinaryStream(), zipper);
									
					zipper.closeEntry();
					fileSize += e.getCompressedSize();

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
		
		if( blobs.size() != 0 ) {
			response.setContentLength((int) fileSize);
		}
		
		try {
			zipper.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		blobQuery.closeConn();
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
}