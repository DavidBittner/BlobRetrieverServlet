package blobservlet.db;

import java.sql.Blob;
import java.util.logging.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import blobservlet.db.BlobSet;

public class Querier {
	private Connection conn = null;
	private ResultSetProcessor queryProcessor;
	private final static Logger LOGGER = Logger.getLogger(Querier.class.getName());
	
	public enum QueryType {
		INVOICE_QUERY,
		NO_INVOICE_QUERY
	}
	
	PreparedStatement treeQueryInvoices = null;
	PreparedStatement treeQueryNonInvoices = null;
	PreparedStatement blobQuery = null;
	
	public Querier() {
		LOGGER.setLevel(Level.ALL);
		
		try
        {
            Class.forName(Config.getEntry("dbdriver"));
        	LOGGER.log(Level.INFO, "Starting connection to "+Config.getEntry("dburl") + ".");
        	LOGGER.log(Level.INFO, "Username: "+Config.getEntry("dbuser"));
        	LOGGER.log(Level.INFO, "Password: "+Config.getEntry("dbpass"));

            conn = DriverManager.getConnection(Config.getEntry("dburl"), Config.getEntry("dbuser"), Config.getEntry("dbpass"));
        }catch( SQLException ex )
		{
        	LOGGER.log(Level.SEVERE, ex.getMessage() + " " + Config.getEntry("dburl"));
		} catch (ClassNotFoundException ex) {
        	LOGGER.log(Level.SEVERE, "Driver not found: " + ex.getMessage());
		}
		
		try {
			treeQueryInvoices    = conn.prepareStatement(Config.getEntry("treequery"));
			treeQueryNonInvoices = conn.prepareStatement(Config.getEntry("treequery2"));
			blobQuery            = conn.prepareStatement(Config.getEntry("blobquery"));
		} catch (SQLException e) {
        	LOGGER.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public ResultSetProcessor treeQuery(String start, String end, QueryType type) {
		try {
			PreparedStatement treeQuery = null;
			switch( type ) {
				case INVOICE_QUERY: {
					treeQuery = treeQueryInvoices;
					break;
				}
				case NO_INVOICE_QUERY: {
					treeQuery = treeQueryNonInvoices;
					break;
				}
			}
			
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

	        return queryProcessor;
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
	
	public ArrayList<BlobSet> queryBlobs( String keys, boolean singleDir, boolean incInvoices ) {
		int BLOB_COL = 0;
		int NAME_COL = 0;
		int VOUCHER_COL = 0;
		int KEY_COL = 0;
		
		String statement;
		if( incInvoices ) {
			//Not dangerous due to SQL injection check earlier.
			statement = Config.getEntry("blobquery").replaceAll("\\?", keys);
		}else {
			statement = Config.getEntry("blobquery2").replaceAll("\\?", keys);
		}
		
		ArrayList<BlobSet> ret = new ArrayList<>();
		try {
			
			ResultSet results = conn.prepareStatement(statement).executeQuery();
			ResultSetMetaData metaData = results.getMetaData();

			for( int i = 1; i <= metaData.getColumnCount(); i++ ) {
				switch( metaData.getColumnName(i) ) {
					case "BLOB":
					{
						BLOB_COL = i;
						break;
					}
					case "NAME":
					{
						NAME_COL = i;
						break;
					}
					case "VOUCHER":
					{
						VOUCHER_COL = i;
						break;
					}
					case "KEY":
					{
						KEY_COL = i;
						break;
					}
				}
			}
			
			String prevKey = "";
			while( results.next() ) {
				
				if( results.getString(KEY_COL).equals(prevKey) ) {
					continue;
				}else{
					prevKey = results.getString(KEY_COL);
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
				
				String tempKey = results.getString(VOUCHER_COL);
				int expKey = Integer.parseInt(tempKey.replace("V",""));
				
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
