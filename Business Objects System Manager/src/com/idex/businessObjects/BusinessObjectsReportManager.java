package com.idex.businessObjects;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.idex.businessObjects.sessionManager.sessionManager;
import com.idex.businessObjects.dataManager.connectionManager;
import com.idex.businessObjects.objectManager.folderManager;
import com.idex.businessObjects.objectManager.reportManager;
import com.idex.businessObjects.emailManager.emailManager;
import com.idex.businessObjects.objectManager.userManager;

public class BusinessObjectsReportManager {

	/**
	 * @param args
	 */
	private static final boolean DEBUG 									= false;
	private static final boolean DEBUG_allFolders 						= false;
	private static final boolean DEBUG_allReportsFromFolder 			= false;
	private static final boolean DEBUG_allReportsFromParentFolder		= false;
	private static final boolean DEBUG_allReportInstancesFromOwner		= false;
	private static final boolean DEBUG_allReportLargerThanSize			= false;
	private static final boolean DEBUG_recurringReports 				= false;
	private static final boolean DEBUG_PauseReports 					= false;
	private static final boolean DEBUG_ChangeOwner 						= false;
	private static final boolean DEBUG_PauseReportsFromParentFolder 	= false;
	private static final boolean DEBUG_ReportInstancesOlderThan			= false;
	private static final boolean DEBUG_FailedInstances					= false;
	
	private static final boolean DEBUG_allObjectsForAnalytics			= false;
	
	private static final boolean DEBUG_catalogRecurringJobs 			= false;
	private static final boolean DEBUG_ReportDataSource 				= false;
	
	private static final boolean DEBUG_GetOldInstances					= false;
	private static final boolean DEBUG_deleteInstances					= false;
	
	private static final boolean DEBUG_getUsers				 			= false;
	private static final boolean DEBUG_userManagerType		 			= false;
	private static final boolean DEBUG_userManager						= false;
	private static final boolean DEBUG_deleteUsers						= false;
	
	private static Properties prop = new Properties();
	
	private static String recipient = "defaultemail@yourcompany.com";
	private static String BOEEnvironment = "serverName:6400";
	private static String RecurringJobDBName = "[BOE-XI_31]";
		
	private static final String mySQLGetMaxDateViewed = "SELECT max([TimeStamp]) as LastViewed, sum(CASE WHEN Year(TimeStamp)=Year(GetDate()) THEN 1 ELSE 0 END) as NumberOfViews FROM [BOE-XI_31_logs].dbo.Audit_ObjectViews where Event_Type_ID IN (11, 196609, 196610) AND Object_CUID=?";
	private static final String mySQLGetMaxDateScheduled = "SELECT max([TimeStamp]) as LastScheduled, sum(CASE WHEN Year(TimeStamp)=Year(GetDate()) THEN 1 ELSE 0 END) as NumberSchedules FROM [BOE-XI_31_logs].dbo.Audit_ObjectViews where Event_Type_ID IN (327681, 327682, 327683) AND FolderName=?";

	private static DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
	
