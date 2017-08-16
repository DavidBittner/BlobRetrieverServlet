package blobservlet.db;

import java.io.File;
import report.generator.ReportGenerator;
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
	
	//Temporary!!
	public static void main( String []args ) {
		new Config();
	}
	
	public static String GetTree( String start, String end, String param ) {		
		queryMaster = new Querier();
		
		Querier.QueryType type;
		if( param.equals("inc") ) {
			type = Querier.QueryType.INVOICE_QUERY;
		}else {
			type = Querier.QueryType.NO_INVOICE_QUERY;
		}
		
		ResultSetProcessor resultProcessor = queryMaster.treeQuery( start, end, type );
		
		if( resultProcessor == null ) {
			LOGGER.log(Level.SEVERE, "SQL Exception occured. ResultProcessor returned null." );
			return "";
		}
		ArrayList<String[]> results = resultProcessor.getResults();
		
		if( results == null )
		{
			return "No results retrieved due to error.";
		}
		
		Collections.sort(results, new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				int keyA = 0;
				int keyB = 0;
				int comp = o1[resultProcessor.getCapEnd()-2].compareTo(o2[resultProcessor.getCapEnd()-2]);
				
				try {
					keyA = Integer.parseInt(o1[resultProcessor.getKeyCol()-1]);
					keyB = Integer.parseInt(o2[resultProcessor.getKeyCol()-1]);
				}
				catch( NumberFormatException ex ) {
					LOGGER.log(Level.WARNING, ex.getMessage());
					LOGGER.log(Level.WARNING, "A: "+Integer.toString(keyA) + " B: " + Integer.toString(keyB));
					return comp;
				}
				
				if( comp == 0 ) {
					comp = Integer.compare(keyA, keyB);
				}
				
				return comp;
			}
		});
		
		Tree rootNode = new Tree(null, null, null);
		createTree( resultProcessor, results, rootNode );
		
		Tree rooterNode = new Tree("", rootNode);
		rootNode.setTitle("EXPORTS (Root folder)");
		
		Gson rootObject = new Gson();
		String json = rootObject.toJson( rooterNode, Tree.class );

		return json;
	}
	
	private static boolean IsNumeric( String str ) {
		for( char i : str.toCharArray() ) {
			if( !Character.isDigit(i) ) {
				return false;
			}
		}
		return true;
	}
	
	//Change the FileOutputStream on line 87 to the output stream supplied by the Servlet
	public static String CreateZip( String keys, boolean singleDir, boolean inc, String inPath ) {
		final int MAX_LIST_SIZE = 999;
		
		Querier blobQuery = new Querier();
		ArrayList<BlobSet> blobs = new ArrayList<>();
		
		ArrayList<String> keyList = new ArrayList<>(Arrays.asList(keys.split(",")));
		while( keyList.size() > 0 ) {
			StringBuilder queryReplace = new StringBuilder("");
			
			int curSize = keyList.size();
			for( int i = 0; i < Math.min(MAX_LIST_SIZE,curSize); i++ ) {
				if( !IsNumeric(keyList.get(0)) ) {
					LOGGER.log(Level.SEVERE, "Quitting due to potential SQL injection.");
					return "0";
				}
				
				queryReplace.append("'"+keyList.get(0)+"'");
				keyList.remove(0);
				
				if( i < Math.min(MAX_LIST_SIZE, curSize)-1 ) {
					queryReplace.append(",");
				}
			}
			ArrayList<BlobSet> temp = blobQuery.queryBlobs(queryReplace.toString(), singleDir, inc);
			blobs.addAll(temp);
		}
		
		
		if( blobs.size() == 0 ) {
			System.err.println("No blobs retrieved.");
			return null;
		}
		
		Date rnow = Date.from(Instant.now());
		String sessionKey = Integer.toString(rnow.hashCode()) + Integer.toString(keys.hashCode());
		
		ZipOutputStream zipper = null;
		try {
			zipper = new ZipOutputStream(new FileOutputStream(new File(inPath, sessionKey+".zip")));
			LOGGER.log(Level.INFO, "Writing file to: " + inPath);

		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		int counter = 0;
		
		//This key is used to determine what the last report written was.
		//Used primarily so it doesn't write more than one.
		int lastKey = -1;
		for( BlobSet i : blobs ) {
			counter++;
			File path = new File( i.getPath() );
			
			if( lastKey != i.getExpKey() ) {
				String usePath = null;
				if(path.getParent() != null ) {
					usePath = path.getParent().toString();
				}else {
					usePath = path.toString();
				}
				lastKey = i.getExpKey();

				File reportName = new File( usePath, "Voucher "+Integer.toString(lastKey)+" report.html" );

				ZipEntry report = new ZipEntry(reportName.toString());
				try {
					zipper.putNextEntry(report);
				} catch (IOException e) {
					e.printStackTrace();
				}
				ReportGenerator reportGen = new ReportGenerator();
				
				reportGen.CreateReport(lastKey, zipper, blobQuery.getConn());
				try {
					zipper.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
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
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			try {
				response.getWriter().write("Failed.");
				response.getWriter().close();
			} catch (IOException e) {
				e.printStackTrace();
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
	
	static private String createPredecessor( String []row, int cur ) {
		StringBuilder ret = new StringBuilder("");
		for( int i = 0; i <= cur; i++ ) {
			ret.append(row[i]);
		}
		
		return ret.toString();
	}
	
	static void createTree( ResultSetProcessor resultProcessor, ArrayList<String[]> data, Tree parent ) {
		
		final int CAP = resultProcessor.getCapEnd();
		
		if( CAP == parent.getDepth() ) {
			return;
		}
		
		String currentAdd = null;
		boolean found = false;
		
		for( String []row : data ) {			
			if( !createPredecessor( row, parent.getDepth() ).equals(currentAdd) ) {
				
				if( parent.getTitle() != null && !createPredecessor( row, parent.getDepth()-1 ).equals(parent.past) ) {
					if( found ) {
						return;
					}
					continue;
				}
				
				found = true;
				currentAdd = createPredecessor( row, parent.getDepth() );
				
				//Second parameter is the location of the key
				Tree newChild = new Tree( row[parent.getDepth()], row[resultProcessor.getKeyCol()-1], parent );
				newChild.past = currentAdd;
				parent.add( newChild );
								
				createTree( resultProcessor, data, newChild );
			}
		}
	}
	
	public static void SingleQuery( String key, HttpServletResponse response ) {
		Querier blobQuerier = new Querier();
		
		ArrayList<BlobSet> blob = blobQuerier.queryBlobs("'"+key+"'", true, true);
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