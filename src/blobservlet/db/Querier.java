package blobservlet.db;

import java.sql.Blob;
import java.util.logging.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import blobservlet.db.BlobSet;

public class Querier {
	private Connection conn = null;
	private ResultSetProcessor queryProcessor;
	private final static Logger LOGGER = Logger.getLogger(Querier.class.getName());
	
	PreparedStatement treeQuery = null;
	PreparedStatement blobQuery = null;
	
	public Querier() {
		LOGGER.setLevel(Level.ALL);
		
		try
        {
            Class.forName(Config.getDriver());
        	LOGGER.log(Level.INFO, "Starting connection to "+Config.getUrl() + ".");
        	LOGGER.log(Level.INFO, "Username: "+Config.getUsername());
        	LOGGER.log(Level.INFO, "Password: "+Config.getPass());

            conn = DriverManager.getConnection(Config.getUrl(), Config.getUsername(), Config.getPass());
        }catch( SQLException ex )
		{
        	LOGGER.log(Level.SEVERE, ex.getMessage() + " " + Config.getUrl());
		} catch (ClassNotFoundException ex) {
        	LOGGER.log(Level.SEVERE, "Driver not found: " + ex.getMessage());
		}
		
		try {
			treeQuery = conn.prepareStatement(Config.getQuery());
			blobQuery = conn.prepareStatement(Config.getBlobQuery());
		} catch (SQLException e) {
        	LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public ArrayList<String[]> normalQuery(String start, String end) {
		try {
			if( conn == null )
			{
				LOGGER.log(Level.SEVERE, "Connection object is null.");
				return null;
			}else {
		    	LOGGER.log(Level.INFO, "Connection is not null.");
			}
			
			//No SQL injection for me!
			treeQuery.setString(1, start);
			treeQuery.setString(2, end);
			ResultSet tempSet = treeQuery.executeQuery();
			
	    	LOGGER.log(Level.INFO, "Query done, processing results.");
	        queryProcessor = new ResultSetProcessor( tempSet );
	    	LOGGER.log(Level.INFO, "Results processed.");

	        return queryProcessor.getResults();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
		try {
			conn.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
		return null;
	}
	
	public ArrayList<BlobSet> queryBlobs( String keys, boolean singleDir ) {
		final int BLOB_COL = 8;
		final int NAME_COL = 5;
		final int VOUCHER_COL = 3;
		
		//Not dangerous due to SQL injection check earlier.
		String statement = Config.getBlobQuery().replaceAll("\\?", keys);
		ArrayList<BlobSet> ret = new ArrayList<>();
		try {
			
			ResultSet results = conn.prepareStatement(statement).executeQuery();
			
			String prevKey = "";
			while( results.next() ) {
				
				if( results.getString(6).equals(prevKey) ) {
					continue;
				}else{
					prevKey = results.getString(6);
				}
				
				String name = results.getString( NAME_COL );

				StringBuilder path = new StringBuilder("");
				if( !singleDir )
				{
					for( int i = 1; i < NAME_COL; i++ ) {
						path.append( results.getString(i) + "/" );
					}
				}
				Blob curBlob = results.getBlob( BLOB_COL );
				int expKey = Integer.parseInt(results.getString(VOUCHER_COL).replace("VOUCHER - ", ""));
				ret.add( new BlobSet( name, path.toString(), curBlob, expKey ) );
								
			}
			
		}catch (SQLException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
		}
		return ret;
	}

	public void closeConn() {
		try {
			conn.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public Connection getConn() {
		return conn;
	}
}