	public static void main(String[] args) {
		// Initialize parameter values

		// Load properties from config.properties file
		loadProps();
		
		BOEEnvironment = prop.getProperty("BOEEnvironmentName");
		recipient = prop.getProperty("recipient");
		
		System.out.println("BOE REPORT MANAGER :: " + BOEEnvironment);
		
		String commandParam1 = "";
		String commandParam2 = "";
		
		//System.out.println("Calling command: " + commandLine);
		
		if (args.length>0) {
			commandParam1 = args[0];
			System.out.println("     with param1: " + commandParam1);
		}
		if (args.length>1) {
			commandParam2 = args[1];
			System.out.println("     with param2: " + commandParam2);
		}
		
		if (commandParam1.equals("")) { commandParam1="Help"; }
		
			
		try
		{
			if (commandParam1.equals("Help")) {
				System.out.println("Available commands for BOEReportManager:");
				System.out.println("     catalogRecurringJobs - update the recurring job database");
				System.out.println("     getUserList - get a list of all users with their last logon time");
				System.out.println("     getReportsLargerThanSize - get a list of all reports larger than 50MB");
				System.out.println("     reprocessFailedJobs {yyyy-mm-dd} - resubmit all failed jobs since {param1} date");
				System.out.println("     getReportsFromParentFolder {folderCUID} - returns a list of all report objects from");
				System.out.println("               the parent folder");
				System.out.println("     getReportsForAnalytics - fetches all report objects and archives data to DB");
				System.out.println("     getFolderSecurity {FolderCUID} - displays the security groups that have rights granted");
				System.out.println("               on the folder and all subfolders");
				System.out.println("     clearStaleUserSessions - clears all user sessions that are older than 1 day");
				System.out.println("     reportInstancesOlderThan {yyyy-mm-dd} - sends a list of report instances that are older");
				System.out.println("               than {param1} date");
				System.out.println(""); 
				System.exit(0);
			}
			
			System.out.println("Opening SessionManager");
			// Create an enterprise session
			sessionManager sm = new sessionManager(BOEEnvironment, "administrator", "ce1sys", "secEnterprise");
			System.out.println("Logged on as: " + sm.getLoggedOnUserName());
			if (DEBUG_FailedInstances || commandParam1.equals("reprocessFailedJobs")) {
				System.out.println("Validating date format for: " + commandParam1);
				
				try {
					df2.parse(commandParam2);
					reportManager rm = new reportManager(sm.getiStore());
					if (DEBUG) {System.out.println("Report Manager created... getting failed reports");}
					rm.getFailedInstances(commandParam2);
					
					if (DEBUG) {System.out.println("Found " + rm.size() + " failed instances");}
					
					int myReportList = 0;
					for (myReportList=0;myReportList<rm.size();myReportList++) {
						System.out.println("Rescheduling: " + rm.getReportTitle(myReportList) + " : " + rm.getReportOwner(myReportList));
						try {
							// Reschedule the failed report instance
							rm.rescheduleInstances(rm.getReportCUID(myReportList));
							
							// Delete the instance that was rescheduled
							rm.deleteObject(rm.getReportCUID(myReportList));
						} catch (Exception ex) {
							System.out.println("ERROR *** Unable to reschedule: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : " + rm.getReportCUID(myReportList) + " : " + ex.getMessage());
						}
					}
				} catch (Exception ex) {	
				System.out.println("The input parameter does not match the expected date pattern of yyyy-mm-dd");
					System.exit(0);
				}
				
				
			}
			if (DEBUG_userManager || commandParam1.equals("clearStaleUserSessions")) {
				System.out.print("Getting user sessionManager");
				DateFormat df2 = new SimpleDateFormat("MM-dd-yyyy");
				Date currentDate = new Date();
				
				userManager um = new userManager(sm.getiStore());
				System.out.println(".... done");
				// delete sessions
				//um.deleteUserSession(15750);
				//um.deleteUserSession(15672);
												
				System.out.println("Getting active sessions");
				um.getActiveSessions();
				System.out.println("    found " + um.userCount() + " active sessions");
				
				for (int myUserLoop=0;myUserLoop<um.userCount();myUserLoop++) {
					Boolean isStale = false;
					Date sessionDate = df2.parse(um.getUserLastLogonDate(myUserLoop));
					if ((sessionDate.getTime()-currentDate.getTime())/(1000*60*60*24*1) < 0) {
						// The session is at least 1 day old
						System.out.println(um.getUser(myUserLoop) + " : " + um.getUserID(myUserLoop) + " : " + um.getUserLastLogonDate(myUserLoop) + " : " + (sessionDate.getTime()-currentDate.getTime())/(1000*60*60*24));
						um.deleteUserSession(um.getUserID(myUserLoop));
					} else {
						System.out.println("*** " + um.getUser(myUserLoop) + " : " + um.getUserID(myUserLoop) + " : " + um.getUserLastLogonDate(myUserLoop) + " : " + (sessionDate.getTime()-currentDate.getTime())/(1000*60*60*24));
					}
				}
			
				
				System.out.println(".... done");
								
			}
			
			if (DEBUG_userManagerType) {
				userManager um = new userManager(sm.getiStore());
				//um.setUserNamedLicense("usertest", 1);
								
			}
			
			if (DEBUG_getUsers || commandParam1.equals("getUserList")) {
				userManager um = new userManager(sm.getiStore());
				um.getUserList();
				
				if (DEBUG) {System.out.println("... processing " + Integer.toString(um.userCount()) + " users");}
				
				connectionManager myConn = new connectionManager();
				um.writeUserListToDatabase(myConn.getDataConnection());
				myConn.closeDataConnection();
				
				// Email report objects
				// String message = "<b>ENVIRONMENT: " + BOEEnvironment + "</b>";
				// message += "<table border=1><th>Company</th><th>Location</th><th>User ID</th><th>License Type</th><th>Last Logon Time</th><th>Last Updated</th>";
				// for (int myUserList=0;myUserList<um.userCount();myUserList++) {
					//System.out.println("User: " + um.getUser(myUserList) + " : ");
				//	message += "<tr><td>" + um.getUserCompany(myUserList) + "</td><td>" + um.getUserLocation(myUserList) + "</td><td>" + um.getUser(myUserList) + "</td><td>" + um.getUserLicenseType(myUserList) + "</td><td>" + um.getUserLastLogonDate(myUserList) + "</td><td>" + um.getUserLastUpdated(myUserList) + "</td></tr>";
				//}
				
				// message += "</table>";
				// emailManager.sendmessage(recipient, "** USER LIST ***", message);
				
				um = null;
			}
			
			if (DEBUG_deleteUsers) {
				System.out.println("Deleting users");
				userManager um = new userManager(sm.getiStore());
				//um.deleteUser("clinderman");
				//um.deleteUser("USI-MOLDING");
				
				um = null;
			}
			
			if (DEBUG_allFolders) {
				// Get folder manager
				folderManager fm = new folderManager(sm.getiStore());
				if (DEBUG) {System.out.println("Folder Manager created... getting folders");}
				// Get all folder objects
				//fm.getAllFolders();
				//fm.getRootFolders();
				fm.getFolderByCUID("AQR_zBlPMV9OuHOnSy9xDhw");
				//String folderPath = fm.getFolderCompletePath();
				if (DEBUG) {System.out.println("Found " + fm.size() + " folders : " + fm.getFolderName(0) + " : ");}
			}
			
			if (DEBUG_allReportLargerThanSize || commandParam1.equals("getReportsLargerThanSize")) {
				// Get folder manager
				reportManager rm = new reportManager(sm.getiStore());
				if (DEBUG) {System.out.println("Report Manager created... getting reports larger than size");}
				rm.getReportsLargerThanSize();
				if (DEBUG) {System.out.println("Found " + rm.size() + " reports");}
				
				// Email report objects
				String message = "<b>ENVIRONMENT: " + BOEEnvironment + "</b>";
				message += "<table border=1><th>Owner</th><th>Report Name</th><th>Report Size</th><th>Report Path</th><th>Report CUID</th><th>Report Last Updated</th>";
				int myReportList = 0;
				for (myReportList=0;myReportList<rm.size();myReportList++) {
					System.out.println("Report Number: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : " + rm.getReportSize(myReportList).toString());
					try {
						message += "<tr><td>" + rm.getReportOwner(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportSize(myReportList)/1024/1024 + " MB</td><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportLastUpdated(myReportList) + "</td></tr>";
					} catch (Exception ex) {
						System.out.println("ERROR *** Unable to add: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : " + rm.getReportCUID(myReportList) + " : " + ex.getMessage());
					}
				}
								
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
				if (DEBUG) {System.out.println("Report Manager created... getting reports larger than size - ENDING");}
			}
			
			if (DEBUG_allReportsFromParentFolder || commandParam1.equals("getReportsFromParentFolder")) {
				// Get folder manager
				folderManager fm = new folderManager(sm.getiStore());
				
				if (DEBUG) {System.out.println("Folder Manager created... getting folders from parent");}
				// Get all folder objects
				String parentFolderCUID = commandParam2;
				
				if (parentFolderCUID.equals("")) { 
					System.out.println("This process requires the parent folder CUID as a command line parameter");
					System.exit(0);
				}
				
				fm.getAllChildFolders(parentFolderCUID, true);
				if (DEBUG) {System.out.println("Found " + fm.size() + " folders");}
				
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				// Get all report objects from each folder
				for (int myFolderList=0;myFolderList<fm.size();myFolderList++) {
					if (DEBUG) {System.out.println(" ... Fetching Report Objects for " + fm.getFolderName(myFolderList) + " : " + fm.getFolderCUID(myFolderList));}
					rm.getReportObjectsFromFolder(fm.getFolderCUID(myFolderList));
				}
				
				// Email report objects
				String message = "<b>ENVIRONMENT: " + BOEEnvironment + "</b>";
				message += "<table border=1><th>Object Type</th><th>Object Name</th><th>Report CUID</th><th>Owner</th><th>Folder Path</th><th>Creation Date</th><th>Report Last Modified</th><th>Report Last Scheduled</th><th>Report Last Viewed</th><th>YTD Views</th>";
				connectionManager cm = null;
				Connection BOEAuditConn = null;
				
				try {
					cm = new connectionManager();
					BOEAuditConn = cm.getDataConnection();
				} catch (Exception ex) {
					System.out.println("Unable to open connection to database " + ex.getMessage());
				}
				System.out.println("Audit database connection opened");
				
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					System.out.println("Report Number: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : " + rm.getReportCUID(myReportList));
					
					String lastViewedDate = "";
					String lastScheduledDate = "";
					String numberViewsYTD = "";
					String numberSchedulesYTD = "";
					
					try {
						PreparedStatement psLastViewedDate = BOEAuditConn.prepareStatement(mySQLGetMaxDateViewed);
						psLastViewedDate.setString(1, rm.getReportCUID(myReportList));
						
						PreparedStatement psLastScheduledDate = BOEAuditConn.prepareStatement(mySQLGetMaxDateScheduled);
						String strInstanceFolderPath = "/" + rm.getFolderPath(myReportList).replace(" > ", "/") + "/" + rm.getReportTitle(myReportList) + "/";
						psLastScheduledDate.setString(1, strInstanceFolderPath);
						
						try {
							ResultSet rs = psLastViewedDate.executeQuery();
							if (rs.next()) {
								lastViewedDate = df2.format(rs.getTimestamp("LastViewed"));
								if (!rs.getString("NumberOfViews").equals("0")) {
									numberViewsYTD = rs.getString("NumberOfViews");
								}
							}
						} catch (Exception ex) {
							//if (DEBUG) {System.out.println("Unable to process lastViewedDate :: " + ex.getMessage());}					
						}
						try {
							ResultSet rs = psLastScheduledDate.executeQuery();
							if (rs.next()) {
								lastScheduledDate = df2.format(rs.getTimestamp("LastScheduled"));
								//if (DEBUG) {System.out.println(".....Found historical instance of report!!! " + lastScheduledDate);}
								if (!rs.getString("NumberOfSchedules").equals("0")) {
									numberSchedulesYTD = rs.getString("NumberSchedules");
								}
							}
						} catch (Exception ex) {
							//if (DEBUG) {System.out.println("Unable to process lastViewedDate :: " + ex.getMessage());}					
						}
					} catch (Exception ex) {
						System.out.println("Unable to execute fetch from BOEAuditConn :: " + ex.getMessage());
					}
					if (rm.getObjectType(myReportList).equals("Folder")) {
						// Don't add folders to the list
					} else {
						message += "<tr><td>" + rm.getObjectType(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td><td>" + rm.getFolderPath(myReportList) + "</td><td>" + df2.format(rm.getReporCreationDate(myReportList)) + "</td><td>" + rm.getReportLastUpdated(myReportList) + "</td><td>" + lastScheduledDate.toString() + "</td><td>" + lastViewedDate.toString() + "</td><td>" + numberViewsYTD + "</td></tr>";
					}

					//rm.changeOwner(rm.getReportCUID(myReportList), "dbobo", 0);
				}
				message += "</table>";
				
				cm.closeDataConnection();
				
				System.out.println("Preparing to send list via Email..." );
				
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
			}
			
			if (DEBUG_allObjectsForAnalytics || commandParam1.equals("getReportsForAnalytics")) {

				reportManager rm = new reportManager(sm.getiStore());
				rm.getObjectsForAnalytics();
				
				// Email report objects
				String message = "<b>ENVIRONMENT: " + BOEEnvironment + "</b>";
				message += "<table border=1><th>Object Type</th><th>Object Name</th><th>Report CUID</th><th>Owner</th><th>Creation Date</th><th>Report Last Modified</th><th>Report Last Scheduled</th><th>Report Last Viewed</th><th>YTD Views</th>";
				connectionManager cm = null;
				Connection BOEAuditConn = null;
				
				try {
					cm = new connectionManager();
					BOEAuditConn = cm.getDataConnection();
				} catch (Exception ex) {
					System.out.println("Unable to open connection to database " + ex.getMessage());
				}
				System.out.println("Audit database connection opened");
				
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					System.out.println("Report Number: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : " + rm.getReportCUID(myReportList));
					String lastViewedDate = "";
					String lastScheduledDate = "";
					String numberViewsYTD = "";
					String numberSchedulesYTD = "";
					
					try {
						PreparedStatement psLastViewedDate = BOEAuditConn.prepareStatement(mySQLGetMaxDateViewed);
						psLastViewedDate.setString(1, rm.getReportCUID(myReportList));
						
						PreparedStatement psLastScheduledDate = BOEAuditConn.prepareStatement(mySQLGetMaxDateScheduled);
						String strInstanceFolderPath = "/" + rm.getFolderPath(myReportList).replace(" > ", "/") + "/" + rm.getReportTitle(myReportList) + "/";
						psLastScheduledDate.setString(1, strInstanceFolderPath);
						
						try {
							ResultSet rs = psLastViewedDate.executeQuery();
							if (rs.next()) {
								lastViewedDate = df2.format(rs.getTimestamp("LastViewed"));
								if (!rs.getString("NumberOfViews").equals("0")) {
									numberViewsYTD = rs.getString("NumberOfViews");
								}
							}
						} catch (Exception ex) {
							//if (DEBUG) {System.out.println("Unable to process lastViewedDate :: " + ex.getMessage());}					
						}
						try {
							ResultSet rs = psLastScheduledDate.executeQuery();
							if (rs.next()) {
								lastScheduledDate = df2.format(rs.getTimestamp("LastScheduled"));
								//if (DEBUG) {System.out.println(".....Found historical instance of report!!! " + lastScheduledDate);}
								if (!rs.getString("NumberOfSchedules").equals("0")) {
									numberSchedulesYTD = rs.getString("NumberSchedules");
								}
							}
						} catch (Exception ex) {
							//if (DEBUG) {System.out.println("Unable to process lastViewedDate :: " + ex.getMessage());}					
						}
					} catch (Exception ex) {
						System.out.println("Unable to execute fetch from BOEAuditConn :: " + ex.getMessage());
					}
					message += "<tr><td>" + rm.getObjectType(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td><td>" + df2.format(rm.getReporCreationDate(myReportList)) + "</td><td>" + rm.getReportLastUpdated(myReportList) + "</td><td>" + lastScheduledDate.toString() + "</td><td>" + lastViewedDate.toString() + "</td><td>" + numberViewsYTD + "</td></tr>";
					
				}
				message += "</table>";
				
				// Purge the audit history table
				// PreparedStatement psPurgeObjectTable = BOEAuditConn.prepareStatement(mySQLGetMaxDateViewed);
				
				cm.closeDataConnection();
				
				System.out.println("Preparing to send list via Email..." );
				emailManager.sendmessage(recipient, "** OBJECT LIST FOR ANALYTICS ***", message);
			}
			
			if (DEBUG_allReportInstancesFromOwner) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				// Get all report instances by owner
				rm.getReportInstancesByOwner("CKLASNER");
				//rm.getAllReportObjectsByOwner("CKLASNER");
				
				// Email report objects
				String message = "<b>ENVIRONMENT: " + BOEEnvironment + "</b>";
				message += "<table border=1><th>Parent Folder</th><th>Report Name</th><th>Report CUID</th><th>Report CUID</th><th>Owner</th><th>Report Last Updated</th>";
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					message += "<tr><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td><td></td></tr>";
					System.out.println(rm.getReportTitle(myReportList));
					//rm.changeOwner(rm.getReportCUID(myReportList), "JBARNSDALE", 1);
				}
				message += "</table>";
				
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
			}
			
			if (DEBUG_ReportInstancesOlderThan || commandParam1.equals("reportInstancesOlderThan")) {
				if (DEBUG) {System.out.println("Getting old historical instances");}
				
				try {
					df2.parse(commandParam2);
				} catch (Exception ex) {
					System.out.println("Unable to parse value " + commandParam2 + " as date");
					System.exit(0);
				}
				
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				//m.getReportInstancesOlderThan("2014-04-01");
				rm.getReportInstancesOlderThan(commandParam2);
				
				if (DEBUG) {System.out.println("... found " + Integer.toString(rm.size()) + " reports");}
				
				// Get all report objects from each folder
				String message = "<b>ENVIRONMENT: BOEEnvironment</b>";
				message += "<table border=1><th>Report Instance Name</th><th>Report CUID</th><th>Owner</th><th>Instance Date</th><th>Report Size</th>";
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					message += "<tr><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td><td>" + df2.format(rm.getReporCreationDate(myReportList)) + "</td><td>" + rm.getReportSize(myReportList)/1024/1024 + "</td></tr>";
					//rm.deleteObject(rm.getReportCUID(myReportList));
				}
					
				if (DEBUG) {System.out.println("     ... found " + Integer.toString(rm.size()) + " reports");}
				
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
				
			}
			
