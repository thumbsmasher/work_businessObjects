package com.idex.businessObjects.dataManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;


public class connectionManager {
	private static Properties prop = new Properties();
	
	private Connection conn;
	private String DB_ServerName = "";
	private String DB_UserName = "";
	private String DB_pwd = "";
	private String DB_InitialDatabase = "";
	
	private final String DB_URL = "jdbc:sqlserver:"; 
	
	private boolean DEBUG = false;
	
	public connectionManager() {
		// Load properties from config.properties file
		loadProps();
			
		DB_ServerName = prop.getProperty("BOBJ_ServerName");
		DB_UserName = prop.getProperty("BOBJ_UserName");
		DB_pwd = prop.getProperty("BOBJ_pwd");
		DB_InitialDatabase = prop.getProperty("BOBJ_InitialDatabase");
		
		conn = initializeDataConnection();
	}
	
	private Connection initializeDataConnection() {
		   if (DEBUG) {System.out.println("Starting connection");}
		   Connection myConn = null;
		   String myConnectionString = DB_URL + "//" + DB_ServerName + ";User=" + DB_UserName + ";Password=" + DB_pwd + ";databaseName=" + DB_InitialDatabase;
		   System.out.println(myConnectionString);
		   if (DEBUG) {System.out.println("     " + myConnectionString);}
		   try {
			   myConn = DriverManager.getConnection(myConnectionString);
		   } catch (Exception ex) {
			   System.out.println("Cannot connect to SQL Server :: " + ex.getMessage());
			   System.exit(0);
		   }
		   return myConn;
	   }
	
	public void closeDataConnection() {
		   try {
			   if (conn.isClosed()) {
				   conn.close();
			   }
			   if (DEBUG) {System.out.println("Connection closed");}
		   } catch (Exception ex) {
			   if (DEBUG) {System.out.println("Cannot close connection");}
		   }
	   }
	
	public Connection getDataConnection() {
		return this.conn;
	}
	
	private static void loadProps() {
		 try {
          //load a properties file
			 prop.load(new FileInputStream("./config.properties"));
			 
		 } catch (IOException ex) {
			 System.out.println("*** ERROR Loading Properties files *** " + ex.getMessage());
			 System.exit(0);
		 }

	 }

}
