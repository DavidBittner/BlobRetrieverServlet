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
	
	public ResultSetProcessor( ResultSet queryResults ) throws SQLException {
		results = new ArrayList<>();
		
		this.queryResults = queryResults;
		queryMetaData = this.queryResults.getMetaData();
				
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

}