			// Get recurring reports
			if (DEBUG_recurringReports) {
				// Get folder manager
				folderManager fm = new folderManager(sm.getiStore());
				
				if (DEBUG) {System.out.println("Getting recurring reports");}
				// Get all folder objects
				fm.getAllChildFolders("AXViYxwXfMVGhhrkwJMwZhg", true);
						
				if (DEBUG) {System.out.println("Found " + fm.size() + " folders");}
				
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				//rm.getReportFromCUID("Ac3bFw4RFctLina3swwFUhk");
				//rm.getAllRecurringReportObjects();
				//rm.getAllReportObjectsByOwner("AMCOLLINS");
								
				// Get all report objects from each folder
				for (int myFolderList=0;myFolderList<fm.size();myFolderList++) {
					if (DEBUG) {System.out.println(" ... Fetching Report Objects for " + fm.getFolderName(myFolderList) + " : " + fm.getFolderCUID(myFolderList));}
					rm.getRecurringReportObjects(fm.getFolderCUID(myFolderList));
				}
				
				if (DEBUG) {System.out.println("... found " + Integer.toString(rm.size()) + " reports");}
				
				// Get all report objects from each folder
				String message = "<b>ENVIRONMENT: BOEEnvironment</b>";
				message += "<table border=1><th>Folder Name</th><th>Recurring Job Name</th><th>Report ID</th><th>Report CUID</th><th>Owner</th>";
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					//System.out.println("Report Number: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : ");
					message += "<tr><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportID(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td></tr>";
					//rm.setReportSchedulePause(rm.getReportCUID(myReportList), 0);
					//rm.changeOwner(rm.getReportCUID(myReportList), "aphifer", 0);
				}
					
