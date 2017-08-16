package blobservlet.web;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import blobservlet.db.Config;
import blobservlet.db.Querier;
import blobservlet.db.QueryFactory;

/**
 * Servlet implementation class BlobServ
 */
@WebServlet("/BlobServ")
public class BlobServ extends HttpServlet {
	private final static Logger LOGGER = Logger.getLogger(Querier.class.getName());
	private static final long serialVersionUID = 1L;
      
    public BlobServ() {
        super();
		LOGGER.setLevel(Level.ALL);
    }

    //The function that is called upon a GET request
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		new Config();
		
		Map<String, String[]>params = request.getParameterMap();
		
		Cookie[] cookies = request.getCookies();
		boolean found = false, authed = false;
		String cookieName = "unanetAccess";
		String []contents = null;
		
		if( cookies != null )
		{
			for( Cookie i : cookies ) {
				if( i.getName().equals(cookieName) ) {
					found = true;
					contents = URLDecoder.decode(i.getValue(), "UTF-8").split("|");
					break;
				}
			}
			if( found == true ) {
				contents = contents[2].split("^");
			}
			
		}else
		{
			found = false;
		}
		
		if( !found )
		{
        	LOGGER.log(Level.INFO, "Cookie wasn't found.");
			//response.sendError(HttpServletResponse.SC_FORBIDDEN);
			//return;
		}else{
			for( String i : contents ) {
				if( i.contains("administrator") ) {
					authed = true;
				}
			}
		}
		
		if( !authed )
		{
        	LOGGER.log(Level.INFO, "Would've failed auth.");
			//response.sendError(HttpServletResponse.SC_FORBIDDEN);
			//return;
		}
		
		if( params.containsKey("start") && params.containsKey("end") ) {
        	LOGGER.log(Level.INFO, "Starting date query.");
			
			//The input format expected, and the output format wanted
			SimpleDateFormat startFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat endFormat = new SimpleDateFormat("dd-MMM-yyyy");
			
			Date start = null;
			Date end = null;
			try {
				start = startFormat.parse(params.get("start")[0]);
				end = startFormat.parse(params.get("end")[0]);
			} catch (ParseException e) {
				response.getWriter().write("Invalid date format. Expected: yyyy-MM-dd.");
				return;
			}
			
        	LOGGER.log(Level.INFO, "Dates converted.");

			String json = QueryFactory.GetTree(endFormat.format(start), endFormat.format(end), params.get("incinvoice")[0]);
			
        	LOGGER.log(Level.INFO, "JSON tree generated.");
			
			response.setContentType("application/json");
			response.getWriter().write(json);
		}else if( params.containsKey("sessionid") ) {
			Cookie downloadDone = new Cookie("querierSendFinished", "done");
			response.addCookie(downloadDone);
			
			String id = params.get("sessionid")[0];
			QueryFactory.SendZip( getServletContext().getRealPath("") + id+".zip", response);
			
		}else if( params.containsKey("key")){
			QueryFactory.SingleQuery(params.get("key")[0], response);
		}else {
			response.getWriter().write("Invalid paramater combination.");
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		new Config();
		
		Map<String, String[]>params = request.getParameterMap();
		
		if( params.containsKey("keys") && params.containsKey("singleDir")) {
			String keys = params.get("keys")[0];
			
			boolean singleDir = params.get("singleDir")[0].equals("on");
			boolean inc = params.get("incinvoice")[0].equals("inc");
			
			String sessionKey = QueryFactory.CreateZip(keys, singleDir, inc, getServletContext().getRealPath(""));
			
			if( sessionKey != null ){
				response.getWriter().write(sessionKey);
			}
			return;
		}else {
			LOGGER.log(Level.SEVERE, "Failed to send session due to missing parameters.");
		}
		return;
	}
}
