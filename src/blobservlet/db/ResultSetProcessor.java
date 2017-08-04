package blobservlet.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

//ResultSetProcessor is responsible for taking the result set from a query and immediately moving it into a proper data structure.
//When the constructor is called, it is supplied with the first query responsible for gathering information for the tree.
//When the function QueryBlobs is called, it actually retrieves the blobs that it wants to print out.
public class ResultSetProcessor {
	private ResultSet queryResults = null;
	private ResultSetMetaData queryMetaData = null;
	private ArrayList<String[]> results = null;
	
	private int CAP_START = 0;
	private int CAP_END = 0;
	private int KEY_COL = 0;
	
	public ResultSetProcessor( ResultSet queryResults ) throws SQLException {
		results = new ArrayList<>();
		
		this.queryResults = queryResults;
		queryMetaData = this.queryResults.getMetaData();
		
		for( int i = 0; i < queryMetaData.getColumnCount(); i++ ) {
			if( queryMetaData.getColumnName(i+1).equals("START") ) {
				CAP_START = i+1;
			}else if( queryMetaData.getColumnName(i+1).equals("END") ) {
				CAP_END = i+1;
			}else if( queryMetaData.getColumnName(i+1).equals("KEY") ) {
				KEY_COL = i;
			}
		}
				
		while( queryResults.next() )
		{
			results.add( new String[queryMetaData.getColumnCount()] );
			for( int i = 0; i < queryMetaData.getColumnCount(); i++ )
			{
				results.get(results.size()-1)[i] = queryResults.getString(i+1);
			}			
		}
		
		this.queryResults.close();
	}

	public ArrayList<String[]> getResults() {
		return results;
	}
	
	public int getCapStart() {
		return CAP_START;
	}
	public int getCapEnd() {
		return CAP_END;
	}
	public int getKeyCol() {
		return KEY_COL;
	}
	
}