				if (DEBUG) {System.out.println("     ... found " + Integer.toString(rm.size()) + " reports");}
				
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
			}
			
			if (DEBUG_allReportsFromFolder) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				// Get all reports from the first folder object
				if (DEBUG) {System.out.println("Getting all reports in folder Aa3cmxLLaMpHgNrbSyjjIN4");}
				rm.getReportObjectsFromFolder("Acd31lRfcWNAl5W80f0rbKc");
				if (DEBUG) {System.out.println("... found " + Integer.toString(rm.size()) + " reports");}
				
				// Get all report objects from each folder
				String message = "<b>ENVIRONMENT: BOEEnvironment</b>";
				message += "<table border=1><th>Folder Name</th><th>Recurring Job Name</th><th>Report ID</th><th>Report CUID</th><th>Owner</th>";
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					//System.out.println("Report Number: " + myReportList + " : " + rm.getReportTitle(myReportList) + " : ");
					message += "<tr><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportID(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td></tr>";
					//rm.setReportSchedulePause(rm.getReportCUID(myReportList), 0);
					//rm.changeOwner(rm.getReportCUID(myReportList), "amcollins", 0);
				}
					
				if (DEBUG) {System.out.println("     ... found " + Integer.toString(rm.size()) + " reports");}
				
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
			}
			
			if (DEBUG_ChangeOwner) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				if (DEBUG_ChangeOwner) {System.out.println("... Attempting to change report owner");}
				//rm.testChangeOwner();
				//rm.changeOwner("AegerPznHgBJqrN5pfryIzQ", "dbobo", 0);
				//rm.changeOwner("AUqvErThWLNOu8wSOucRbRQ", "nmosier", 0);
				//rm.changeOwner("AT05FqGU_e9Hs.kpxGbfAKg", "rrobins", 0);
									
			}
			
			if (DEBUG_PauseReports) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				//rm.setReportSchedulePause("", 0);
				

			}
			if (DEBUG_PauseReportsFromParentFolder) {
				if (DEBUG) {System.out.println("");
				// Get folder manager
				}
				folderManager fm = new folderManager(sm.getiStore());
				
				if (DEBUG) {System.out.println("Folder Manager created... getting folders from parent");}
				// Get all folder objects
				//fm.getAllChildFolders("AcUeZHFWxxZMh_YUqR0Qa2g", true);	//Sample
				fm.getAllChildFolders("AXViYxwXfMVGhhrkwJMwZhg", true);	//Development
				
				if (DEBUG) {System.out.println("Found " + fm.size() + " folders");}
				
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				// Get all report objects from each folder
				for (int myFolderList=0;myFolderList<fm.size();myFolderList++) {
					if (DEBUG) {System.out.println(" ... Fetching Report Objects for " + fm.getFolderName(myFolderList) + " : " + fm.getFolderCUID(myFolderList));}
					//rm.getReportObjectsFromFolder(fm.getFolderCUID(myFolderList));
					rm.getRecurringReportObjects(fm.getFolderCUID(myFolderList));
					if (DEBUG) {System.out.println();}
				}

				String message = "<b>ENVIRONMENT: BOEEnvironment</b> - PAUSED REPORTS";
				message += "<table border=1><th>Folder Name</th><th>Object Name</th><th>Report CUID</th><th>Last Updated</th><th>Owner</th>";
				
				if (DEBUG) {System.out.println("... found " + Integer.toString(rm.size()) + " reports");}
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
				 // Pause the child reports
					System.out.println("     Pausing report: " + rm.getReportTitle(myReportList) + " : " + rm.getFolderPath(myReportList));
					message += "<tr><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportLastUpdated(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td></tr>";
					rm.setReportSchedulePause(rm.getReportCUID(myReportList), 0);
				}
				
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
				
			}
			
			if (DEBUG_catalogRecurringJobs || commandParam1.equals("catalogRecurringJobs")) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				rm.setDebugFlag(DEBUG);
				
				rm.catalogAllRecurringReportObjects(recipient);
								
				//if (DEBUG) {System.out.println("... attempting to record " + Integer.toString(rm.size()) + " reports");}
			
			}
			
			if (DEBUG_ReportDataSource) {
				// Get folder manager
				folderManager fm = new folderManager(sm.getiStore());
				
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				// Get all report objects from all folders
				fm.getAllChildFolders("AcCbGAZ9VRhDmoA8LXP3vmg", true);
								
				if (DEBUG) {System.out.println(" ... I have " + fm.size() + " folders");}
				
				for (int myFolderList=0;myFolderList<fm.size();myFolderList++) {
					if (DEBUG) {System.out.println(" ... Fetching Report Objects for " + fm.getFolderName(myFolderList) + " : " + fm.getFolderCUID(myFolderList));}
					rm.getReportObjectsFromFolder(fm.getFolderCUID(myFolderList));
				}
				
				if (DEBUG) {System.out.println(" ... I have " + rm.size() + " report objects");}
				
				//rm.getReportFromCUID("Aa_nDL.Xeg1KtyFZloInyGg");
				//rm.getReportFromID(4547660);
				//rm.getReportObjectsByDateUpdated();
				
				if (DEBUG) {System.out.println("... attempting to record " + Integer.toString(rm.size()) + " reports");}
				connectionManager myConn = new connectionManager();
				for (int myReportLoop=0;myReportLoop<rm.size();myReportLoop++) {
					rm.getReportDataSources(sm, myReportLoop);
				}
				if (DEBUG) {System.out.println("Getting datasource completed - writing report documentation");}
				int numberOfReports = rm.writeReportDocumentationToDatabase(myConn.getDataConnection());
			
				myConn.closeDataConnection();
			}
			
			if (DEBUG_GetOldInstances) {
				// Get reportManager
				reportManager rm = new reportManager(sm.getiStore());
				
				if (DEBUG) {System.out.println("... Attempting to get old instances");}
				rm.getOldInstancesList("2010.04.01.00.00.0.00");
				if (DEBUG) {System.out.println("        Found: " + rm.size() + " old instances");}
				
				// Get all objects 
				String message = "<b>ENVIRONMENT: BOEEnvironment</b> - OLD INSTANCE REPORT";
				message += "<table border=1><th>Folder Name</th><th>Object Name</th><th>Report CUID</th><th>Last Updated</th><th>Owner</th>";
				
				for (int myReportList=0;myReportList<rm.size();myReportList++) {
					message += "<tr><td>" + rm.getFolderPath(myReportList) + "</td><td>" + rm.getReportTitle(myReportList) + "</td><td>" + rm.getReportCUID(myReportList) + "</td><td>" + rm.getReportLastUpdated(myReportList) + "</td><td>" + rm.getReportOwner(myReportList) + "</td></tr>";
					// Delete the old report instance
					if (DEBUG_deleteInstances) {rm.deleteObject(rm.getReportCUID(myReportList));}
				}
				message += "</table>";
				emailManager.sendmessage(recipient, "** REPORT LIST ***", message);
			}
			
			if (commandParam1.equals("getFolderSecurity")) {
				System.out.println("Running test process");
				folderManager fm = new folderManager(sm.getiStore());
				fm.getAllChildFolders("AYXyFK1xFWFLnB6v2hDqQHU", true);
				
				for (int myFolderLoop=0; myFolderLoop < fm.size(); myFolderLoop++) {
					fm.checkRights(fm.getFolderCUID(myFolderLoop));
				}
				
			}

			// Close enterprise session
			sm.closeSession();	
			System.out.println("Logged off. SM isLoggedOn = " + sm.isLoggedIn());
			
		} catch (Exception ex) {
			System.out.println("*** ERROR CREATING sm : " + ex.getMessage());
			
		}
	}
	
	private static void loadProps() {
		 try {
           //load a properties file
			 prop.load(new FileInputStream("./config.properties"));
			 
		 } catch (IOException ex) {
			 ex.printStackTrace();
		 }

	 }
	
}


