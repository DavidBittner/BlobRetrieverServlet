package report.generator;

import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import blobservlet.db.Config;

public class ReportGenerator {
	private ArrayList<String[]> entries;
	
	final static int PROJ_ORG = 0;
	final static int VOUCHER = 1;
	final static int DATE = 2;
	final static int PERSON = 3;
	final static int TASK = 4;
	final static int EXPENSE_TYPE = 5;
	final static int EXPENSE_AMOUNT = 6;
	final static int PROJ_CODE = 7;
	
	public void CreateReport( int key, OutputStream write, Connection conn ) {
		entries = new ArrayList<>();

		PrintStream writer = null;
		writer = new PrintStream(write);
		
		PopulateEntries( key, conn );
		if( entries.size() <= 0) {
			return;
		}
		
		String start = templa;
		if( entries.get(0)[VOUCHER] != null ) {
			start = start.replace("%1", entries.get(0)[VOUCHER] + " for " + entries.get(0)[PERSON]);
			start = start.replace("%0", "Expense Report "+entries.get(0)[VOUCHER] + " Summary");
		}
		start = start.replace("%CompanyName", Config.getEntry("companyname"));
		
		if( entries.get(0)[DATE] != null ) {
			SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			
			java.util.Date parsedDate = null;
			try {
				parsedDate = date.parse(entries.get(0)[DATE]);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			start = start.replace("%2", new SimpleDateFormat("MM-dd-yyyy").format(parsedDate) );
		}
		
		writer.println(start);
		
		double total = 0;
		for( String []i : entries ) {
			writer.println("<tr>");

			writer.println("<td>"+i[PROJ_ORG]+"</td>");
			writer.println("<td>"+i[PROJ_CODE]+"</td>");
			writer.println("<td>"+i[TASK]+"</td>");
			writer.println("<td>"+i[EXPENSE_TYPE]+"</td>");

			double am = Double.parseDouble((i[EXPENSE_AMOUNT]==null)?("0.0"):(i[EXPENSE_AMOUNT]));
			writer.println("<td>"+String.format( "$%.2f", am)+"</td>");

			total += am;

			writer.println("</tr>");
		}
		String end = templb;
		end = end.replace("%1", String.format( "$%.2f", total));
		
		writer.println(end);
	}
	
	private void PopulateEntries( int key, Connection conn ) {		
		PreparedStatement statement = null;
		try {
			statement = conn.prepareStatement(Config.getEntry("reportquery"));
			statement.setInt(1, key);
			
			ResultSet         results = statement.executeQuery();
			ResultSetMetaData info    = results.getMetaData();
			
			while( results.next() ) {
				
				entries.add(new String[info.getColumnCount()]);
				for( int i = 0; i < info.getColumnCount(); i++ ) {
					entries.get(entries.size()-1)[i] = results.getString(i+1);
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	static final String templa = 
			"<!DOCTYPE HTML>\n" + 
			"<head>\n" + 
			"<style>\n" + 
			"table {\n" + 
			"    border-collapse: collapse;\n" + 
			"    width: 75%;\n" + 
			"}\n" + 
			"\n" + 
			"#header {\n" + 
			"    border: solid;\n" + 
			"    border-width: 0px 0px 2px 0px;\n" + 
			"}\n" + 
			"\n" + 
			"th {\n" + 
			"    background-color: gainsboro;\n" + 
			"    text-align: left;\n" + 
			"}\n" + 
			"\n" + 
			"#total {\n" + 
			"    background-color: white;\n" + 
			"    text-align: right;\n" + 
			"}\n" + 
			"</style>\n" + 
			"</head>\n" + 
			"<body>\n" + 
			"<div id=\"header\">\n" + 
			"    <h1>%CompanyName <span style=\"color: gray; font-weight: normal\"> - Expense Report</span></h1>\n" + 
			"    <h2>Voucher: <span style=\"font-weight: normal\">%1</span></h2>\n" + 
			"    <h2>Expense Date: <span style=\"font-weight: normal\">%2</span></h2>\n" + 
			"</div>\n" + 
			"<div id=\"body\">\n" + 
			"    <h2>%0</h2>\n" + 
			"    <table>\n" + 
			"        <tr>\n" + 
			"            <th>Project Org</th>\n" + 
			"            <th>Project Code</th>\n" + 
			"            <th>Task</th>\n" + 
			"            <th>Expense Type</th>\n" + 
			"            <th>Expense Amount</th>\n" + 
			"        </tr>";
	
	static final String templb = 
			"		<tr>\n" + 
			"            <td></td>\n" + 
			"            <td></td>\n" + 
			"            <td></td>\n" + 
			"            <th id=\"total\">Total:</th>\n" + 
			"            <td>%1</td>\n" + 
			"        </tr>\n" + 
			"    </table>\n" + 
			"</div>\n" + 
			"</body>";
}
