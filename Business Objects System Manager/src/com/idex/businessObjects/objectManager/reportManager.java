package com.idex.businessObjects.objectManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.CeScheduleType;
import com.crystaldecisions.sdk.occa.infostore.IDestination;
import com.crystaldecisions.sdk.occa.infostore.IDestinationPlugin;
import com.crystaldecisions.sdk.occa.infostore.IDestinations;
import com.crystaldecisions.sdk.occa.infostore.IFiles;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.occa.infostore.ISchedulingInfo;
import com.crystaldecisions.sdk.occa.managedreports.IReportAppFactory;
import com.crystaldecisions.sdk.occa.report.application.*;
import com.crystaldecisions.sdk.occa.report.data.IDatabase;
import com.crystaldecisions.sdk.occa.report.data.ITable;
import com.crystaldecisions.sdk.plugin.desktop.common.IReportLogon;
import com.crystaldecisions.sdk.plugin.desktop.common.IReportProcessingInfo;
import com.crystaldecisions.sdk.plugin.destination.diskunmanaged.IDiskUnmanagedOptions;
import com.crystaldecisions.sdk.plugin.destination.smtp.ISMTPOptions;
import com.crystaldecisions.sdk.properties.IProperties;
import com.crystaldecisions.sdk.properties.internal.SDKPropertyBag;
import com.businessobjects.sdk.plugin.desktop.webi.IWebi;
import com.idex.businessObjects.dataManager.connectionManager;
import com.idex.businessObjects.sessionManager.sessionManager;

public class reportManager {
	private List<report> myObjectList;
	private ArrayList<String> myFields;
	private IInfoStore iStore;
	
	private boolean DEBUG = false;
	private boolean DEBUG_showQuery = false;
	//private boolean DEBUGConn = false;
	
	// Get list of all folders in the system
	private final String getReportFromCUID = "SELECT top 1 {fields} from CI_INFOOBJECTS where SI_CUID='{CUID}'";
	private final String getReportFromID = "SELECT top 1 {fields} from CI_INFOOBJECTS where SI_ID={ID}";
	private final String getReportsFromFolder = "SELECT {fields} from CI_INFOOBJECTS where SI_PARENT_CUID='{prompt1}'";
	private final String getReportsListByDateUpdated = "SELECT top 3 {fields} from CI_INFOOBJECTS where SI_RECURRING=0 and SI_UPDATE_TS>='{prompt1}'";
	
	private final String getFailedInstances = "SELECT top 1000 * from CI_INFOOBJECTS where SI_INSTANCE=1 and SI_UPDATE_TS>='{date}' and SI_SCHEDULEINFO.SI_OUTCOME>=2";
	
	private final String getAllRecurringReports = "SELECT top 15000 {fields} from CI_INFOOBJECTS where SI_RECURRING=1";
	private final String getAllReportsByOwner = "SELECT top 5000 {fields} from CI_InfoObjects where SI_Owner='{owner}' and SI_KIND!='FavoritesFolder' and SI_KIND!='Folder' and SI_KIND!='PersonalCategory' and SI_KIND!='Inbox' and SI_INSTANCE=0";
	
	private final String getAllObjects_UsageAnalysis = "SELECT top 50000 {fields} from CI_InfoObjects where SI_KIND!='FavoritesFolder' and SI_KIND!='Folder' and SI_KIND!='PersonalCategory' and SI_KIND!='Inbox' and SI_KIND!='ObjectPackage' and SI_INSTANCE=0";
	
	private final String getReportInstancesOlderThan = "SELECT top 1200 {fields} from CI_INFOOBJECTS where SI_INSTANCE=1 and SI_RECURRING=0 and SI_UPDATE_TS<='{prompt1}'";
	
	private final String getRecurringReportsList = "SELECT top 5000 {fields} from CI_INFOOBJECTS where SI_RECURRING=1 and SI_PARENT_FOLDER_CUID='{prompt1}'";
	private final String getReportsLargerThanSize = "SELECT {fields} from CI_InfoObjects where SI_FILES.SI_VALUE1>50000000";
	
	private final String mySQLDeleteRecurring = "DELETE from [BOE-XI_31_logs].dbo.recurringJobList where reportCUID=?";
	private final String mySQLInsertRecurring = "INSERT INTO [BOE-XI_31_logs].dbo.recurringJobList (reportCUID, reportID, reportName, folderPath, parentReport, parentReportCUID, creationDate, nextRunTime, endDate, scheduledOutputFormat, owner, scheduleType, scheduleInterval, destinationType, destinationEmailFrom, destinationEmailTo, destinationFileLocation, destinationFileName, updateDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GetDate())";
	private final String mySQLMissingRecurring = "SELECT folderPath, parentReport, owner, endDate, updateDate, reportID, reportCUID FROM [boe-xi_31_logs].dbo.recurringJobList where updateDate < dateadd(hh, -3, GetDate()) ORDER BY updateDate DESC, owner";
	private final String mySQLPurgeOldRecurring = "DELETE FROM [BOE-XI_31_logs].dbo.recurringJobList where updateDate < dateadd(hh, -4, GetDate())";
	//private final String mySQLPurgeDeletedRecurring = "DELETE FROM [BOE-XI_31_logs].dbo.recurringJobList from [BOE-XI_31_logs].dbo.recurringJobList as a INNER JOIN [BOE-XI_31_logs].dbo.BOE_AddUpdateDelete_Audit as b on a.reportCUID=b.Object_CUID and b.Event_Type_ID=65543";
	
	private final String mySQLSelect_reportMaster = "SELECT Report_CUID, ReportName, ReportPath, LastUpdated, DateUpdated from [BOE-XI_31_logs].dbo.ReportDocumentor_ReportMaster where Report_CUID=?";
	private final String mySQLUpdate_reportMaster_Matching = "UPDATE [BOE-XI_31_logs].dbo.ReportDocumentor_ReportMaster set DateUpdated=GetDate() where Report_CUID=?";
	private final String mySQLUpdate_reportMaster_Updated = "UPDATE [BOE-XI_31_logs].dbo.ReportDocumentor_ReportMaster set LastUpdated=?, DateUpdated=GetDate() where Report_CUID=?";
	private final String mySQLInsert_reportMaster = "INSERT INTO [BOE-XI_31_logs].dbo.ReportDocumentor_ReportMaster (Report_CUID, ReportName, ReportPath, ReportType, LastUpdated, DateUpdated) VALUES (?, ?, ?, ?, ?, GetDate())";
	private final String mySQLInsert_reportMaster_insertDataSources = "INSERT INTO [BOE-XI_31_logs].dbo.ReportDocumentor_DataSources (Report_CUID, DataSourceName, DataSourceType, LastUpdated) VALUES (?, ?, ?, ?)";
	private final String mySQLInsert_reportMaster_insertTableName = "INSERT INTO [BOE-XI_31_logs].dbo.ReportDocumentor_DataSourceTables (Report_CUID, TableName, LastUpdated) VALUES (?, ?, ?)";
	
	//Default constructor
	public reportManager(Object myiStore) throws Exception  {
		//if (DEBUG) {System.out.println("... Report Manager... constructor");}
		
		myFields = new ArrayList<String>();
		myFields.add("SI_CUID");
		myFields.add("SI_ID");
		myFields.add("SI_NAME");
		myFields.add("SI_KIND");
		myFields.add("SI_PARENT_CUID");
		myFields.add("SI_PARENTID");
		myFields.add("SI_OWNER");
		myFields.add("SI_FILES");
		myFields.add("SI_UPDATE_TS");
		myFields.add("SI_LAST_RUN_TIME");
		myFields.add("SI_INSTANCE");
		myFields.add("SI_PARENT_FOLDER_CUID");
		myFields.add("SI_CREATION_TIME");
		myFields.add("SI_NEXTRUNTIME");
		myFields.add("SI_SCHEDULE_STATUS");
		myFields.add("SI_SCHEDULEINFO");
		myFields.add("SI_DESTINATIONS");
		
		myObjectList = new ArrayList<report>();
		
		iStore = (IInfoStore)myiStore;
	}
    
	public void getReportObjectsFromFolder(String folderCUID) {
		
		String myQuery = getReportsFromFolder.replace("{prompt1}", folderCUID);
		if (DEBUG) {System.out.println("... Report Manager... query :: " + getReportsFromFolder);}
		
		this.getObjectList("report", myQuery, myFields);
	}
	
	public void getReportInstancesOlderThan(String myDate) {
		
		String myQuery = getReportInstancesOlderThan.replace("{prompt1}", myDate);
		if (DEBUG) {System.out.println("... Report Manager... query :: " + getReportInstancesOlderThan);}
		
		this.getObjectList("recurringInstance", myQuery, myFields);
	}
	
	public void getObjectsForAnalytics() {
		
		String myQuery = getAllObjects_UsageAnalysis;
		if (DEBUG) {System.out.println("... Report Manager... query :: " + getAllObjects_UsageAnalysis);}
		
		this.getObjectList("report", myQuery, myFields);
	}
	
	public void getRecurringReportObjects(String myParentFolderCUID) {
		if (DEBUG) {System.out.println("... Report Manager... get recurring reports");}
		
		String myQuery = getRecurringReportsList.replace("{prompt1}", myParentFolderCUID);
		this.getObjectList("recurringInstance", myQuery, myFields);	
	}
	public void setDebugFlag(Boolean myBoolean) {
		System.out.println("... Report Manager... setting debug value to " + myBoolean);
		DEBUG = myBoolean;
	}
	
	public void getReportInstancesByOwner(String myOwner) {
		if (DEBUG) {System.out.println("... Report Manager... get recurring reports by owner");}
		
		String myQuery = getAllRecurringReports + " and SI_OWNER='" + myOwner + "'";
		System.out.println(myQuery);

		this.getObjectList("recurringInstance", myQuery, myFields);
	}
	
	public void getAllRecurringReportObjects() {
		if (DEBUG) {System.out.println("... Report Manager... get recurring reports");}
		
		String myQuery = getAllRecurringReports;
		if (DEBUG) {System.out.println("... Report Manager... query :: " + myQuery);}
		
		this.getObjectList("recurringInstance", myQuery, myFields);
	}
	
	public void getAllReportObjectsByOwner(String myOwner) {
		if (DEBUG) {System.out.println("... Report Manager... get all reports");}
		
		String myQuery = getAllReportsByOwner.replace("{owner}", myOwner);
		
		if (DEBUG) {System.out.println("... Report Manager... query :: " + myQuery);}
		
		this.getObjectList("report", myQuery, myFields);
	}
	
	public void getReportObjectsByDateUpdated() {
		if (DEBUG) {System.out.println("... Report Manager... get reports by last modification date");}
		
		String myQuery = getReportsListByDateUpdated.replace("{prompt1}", "2011.01.01.00.00");
		if (DEBUG) {System.out.println("... Report Manager... query :: " + getReportsFromFolder);}
		
		this.getObjectList("report", myQuery, myFields);
	}
	
	
	public void getReportFromCUID(String myCUID) {
		if (DEBUG) {System.out.println("... Report Manager... get report from CUID");}
		
		String myQuery = getReportFromCUID.replace("{CUID}", myCUID);
		if (DEBUG) {System.out.println("...                   QUERY: " + myQuery);}
		this.getObjectList("report", myQuery, myFields);
	}
	
	public void getReportsLargerThanSize() {
		if (DEBUG) {System.out.println("... Report Manager... get reports larger than size");}
		
		String myQuery = getReportsLargerThanSize;
		if (DEBUG) {System.out.println("...                   QUERY: " + myQuery);}
		this.getObjectList("report", myQuery, myFields);
	}
	
	public void getRecurringReportFromCUID(String myCUID) {
		if (DEBUG) {System.out.println("... Report Manager... get recurring report from CUID");}
		
		String myQuery = getReportFromCUID.replace("{CUID}", myCUID);
		if (DEBUG) {System.out.println("...                   QUERY: " + myQuery);}
		this.getObjectList("recurringInstance", myQuery, myFields);
	}
	
	public void getReportFromID(int myID) {
		if (DEBUG) {System.out.println("... Report Manager... get report from ID");}
		
		String myQuery = getReportFromID.replace("{ID}", Integer.toString(myID));
		this.getObjectList("report", myQuery, myFields);
	}
	
	public String getReportParentFolderCUID(int myArrayIndex) {
		if (DEBUG) {System.out.println("... Report Manager... get report parent folder from CUID (" + myArrayIndex + ") - (" + myObjectList.size() + ")");}
		report myReport = myObjectList.get(myArrayIndex);
		if (DEBUG) {System.out.println("...... have report: " + myReport.getReportName() + " : " + myReport.getParentFolderCUID());}
		return myReport.getParentFolderCUID();
	}
	public String getParentReportCUID(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getParentReportCUID();
	}
	private String getParentReportName(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getParentReportName();
	}
	public String getReportTitle(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getReportName();
	}
	public String getObjectType(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getObjectType();
	}
	public String getReportOwner(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getOwner();
	}
	public int getReportID(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getReportID();
	}
	public Long getReportSize(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getReportSize();
	}
	public String getReportCUID(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getReportCUID();
	}
	public String getFolderPath(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getFolderPath();
	}
	public String getReportLastUpdated(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		return df.format(myReport.getReportLastUpdatedDate());
	}
	public String getReportLastScheduledDate(int myArrayIndex) {
		String dString = "";
		report myReport = myObjectList.get(myArrayIndex);
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
		Date date = null;
		try {
			DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			date = sdf.parse("2010-01-01");
		} catch (ParseException e) {
			System.out.println("Error processing date for getReportLastScheduledDate");
		}
		Date d = (myReport.getReportLastScheduledDate());
		try {
			if (d.after(date)) {
				dString = df.format(d);
			}
		} catch (Exception ex) {
			System.out.println("Unable to format getReportLastScheduledDate :: " + d + " :: " + ex.getMessage());
		}
		
		return dString;
		
	}
	public Date getNextRunTime(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		//DateFormat df = new SimpleDateFormat("MM-dd-yyyy k:m:s z");
		//return df.format(myReport.getNextRunTime());
		return myReport.getNextRunTime();
	}
	public Date getEnddate(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		//DateFormat df = new SimpleDateFormat("MM-dd-yyyy k:m:s z");
		//return df.format(myReport.getEnddate());
		return myReport.getEnddate();
	}
	public Date getReporCreationDate(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		//DateFormat df = new SimpleDateFormat("MM-dd-yyyy k:m:s z");
		//return df.format(myReport.getCreationDate());
		return myReport.getCreationDate();
	}
	private String getDestinationFileName(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getDestinationFileName();
	}

	private String getDestinationFileLocation(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getDestinationFileLocation();
	}

	private String getDestinationEmailTo(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getDestinationEmailTo();
	}

	private String getDestinationEmailFrom(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getDestinationEmailFrom();
	}

	private String getDestinationType(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getDestinationType();
	}

	private int getScheduleInterval(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getScheduleInterval();
	}

	private int getScheduleType(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getScheduleType();
	}

	private String getOutputFormat(int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
		return myReport.getOutputFormat();
	}
	
	public void getReportDataSources(sessionManager mySM, int myArrayIndex) {
		report myReport = myObjectList.get(myArrayIndex);
			
		String myQuery_getReport = "SELECT SI_CUID, SI_NAME, SI_UPDATE_TS, SI_PROCESSINFO, SI_UNIVERSE, SI_UNIVERSE_INFO, SI_LOGON_INFO, SI_PARENTID, SI_FILES ";
			myQuery_getReport += "from CI_InfoObjects where SI_CUID='" + myReport.getReportCUID() + "' AND SI_RECURRING=0 AND SI_KIND!='Folder' AND SI_KIND!='Shortcut'";
		if (DEBUG_showQuery) {System.out.println("   Getting Report DataSources : " + myQuery_getReport);}
	    try
	    {
	    	// Get InfoObject based on Report CUID
	    	IInfoObjects results = iStore.query(myQuery_getReport);
		    	
		    if (!results.isEmpty())
		    {
		    	// For each InfoObject parse the report file location, data connection information, and (if relevant), the tables used in the report
		    	for(int i = 0; i < results.size(); i++)
		    	{
		    		IInfoObject myReport_InfoObject = (IInfoObject)results.get(i);
		    		if (DEBUG) {System.out.println("         HAVE REPORT OBJECT :: " + myReport_InfoObject.getTitle() + " : " + myReport_InfoObject.getCUID() + " : " + myReport_InfoObject.getKind());}
		    		
		    		// File location
		    		String myInputFile = "";
		    	    
		    	    // Data connection properties
		    		int myConnectionProperty = 0;
		    	    int myConnectionType = 0;
		    	    String myConnectionName = "";
		    	    String myConnectionDatabase = "";
		    	    String myConnectionUserName = "";
		    	    ReportClientDocument reportClientDocument;
		    	    boolean hasUniverseQuery = false;

		    	    // Get the physical file location
		    	    try {
		    	    	IFiles myReportFiles = myReport_InfoObject.getFiles();
		    	        myInputFile = myReportFiles.getFRSPathURL() + myReportFiles.properties().get("SI_FILE1").toString();
		    	    } catch (Exception ex) {
		    	    	myInputFile += "Unable to retrieve file path " + ex.getMessage();
		    	    }
		    	    if (DEBUG) {System.out.println("            InputFile : " + myInputFile);}
		    	    myReport.setInputFRSPath(myInputFile);
		    	    
		    	    // Check the object type and handle accordingly 
		    	    if (myReport_InfoObject.getKind().equals("CrystalReport")) {
		    			if (DEBUG) {System.out.println("            === CRYSTAL REPORT ===");}
		    					    			
		    			// If the object type if a Crystal Report object,  
		    			IReportProcessingInfo myReportProcessingInfo = (IReportProcessingInfo) myReport_InfoObject;
		    			for (int logonLoop=0;logonLoop<myReportProcessingInfo.getReportLogons().size();logonLoop++) {
		    				IReportLogon myReportLogon = (IReportLogon) myReportProcessingInfo.getReportLogons().get(logonLoop);
		    				if (myReportLogon.getServerType()==1000) {
		    					// ServerType 1000 is a Universe Connection
		    					hasUniverseQuery = true;
		    					myConnectionProperty = 0;
		    					myConnectionType = myReportLogon.getServerType();
		    					
		    					try {
		    						SDKPropertyBag myProcessingInfoPropertyBag = (SDKPropertyBag)myReport_InfoObject.getProcessingInfo().properties();
		    						SDKPropertyBag myUniverseInfoPropertybag = (SDKPropertyBag)myProcessingInfoPropertyBag.getItem("SI_UNIVERSE_INFO").getPropertyBag();
			    					String myUniverseCUID = myUniverseInfoPropertybag.getItem(Integer.toString(logonLoop+1)).getValue().toString();
			    					
			    					universeManager um = new universeManager(iStore);
			    					um.getUniverseFromCUID(myUniverseCUID);
			    					
			    					myConnectionName = um.getUniverseName(0);
			    					if (DEBUG) {System.out.println("               Report Logon Universe: " + myUniverseCUID + " : " + myConnectionName);}
			    					
			    					myConnectionDatabase = "Data Warehouse";
			    					myConnectionUserName = "";
		    					} catch (Exception getUniverseException) {
		    						if (DEBUG) {System.out.println("               *** ERROR *** Cannot get universe info: " + getUniverseException.getMessage());}
		    					}
		    					
		    				} else {
		    					if (myReportLogon.isOriginalDataSource()) {
		    						myConnectionProperty = 0;
			    					myConnectionType = myReportLogon.getServerType();
			    					myConnectionName = myReportLogon.getServerName().toString();
			    					myConnectionDatabase = myReportLogon.getDatabaseName();
			    					myConnectionUserName = myReportLogon.getUserName().toString();
			    				} else {
			    					myConnectionProperty = 1;
			    					myConnectionType = myReportLogon.getCustomServerType();
			    					myConnectionName = myReportLogon.getCustomServerName();
			    					myConnectionDatabase = myReportLogon.getCustomDatabaseName();
			    					myConnectionUserName = myReportLogon.getCustomUserName().toString();
		    					}
		    				}
		    				
		    				// Add the data connection to the report properties
		    				if (DEBUG) {System.out.println("               Report Logon Added : " + myConnectionType + " : " + myConnectionName + " : " + myConnectionDatabase + " : " + myConnectionUserName);}
		    				myReport.addDataConnection(myConnectionProperty, myConnectionType, myConnectionName, myConnectionDatabase, myConnectionUserName);
		    			}
		    			
		    			// If the report does not have a universe query (should only be those reports with ODBC connections, get the list of tables it uses
		    			if (!hasUniverseQuery && 1==0) {
		    				// Initialize the ReportClientDocument object so that more detailed report information can be obtained.
				    		try {
				    			if (DEBUG) {System.out.println("            Attempting to open report - creating ReportAppFactory");}
				    			IReportAppFactory reportAppFactory = mySM.getRASReportService();
				    			if (DEBUG) {System.out.println("            ReportAppFactory created");}
				    			
					    		reportClientDocument = reportAppFactory.openDocument(myReport_InfoObject, 0, Locale.ENGLISH);
				    			//reportClientDocument = reportAppFactory.openDocument(myReport_InfoObject.getID(), 0, Locale.ENGLISH);
				    			
					    		if (DEBUG) {System.out.println("            Report opened");}
				    			
					    		IDatabase myRptDatabase = reportClientDocument.getDatabase();	
					    		for (int myTableLoop=0;myTableLoop<myRptDatabase.getTables().size();myTableLoop++) {
					    			ITable myTable = myRptDatabase.getTables().getTable(myTableLoop);
					    			if (DEBUG) {System.out.println("                Table: " + myTable.getName() + " : " +  myTable.getDescription());}
					    			// Add table to report table list
					    			myReport.addDataTable(myTable.getName());
					    		}
					    		
					    		reportClientDocument.close();
				    		} catch (Exception ex) {
				    			if (DEBUG) {System.out.println("            Unable to open the report " + ex.getMessage());}
				    		}
		    			} else {
		    				// If the report uses universe connections, record the universe name as a table name
		    				
		    			}
		    				
		    			if (DEBUG) {System.out.println("            Report Logon Completed");}

		    	    } else if (myReport_InfoObject.getKind().equals("Webi")) {
		    			if (DEBUG) {System.out.println("         WEBI REPORT");}
		    			try {
		    				IWebi myWebIReport = (IWebi)myReport_InfoObject;
		    				Iterator<Object> mySetIt = myWebIReport.getUniverses().iterator();
		    				int myUniverseLoop = myWebIReport.getUniverses().size();
		    				
		    				if (myUniverseLoop==0) { 
		    					myReport.addDataConnection(0, 1000, "Unknown Universe", "Data Warehouse", ""); 
		    				}
		    				
		    				while (mySetIt.hasNext()) {
		    					int myUniverseID = Integer.parseInt((String) mySetIt.next().toString());

		    					universeManager um = new universeManager(iStore);
		    					um.getUniverseFromID(myUniverseID);
		    					if (DEBUG) {System.out.println("               -> : " + um.getUniverseName(0));}
		    					
		    					myConnectionProperty = 0;
		    					myConnectionType = 1000;
		    					myConnectionName = um.getUniverseName(0);
		    					myConnectionDatabase = "Data Warehouse";
		    					myConnectionUserName = "";
		    					myReport.addDataConnection(myConnectionProperty, myConnectionType, myConnectionName, myConnectionDatabase, myConnectionUserName);
		    				}
		    				
		    			} catch (Exception ex) {
		    				if (DEBUG) {System.out.println("            *** ERROR getting universe info *** : " + ex.getMessage());}
		    			}
						
		    			
		    		} else {
		    			if (DEBUG) {System.out.println("         UNKNOWN FILE TYPE: " + myReport_InfoObject.getKind() + myReport_InfoObject.getCUID());}
		    		}
		    		
		       	}
		    } else {
		    	// This routine will fail if the report CUID is invalid, or if the InfoObject is a recurring job
		    	if (DEBUG) {System.out.println("         *** ERROR *** :: Object not found for " + myReport.getReportCUID());}
		    }
		    	
		    if (DEBUG) {System.out.println("         Closing dataSourceManager");}
	    } 
	    	    
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... dataSourceManager SDK EXCEPTION " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... dataSourceManager GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void setReportSchedulePause(String myReportCUID, int myRefID) {
		// Sets the status of a recurring job to PAUSED
		// ISchedulingInfo has a setFlag method
		// Possible flag values appear to are: 
		// 		PAUSE = 1; 
		//	    RESUME = 0; 
		//	    RETRY = 8; 
		
		// myRefID controls whether the report CUID references the parent CUID, or the 
		// 		CUID of the actual recurring job
		//      ACTUAL CUID = 0, PARENT CUID = 1    
		String myRefVal = "";
		if (myRefID==1){myRefVal="SI_PARENT_CUID";} else {myRefVal="SI_CUID";}
	
		String myQuery_getReport = "SELECT * from CI_InfoObjects where " + myRefVal + "='" + myReportCUID + "' AND SI_RECURRING=1";
	    
	    try
	    {
	    	// Get InfoObject based on Report CUID
	    	IInfoObjects results = iStore.query(myQuery_getReport);
		    	
		    if (!results.isEmpty())
		    {
		    	// For each InfoObject, set the ISchedulingInfo flag to 1 (PAUSED)
		    	for(int i = 0; i < results.size(); i++)
		    	{
		    		IInfoObject myReport = (IInfoObject)results.get(i);
		    		if (DEBUG) {System.out.println("         PAUSING RECURRING INSTANCE :: " + myReport.getTitle() + " : " + myReport.getCUID());}
		    		myReport.getSchedulingInfo().setFlags(1);
		       	}
		    } else {
		    	// This routine will fail if the report CUID is invalid, or if the InfoObject is not a recurring job
		    	if (DEBUG) {System.out.println("         *** ERROR *** :: Recurring object not found for " + myRefVal + " : " + myReportCUID);}
		    }
		    	
		    // Commit the changed InfoObjects group to CMS
		    System.out.println("Saving... " );
		    iStore.commit(results); 
		    System.out.println("Complete");
		    
	        System.out.println("   Closing changeManager");
	    } 
	    	    
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... pauseRecurringJob SDK EXCEPTION " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... pauseRecurringJob GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void getObjectList(String myObjectType, String myQuery, ArrayList<String> myFields) {
		// Perform the query.
	    IInfoObjects results;
	    
	    // Get the fields from those passed for use in the InfoObjects query
	    String myFieldsAsString = "";
	    for(int i=0;i<myFields.size();i++) {
	    	myFieldsAsString += myFields.get(i) + ", ";
	    }
	    myFieldsAsString = myFieldsAsString.substring(0, myFieldsAsString.length()-2);
	    
	    myQuery = myQuery.replace("{fields}", myFieldsAsString);
	    if (DEBUG) {System.out.println("...... Report Manager query :: " + myQuery);}
	    
	    try
	    {
	        results = iStore.query(myQuery);
	        if (DEBUG) {System.out.println("...... Report Manager query :: I have " + results.size() + " objects");}
	        if (DEBUG) {System.out.println("...................Fetching object array for " + myObjectType + " objects");}
	        
	        if (results.isEmpty())
		    {
		    	// Return an informative message if there are no sub-folders.
	        	System.out.println("No objects found matching these specifications");
		    } else {
		    	for(int i = 0; i < results.size(); i++)
		        {
		    		IInfoObject infoObject = (IInfoObject)results.get(i);
		            
		            try {
		            	if (DEBUG_showQuery) {System.out.println(myObjectType + " : " + infoObject.getCUID() + " : " + infoObject.getTitle() + " : " + infoObject.getParentCUID());}
		            	if (myObjectType.equals("report") & infoObject.isInstance()==false & !infoObject.getKind().equals("folder")) { 
		            		if (DEBUG) {System.out.println("........................ Get Object Details: ObjectType=REPORT and INSTANCE=FALSE : " + infoObject.getCUID());}
		            		
		            		String owner = infoObject.properties().getProperty("SI_OWNER").toString();
		            		DateFormat df = new SimpleDateFormat("EEE MMM dd k:m:s z yyyy");

		            		Date creationDate = df.parse(infoObject.properties().getProperty("SI_CREATION_TIME").toString());
		            		Date lastUpdatedDate = df.parse(infoObject.properties().getProperty("SI_UPDATE_TS").toString());
		            		Date lastScheduledDate = null;
		            		if (infoObject.properties().getProperty("SI_LAST_RUN_TIME") != null) {
		            			lastScheduledDate = df.parse(infoObject.properties().getProperty("SI_LAST_RUN_TIME").toString());
		            		}
		            		
		            		
		            		// Get the folder CUID of the report
		            		String myParentFolderCUID = infoObject.getParentCUID();
		            		String folderPath = "";
		            		boolean parentIsFolder = false;
		            		
		            		while (!parentIsFolder) {
		            			// Check to make sure that the parent is a type FOLDER
			            		String getParentQuery = getReportFromCUID.replace("{CUID}", myParentFolderCUID).replace("{fields}",  "*");
			            		if (DEBUG) {System.out.println(" ... Checking to see if object is folder => " + myParentFolderCUID);}
			            		if (DEBUG) {System.out.println(" ...  " + parentIsFolder + " : " + getParentQuery);}
			            		
			            		IInfoObjects myParentObjects =  iStore.query(getParentQuery);
			            		IInfoObject myParentObject;
			            		if (myParentObjects.size()>0) {
			            			myParentObject = (IInfoObject)myParentObjects.get(0);
			            			if (DEBUG) {System.out.println(" ... .... Object is a => " + myParentObject.getKind());}
			            			if (myParentObject.getKind().equals("Folder") || myParentObject.getKind().equals("FavoritesFolder")) {
				            			parentIsFolder = true;
				            			
				            		} else {
				            			myParentFolderCUID = myParentObject.getParentCUID();
				            		}
			            		} else {
			            			// Special case where Parent CUID does not exist as an object
			            			// Possibly parent folder = Temporary Storage
			            			parentIsFolder = true;
			            		}
			            		
		            		}
		            		
		            		// Get complete path from parent report's folder
			            	if (DEBUG) {System.out.println("===== reportManager - getting foldermanager for parent folder :: " + myParentFolderCUID);}
			            	
			            	folderManager parentFolder = new folderManager(iStore);   
		            		try {
				            	parentFolder.getFolderByCUID(myParentFolderCUID);
				            	folderPath = parentFolder.getFolderCompletePath();
				            } catch (Exception EX) {
				            	if (DEBUG) {System.out.println("***** ERROR GETTING FOLDER PATH ******");}
				            	folderPath = "UNKNOWN";
				            }
		            		
		            		Long myReportSize = Long.parseLong("0");
		            		try {
		            			IProperties props=(IProperties)infoObject.properties().getProperty("SI_FILES").getValue();            			
		            			// Get the report FILE size in KB
		            			myReportSize = Long.parseLong(props.getProperty("SI_VALUE1").toString());		            				            			
		            		} catch (Exception ex) {
		            			if (DEBUG) {System.out.println("ERROR Getting FileInfo -> " + infoObject.getCUID() + " :: " + ex.getMessage());}
		            		}
		            		
			            	if (DEBUG) {System.out.println("***** " + myObjectList.size() + "  : " + infoObject.getTitle() + " : " + infoObject.getParentCUID() + " : " + folderPath);}
		            		
		            		report myReport = new report(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle(), infoObject.getParentCUID(), folderPath, owner, infoObject.getKind(), lastUpdatedDate);
		            		myReport.setReportSize(myReportSize);
		            		myReport.setCreationDate(creationDate);
		            		if (lastScheduledDate != null) {
		            			myReport.setReportLastScheduledDate(lastScheduledDate);
		            		}
		            		myReport.setObjectType(infoObject.getKind());
		            		
		            		if (DEBUG) {System.out.println(folderPath + "\t" + infoObject.getCUID() + "\t" + infoObject.getTitle());}
		            		myObjectList.add(myReport);
		            		
		            		//System.out.print(".");
		            	} else if (myObjectType.equals("CUID")) {
		            		if (DEBUG) {System.out.println(infoObject.getCUID() + "\t" + infoObject.getID() + "\t" + infoObject.getTitle());}
		            		myObjectList.add(new report(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle(), ""));
		            		
		            	} else if (myObjectType.equals("recurringInstance") || infoObject.isInstance()==true) {
		            		if (DEBUG) {System.out.println("........................ Get Object Details: ObjectType=RECURRING INSTANCE OR INSTANCE=TRUE");}
		            		
		            		DateFormat df = new SimpleDateFormat("EEE MMM dd k:m:s z yyyy");
		            		Date creationDate = df.parse(infoObject.properties().getProperty("SI_CREATION_TIME").toString());
		            		Date nextRunTime = new Date();
		            		try {
		            			nextRunTime = df.parse(infoObject.properties().getProperty("SI_NEXTRUNTIME").toString());
		            		} catch (Exception ex) {
		            			// Do nothing
		            		}
		            		Date lastUpdatedDate = df.parse(infoObject.properties().getProperty("SI_UPDATE_TS").toString());
		            		//Date lastScheduledDate = df.parse(infoObject.properties().getProperty("SI_LAST_RUN_TIME").toString());
		            		Date lastScheduledDate = null;
		            		if (infoObject.properties().getProperty("SI_LAST_RUN_TIME") != null) {
		            			lastScheduledDate = df.parse(infoObject.properties().getProperty("SI_LAST_RUN_TIME").toString());
		            		}
		            		
		            		String outputFormat = infoObject.properties().getProperty("SI_KIND").toString();
		            		String owner = infoObject.properties().getProperty("SI_OWNER").toString();
		            		String destinationType = "";
		            		String destinationEmailFrom = "";
		            		String destinationEmailTo = "";
		            		String destinationFileLocation = "";
		            		String destinationFileName = "";
		            				            			
		            		if (DEBUG) {System.out.println("......reportManager - get scheduling info " + infoObject.getCUID() + " : ");}
		            		ISchedulingInfo mySchedule = infoObject.getSchedulingInfo();
		            		// Schedule SI_SCHEDULE_TYPE = 1		Hourly
		            		// Schedule SI_SCHEDULE_TYPE = 2		Daily
		            		// Schedule SI_SCHEDULE_TYPE = 3		Weekly
		            		// Schedule SI_SCHEDULE_TYPE = 4		Monthly 		
		            		// Schedule SI_SCHEDULE_TYPE = 9 		Custom Calendar
		            		int myScheduleInterval = -1;
		            		if (mySchedule.getType()==1) {
		            			myScheduleInterval = mySchedule.getIntervalHours() * 100 + mySchedule.getIntervalMinutes();
		            		} else if (mySchedule.getType()==2) {
		            			myScheduleInterval = mySchedule.getIntervalDays();
		            		} else if (mySchedule.getType()==3) {
			            		
		            		} else if (mySchedule.getType()==4) {
		            			myScheduleInterval = mySchedule.getIntervalMonths();
		            		} else if (mySchedule.getType()==9) {
		            			myScheduleInterval = -999;
		            		} else {
		            			
		            		}
		            		
		            		if (DEBUG) {System.out.println("......reportManager - get report destinations");}
		            		// Get destination
		            		//Destinations myDests = (Destinations) infoObject.getSchedulingInfo().getDestinations();
		            		IDestinations myIDests = infoObject.getSchedulingInfo().getDestinations();
		            		
		            		for (int destLoop=0;destLoop<infoObject.getSchedulingInfo().getDestinations().size();destLoop++) {
		            			IDestination myIDest = (IDestination) myIDests.get(destLoop);
		            			
		            			if (DEBUG) {System.out.println("......reportManager - " + myIDest.properties().getProperty("SI_PROGID").getValue());}
			            				            			
		            			if (myIDest.properties().getProperty("SI_PROGID").getValue().equals("CrystalEnterprise.Smtp")) {
		            				// Email destination
		            				destinationType = "Email";
		            				
		            				// Gets the SMTP InfoObject from the CMS.  Note that the SI_PARENTID will always be 29.
			            			IDestinationPlugin destPlugin = (IDestinationPlugin) iStore.query("select * from ci_systemobjects where si_parentId=29 and si_name='CrystalEnterprise.SMTP' ").get(0); 
		            				myIDest.copyToPlugin(destPlugin); 
		            				ISMTPOptions destOptions = (ISMTPOptions) destPlugin.getScheduleOptions(); 
		            				
		            				destinationEmailFrom = destOptions.getToAddresses().toString(); 
		            				destinationEmailFrom += destOptions.getCCAddresses().toString();
		            						            				
		            				String s = "";
		            				try {
		            					s = destOptions.getSenderAddress().toString(); 
		            				} catch (Exception ex) {
		            					s = "***unknown***";
		            				}
				            		
		            				s = s.replace(',',';'); 
		            				s = s.replace('[',' '); 
		            				s = s.replace(']',' '); 
		            				s = s.trim(); 
		            				destinationEmailTo = s;
		            				
		            				if (DEBUG) {System.out.println("EMAIL OPTIONS - destinationEmailFrom: " + destinationEmailFrom);}
		            				if (DEBUG) {System.out.println("EMAIL OPTIONS - destinationEmailTo: " + destinationEmailTo);}
		            				if (DEBUG) {System.out.println("EMAIL OPTIONS - destinationEmailTo: " + destOptions.getCCAddresses().toString());}
		            				
		            			} else if (myIDest.properties().getProperty("SI_PROGID").getValue().equals("CrystalEnterprise.DiskUnmanaged")) {
		            				// File system destination
		            				destinationType = "File Location";
		            				
		            				// Gets the interface InfoObject from the CMS.  Note that the SI_PARENTID will always be 29.
			            			try {
			            				IDestinationPlugin destPlugin = (IDestinationPlugin) iStore.query("select * from ci_systemobjects where si_parentId=29 and si_name='CrystalEnterprise.DiskUnmanaged' ").get(0);
			            				myIDest.copyToPlugin(destPlugin); 
			            				IDiskUnmanagedOptions destOptions = (IDiskUnmanagedOptions) destPlugin.getScheduleOptions(); 
			            				destinationFileName = destOptions.getUserName().toString();
			            				
			            				destinationFileLocation = destOptions.getDestinationFiles().get(0).toString();
			            			} catch (Exception ex) {
			            				// Special case - DiskUnmanaged selected, but no file location specified
			            				// System.out.println("Could not get interface for DiskUnmanaged :: " + ex.getMessage());
			            				destinationFileLocation = "NOT DEFINED";
			            				destinationFileName = "NOT DEFINED";
			            			}
		            				
		            				
		            				
		            			} else {
		            				destinationType = myIDest.properties().getProperty("SI_PROGID").getValue().toString();
		            			}
		            		}
		            		
		            		Long myReportSize = Long.parseLong("0");
		            		try {
		            			IProperties props=(IProperties)infoObject.properties().getProperty("SI_FILES").getValue();            			
		            			myReportSize = Long.parseLong(props.getProperty("SI_VALUE1").toString());		            				            			
		            		} catch (Exception ex) {
		            			//System.out.println("ERROR Getting FileInfo -> " + ex.getMessage());
		            		}
		            		
		            		// Get parent report
		            		if (DEBUG) {System.out.println("...... reportManager - getting reportmanager for parent report -> " + infoObject.getParentID());}
		            		reportManager parentReport = new reportManager(iStore);
		            		parentReport.getReportFromCUID(infoObject.getParentCUID());
		            		
		            		if (DEBUG) {System.out.println("...... reportManager - PARENT REPORT :: " + parentReport.getReportTitle(0) + " : Parent Folder CUID: " + parentReport.getParentReportCUID(0));}
		            		
		            		// Get the folder CUID of the report
		            		String myParentFolderCUID = infoObject.getParentCUID();
		            		String folderPath = "";
		            		boolean parentIsFolder = false;
		            		
		            		while (!parentIsFolder) {
		            			// Check to make sure that the parent is a type FOLDER
			            		String getParentQuery = getReportFromCUID.replace("{CUID}", myParentFolderCUID).replace("{fields}",  "*");
			            		if (DEBUG) {System.out.println(" ... Checking to see if object is folder => " + myParentFolderCUID);}
			            		if (DEBUG) {System.out.println(" ...       " + getParentQuery);}
			            		
			            		IInfoObject myParentObject = (IInfoObject)iStore.query(getParentQuery).get(0);
			            		if (DEBUG) {System.out.println(" ... .... Object is a => " + myParentObject.getKind());}
			            		
			            		if (myParentObject.getKind().equals("Folder") || myParentObject.getKind().equals("FavoritesFolder")) {
			            			parentIsFolder = true;
			            			
			            		} else {
			            			myParentFolderCUID = myParentObject.getParentCUID();
			            		}
			            		
		            		}
		            		
		            		// Get complete path from parent report's folder
			            	if (DEBUG) {System.out.println("===== reportManager - getting foldermanager for parent folder :: " + myParentFolderCUID);}
			            	
			            	folderManager parentFolder = new folderManager(iStore);   
		            		try {
				            	parentFolder.getFolderByCUID(myParentFolderCUID);
				            	folderPath = parentFolder.getFolderCompletePath();
				            } catch (Exception EX) {
				            	if (DEBUG) {System.out.println("***** ERROR GETTING FOLDER PATH ******");}
				            	folderPath = "UNKNOWN";
				            }
			            				            	
		            		if (DEBUG) {System.out.println("***** " + myObjectList.size() + "  : " + infoObject.getTitle() + " : " + infoObject.getParentCUID());}
		            		
		            		// Add the infoobject to the object list
		            		if (DEBUG) {System.out.println(folderPath + "\t" + infoObject.getCUID() + "\t" + infoObject.getTitle());}
		            		report myReport = new report(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle(), infoObject.getParentCUID(), parentReport.getReportTitle(0), folderPath, lastUpdatedDate, lastScheduledDate, creationDate, nextRunTime, mySchedule.getEndDate(), outputFormat, owner, mySchedule.getType(), myScheduleInterval, destinationType, destinationEmailFrom, destinationEmailTo, destinationFileLocation, destinationFileName);
		            		myReport.setReportSize(myReportSize);
		            		myReport.setObjectType(infoObject.getKind());

		            		myObjectList.add(myReport);
		            		
		            	}
		            }
		            catch (Exception ex) {
		            	if (true) {System.out.println("... reportManager GENERAL ERROR (adding objects to objectList) " + ex.getClass() + " : " + ex.getMessage());}
		            }
		        }
		    	if (DEBUG) {System.out.println("... reportManager has " + Integer.toString(myObjectList.size()) + " objects");}
		    }
	    }
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... reportManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... reportManager GENERAL ERROR " + ex.getMessage());}
	    }
	    
	}
	
	private IInfoObject getOwnerInfoObject(String myOwnerName) {
		IInfoObjects resultsOwner;
		IInfoObject myOwner;
		String myQuery_getOwner = "SELECT * from CI_SystemObjects where SI_Name='" + myOwnerName + "' and SI_KIND='User'";
		
		try
		{
			resultsOwner = iStore.query(myQuery_getOwner);
			if (resultsOwner.isEmpty() || resultsOwner.size()>1){
				// An exception has been caught - either the OWNER does not exist, or multiple values have been returned
				myOwner = null;
			} else {
				myOwner = (IInfoObject)resultsOwner.get(0);
			}
		} catch (SDKException ex) {
			myOwner = null;
			if (DEBUG) {System.out.println("         *** ERROR *** :: getOwnerInfoObject SDK Exception " + myOwnerName);}
		}
		return myOwner;
	}
	
	private void changeOwnerCore(IInfoObject myReport, IInfoObject myNewOwner) {
		String oldOwnerID;
		
		try
	    {
		    if (DEBUG) {System.out.println("      Have InfoObject: " + myReport.getTitle());}
		    oldOwnerID = myReport.properties().getProperty("SI_OWNERID").getValue().toString();
		           			            
		    if (DEBUG) {System.out.println("      Current Owner: (" + oldOwnerID + " : " + myReport.properties().getProperty("SI_OWNER").getValue().toString() + ")  changing to (" + myNewOwner.getID() + " : " + myNewOwner.getTitle() + ")");}
		            
		    myReport.properties().setProperty(CePropertyID.SI_OWNERID, myNewOwner.getID());
		            
		    // Change the submitter if it is an instance 
		    if (myReport.isInstance()){ 
		    	ISchedulingInfo schedInfo = myReport.getSchedulingInfo(); 
		        schedInfo.properties().setProperty(CePropertyID.SI_SUBMITTERID, myNewOwner.getID()); 
		    } 	            
		    myReport.save();     	
	    } 
	    	    
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... changeOwnerCore SDK EXCEPTION " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... changeOwnerCore GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void changeOwner(String myReportCUID, String myNewOwner, int checkForInstances) {
		// Perform the query.
	    IInfoObject myOwner;
	    String myQuery_getReport, myQuery_getReportInstances;
	    
	    myQuery_getReport = "SELECT * from CI_InfoObjects where SI_CUID='" + myReportCUID + "'";
	    myQuery_getReportInstances = "SELECT * from CI_InfoObjects where SI_PARENT_CUID='{prompt1}'";
	    
	    try
	    {
	    	// Fetch owner object
	    	myOwner = getOwnerInfoObject(myNewOwner);
	    	if (DEBUG) {System.out.println("...... Change Owner To :: " + myOwner.getTitle());}
	    	
	    	IInfoObjects results = iStore.query(myQuery_getReport);
		    	
		    if (!results.isEmpty())
		    {
		    	// For each InfoObject, change the owner
		    	for(int i = 0; i < results.size(); i++)
		    	{
		    		IInfoObject myReport = (IInfoObject)results.get(i);
		    		changeOwnerCore(myReport, myOwner);
		    		// Check to see if the report has any instances
		    		if (checkForInstances==1) {
		    			if (DEBUG) {System.out.println("       check for instances");}
			    		myQuery_getReportInstances = myQuery_getReportInstances.replace("{prompt1}", myReport.getCUID());
		    			if (DEBUG) {System.out.println("       " + myQuery_getReportInstances);}
		    			
		    			IInfoObjects result_Instances = iStore.query(myQuery_getReportInstances);
		    			if (!result_Instances.isEmpty()) {
		    				for(int instances = 0; instances < result_Instances.size(); instances++)
		    		    	{
		    					IInfoObject myReportInstance = (IInfoObject)result_Instances.get(instances);
		    		    		changeOwnerCore(myReportInstance, myOwner);
		    		    	}
		    			}
		    		}
		           	
		       	}
		    } else {
		    	if (DEBUG) {System.out.println("         *** ERROR *** :: No reports found for CUID " + myReportCUID);}
		    }
		    	
		    // Commit the changed InfoObjects group to CMS
		    System.out.print("Saving... " );
		    iStore.commit(results); 
		    System.out.println("Complete");
		    
	    } 
	    	    
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... changeOwner SDK EXCEPTION " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... changeOwner GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void deleteObject(String myReportCUID) {
		// Perform the query.
	    String myQuery_getReport;
	    
	    myQuery_getReport = "SELECT TOP 1 * from CI_InfoObjects where SI_CUID='" + myReportCUID + "'";
	    
	    try
	    {
	    	if (DEBUG) {System.out.println("...... Getting InfoObject to Delete");}
	    	IInfoObjects results = iStore.query(myQuery_getReport);
		    	
		    if (!results.isEmpty())
		    {
		    	// Delete the infoObject
		    	IInfoObject myReport = (IInfoObject)results.get(0);
		    	if (DEBUG) {System.out.println("............ " + myReport.getTitle() + " : " + myReport.getCUID());}
		    	myReport.deleteNow();
		    } else {
		    	if (DEBUG) {System.out.println("         *** ERROR *** :: No reports found for CUID " + myReportCUID);}
		    }
		    	
		    // Commit the changed InfoObjects group to CMS
		    System.out.print("Saving... " );
		    iStore.commit(results); 
		    System.out.println("Complete");
	    } 
	    	    
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... deleteInstance SDK EXCEPTION " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... deleteInstance GENERAL ERROR " + ex.getMessage());}
	    }
		
	}
	
	public void getOldInstancesList(String myTimeStamp) {
		// Perform the query.
	    String myQuery_getOldReportInstances;
	    
	    myQuery_getOldReportInstances = "SELECT * FROM CI_InfoObjects where SI_INSTANCE=1 and SI_UPDATE_TS<='" + myTimeStamp + "'";
	    myFields.add("SI_UPDATE_TS");
		
	    try
	    {
	    	this.getObjectList("report", myQuery_getOldReportInstances, myFields);
		} catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... getOldInstancesList GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public Integer size() {
		try {
			return (Integer)myObjectList.size();
		} catch (Exception ex) {
			if (DEBUG) {System.out.println("... reportManager GENERAL ERROR (size)" + ex.getMessage());}
			return -999;
		}
	}
	
	public int writeRecurringJobsToDatabase(Connection myConn, reportManager rm) {
		int myRecords = 0;
		int myInserts = 0;
		
		if (DEBUG) {System.out.println("Starting Write Recurring Jobs to Database");}
		
		for (int myLoop=0;myLoop<rm.size();myLoop++) {
			myRecords = 0;
			if (DEBUG) {System.out.println("Object: " + myLoop + " : " + rm.getReportTitle(myLoop));}
			try {
				DateFormat df2 = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
				
				// Delete any existing record based on the report CUID
				PreparedStatement psExisting = myConn.prepareStatement(mySQLDeleteRecurring);
				psExisting.setString(1, rm.getReportCUID(myLoop));
				try {
					psExisting.executeQuery();
				} catch (Exception ex) {
					// System.out.println("No records to delete");					
				}
				
				if (DEBUG_showQuery) {System.out.println("INSERTING: " + mySQLInsertRecurring);}
				PreparedStatement psInsert = myConn.prepareStatement(mySQLInsertRecurring);
				psInsert.setString(1, rm.getReportCUID(myLoop));
				psInsert.setInt(2, rm.getReportID(myLoop));
				psInsert.setString(3, rm.getReportTitle(myLoop));
				psInsert.setString(4, rm.getFolderPath(myLoop));
				psInsert.setString(5, rm.getParentReportName(myLoop));
				psInsert.setString(6, rm.getParentReportCUID(myLoop));
				// Dates
				psInsert.setString(7, new StringBuilder( df2.format(rm.getReporCreationDate(myLoop))).toString());
				psInsert.setString(8, new StringBuilder( df2.format(rm.getNextRunTime(myLoop))).toString());
				psInsert.setString(9, new StringBuilder( df2.format(rm.getEnddate(myLoop))).toString());
					
				// Scheduling Info
				psInsert.setString(10, rm.getOutputFormat(myLoop));
				psInsert.setString(11, rm.getReportOwner(myLoop));
				psInsert.setInt(12, rm.getScheduleType(myLoop));
				psInsert.setInt(13, rm.getScheduleInterval(myLoop));	
				psInsert.setString(14, rm.getDestinationType(myLoop));
				psInsert.setString(15, rm.getDestinationEmailFrom(myLoop).trim());
				String strToEmail = rm.getDestinationEmailTo(myLoop).replace(" ", "");
				int nToLength = strToEmail.length();
				if (nToLength>1200) {nToLength = 1200;}
				psInsert.setString(16, rm.getDestinationEmailTo(myLoop).replace(" ", "").substring(0, nToLength));
				psInsert.setString(17, rm.getDestinationFileLocation(myLoop));
				psInsert.setString(18, rm.getDestinationFileName(myLoop));
				
				myRecords = psInsert.executeUpdate();
				if (DEBUG) {System.out.println("       " + rm.getReportTitle(myLoop) + " : " + myRecords + " records inserted");}
					myInserts = myInserts+1;
				}
			 
			catch (Exception ex) {
				if (true) {System.out.println("*** ERROR 10 " + ex.getMessage());}
				if (true) {System.out.println("             " + rm.getReportCUID(myLoop) + " : " + rm.getReportTitle(myLoop) + " : " + rm.getFolderPath(myLoop) + " : " + rm.getReportOwner(myLoop) + " : " + rm.getScheduleType(myLoop) + " : " + rm.getScheduleInterval(myLoop));}
				if (true) {System.out.println("             " + rm.getEnddate(myLoop) + " : " + rm.getNextRunTime(myLoop));}
			}	
		}
		if (DEBUG) {System.out.println("Inserts: " + myInserts);}

		return myInserts*10000;
	}
	
	public int writeReportDocumentationToDatabase(Connection myConn) {
		int myRecords = 0;
		int myInserts = 0;
		int myUpdates = 0;
		
		if (DEBUG) {System.out.println("+++ Write Report Documentation" );}
		for (int myLoop=0;myLoop<myObjectList.size();myLoop++) {
			try {
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
				
				// Fetch existing record
				PreparedStatement psExisting = myConn.prepareStatement(mySQLSelect_reportMaster);
				psExisting.setString(1, myObjectList.get(myLoop).getReportCUID());
				if (DEBUG_showQuery) {System.out.println("STATEMENT: " + mySQLSelect_reportMaster + " : " + myObjectList.get(myLoop).getReportName() + " : " + myObjectList.get(myLoop).getReportCUID() + " : " +  myObjectList.get(myLoop).getReportLastUpdatedDate()+ " : " + myLoop);}	   
				
				ResultSet rs = psExisting.executeQuery();
				if (rs.next()) {
					// An existing recurring job was found
					if (DEBUG) {System.out.println("Found Existing Report");}
					
					// Check to see if the LastUpdated field matches the report's SI_UPDATE_TS
					String rsDate = df2.format(rs.getTimestamp("LastUpdated"));
					String reportDate = df2.format(myObjectList.get(myLoop).getReportLastUpdatedDate());
					if (DEBUG) {System.out.println("Comparing dates: " + rsDate + " : " + reportDate);}
					
					if (rsDate.equals(reportDate)) {
						if (DEBUG) {System.out.println("              DATE MATCH!");}
						// LastUpdated date matches SI_UPDATE_TS - so just update the DateUpdated field
						PreparedStatement ps = myConn.prepareStatement(mySQLUpdate_reportMaster_Matching);
						ps.setString(1, myObjectList.get(myLoop).getReportCUID());
											
						if (DEBUG_showQuery) {System.out.println("STATEMENT: " + mySQLUpdate_reportMaster_Matching);}	   
						ps.executeUpdate();
					} else {
						if (DEBUG) {System.out.println("              ==== DATE NOT MATCH!");}
						PreparedStatement ps = myConn.prepareStatement(mySQLUpdate_reportMaster_Updated);
						ps.setString(1, reportDate);
						ps.setString(2, myObjectList.get(myLoop).getReportCUID());
											
						if (DEBUG_showQuery) {System.out.println("STATEMENT: " + mySQLUpdate_reportMaster_Updated);}	   
						ps.executeUpdate();
						
						// Process updates to DataSources
						if (DEBUG) {System.out.println(".... Getting data sources - " + myObjectList.get(myLoop).getNumberOfReportConnections());}	 
						for (int myDataSourceLoop=0;myDataSourceLoop<myObjectList.get(myLoop).getNumberOfReportConnections();myDataSourceLoop++) {
							if (DEBUG) {System.out.println("........." + myObjectList.get(myLoop).getReportConnectionName(myDataSourceLoop) + " : " + myObjectList.get(myLoop).getReportCUID() + " : " + reportDate);}
							PreparedStatement psDataSource = myConn.prepareStatement(mySQLInsert_reportMaster_insertDataSources);
							psDataSource.setString(1, myObjectList.get(myLoop).getReportCUID());
							psDataSource.setString(2, myObjectList.get(myLoop).getReportConnectionName(myDataSourceLoop));
							psDataSource.setString(3, myObjectList.get(myLoop).getReportConnectionType(myDataSourceLoop));
							psDataSource.setString(4, reportDate);
							psDataSource.executeUpdate();
						}
						
						// Process updates to DataTables
						if (DEBUG) {System.out.println(".... Getting data tables - " + myObjectList.get(myLoop).getReportNumberOfTables());}	 
						for (int myTableLoop=0;myTableLoop<myObjectList.get(myLoop).getReportNumberOfTables();myTableLoop++) {
							if (DEBUG) {System.out.println("........." + myObjectList.get(myLoop).getReportTableName(myTableLoop));}
							PreparedStatement psDataSourceTables = myConn.prepareStatement(mySQLInsert_reportMaster_insertTableName);
							psDataSourceTables.setString(1, myObjectList.get(myLoop).getReportCUID());
							psDataSourceTables.setString(2, myObjectList.get(myLoop).getReportTableName(myTableLoop));
							psDataSourceTables.setString(3, reportDate);
							psDataSourceTables.executeUpdate();
						}
						
						myUpdates = myUpdates + 1;
					}
					
				} else {
					// No existing report documented - insert report master information
					PreparedStatement psInsert = myConn.prepareStatement(mySQLInsert_reportMaster);
					psInsert.setString(1, myObjectList.get(myLoop).getReportCUID());
					psInsert.setString(2, myObjectList.get(myLoop).getReportName());
					psInsert.setString(3, myObjectList.get(myLoop).getFolderPath());
					psInsert.setString(4, myObjectList.get(myLoop).getReportType());
					
					// Dates
					psInsert.setString(5, new StringBuilder( df2.format(myObjectList.get(myLoop).getReportLastUpdatedDate())).toString());
					
					if (DEBUG_showQuery) {System.out.println("STATEMENT: " + mySQLInsert_reportMaster);}
					if (DEBUG) {System.out.println("           " + myObjectList.get(myLoop).getReportCUID() + " : " + myObjectList.get(myLoop).getReportName() + " : " + myObjectList.get(myLoop).getFolderPath() + " : " + df2.format(myObjectList.get(myLoop).getReportLastUpdatedDate()));}
					myRecords = psInsert.executeUpdate();
					if (DEBUG) {System.out.println("       " + myObjectList.get(myLoop).getReportName() + " : " + myRecords + " records inserted");}
					
					// Insert data source information
					String reportDate = df2.format(myObjectList.get(myLoop).getReportLastUpdatedDate());
					if (DEBUG) {System.out.println(".... Getting data sources - " + myObjectList.get(myLoop).getNumberOfReportConnections());}	 
					for (int myDataSourceLoop=0;myDataSourceLoop<myObjectList.get(myLoop).getNumberOfReportConnections();myDataSourceLoop++) {
						if (DEBUG) {System.out.println("........." + myObjectList.get(myLoop).getReportConnectionName(myDataSourceLoop));}
						PreparedStatement psDataSource = myConn.prepareStatement(mySQLInsert_reportMaster_insertDataSources);
						psDataSource.setString(1, myObjectList.get(myLoop).getReportCUID());
						psDataSource.setString(2, myObjectList.get(myLoop).getReportConnectionName(myDataSourceLoop));
						psDataSource.setString(3, myObjectList.get(myLoop).getReportConnectionType(myDataSourceLoop));
						psDataSource.setString(4, reportDate);
						psDataSource.executeUpdate();
					}
									
					// Insert table information (if exists)
					if (DEBUG) {System.out.println(".... Getting data tables - " + myObjectList.get(myLoop).getReportNumberOfTables());}	 
					for (int myTableLoop=0;myTableLoop<myObjectList.get(myLoop).getReportNumberOfTables();myTableLoop++) {
						if (DEBUG) {System.out.println("........." + myObjectList.get(myLoop).getReportTableName(myTableLoop));}
						PreparedStatement psDataSourceTables = myConn.prepareStatement(mySQLInsert_reportMaster_insertTableName);
						psDataSourceTables.setString(1, myObjectList.get(myLoop).getReportCUID());
						psDataSourceTables.setString(2, myObjectList.get(myLoop).getReportTableName(myTableLoop));
						psDataSourceTables.setString(3, reportDate);
						psDataSourceTables.executeUpdate();
					}		
					myInserts = myInserts+1;
				}
			} catch (Exception ex) {
				if (true) {System.out.println("*** ERROR 10 " + ex.getMessage());}
				if (true) {System.out.println("             " + myObjectList.get(myLoop).getReportCUID() + " : " + myObjectList.get(myLoop).getReportName() + " : " + myObjectList.get(myLoop).getFolderPath());}
			}
		}
		return myRecords;
	}

	public String getMissingRecurringJobs(Connection myConn) {
		String missingJobs = "";
		
		String messageHeader;
		String messageRow;
		String messageFooter;
		
		int missingJobCount = 0;
		
		messageHeader = "<table border=1 style='font-size:10pt;font-family:verdana;'>\n";
		messageHeader += "<th>Folder</th><th>Parent Report</th><th>Owner</th><th>Last Updated</th><th>Scheduled End Date</th><th>Report ID</th>\n";
		messageFooter = "</table>\n";
		
		try {
			// Fetch existing record
			PreparedStatement psExisting = myConn.prepareStatement(mySQLMissingRecurring);
			if (DEBUG) {System.out.println("Getting missing jobs");}	   
			
			ResultSet rs = psExisting.executeQuery();
			
			missingJobs = messageHeader;
			
			while (rs.next()) {
				messageRow = "";
				messageRow += "\t";
				messageRow += "<tr>";
				messageRow += "<td>" + rs.getString("folderPath") + "</td>";
				messageRow += "<td>" + rs.getString("parentReport") + "</td>";
				messageRow += "<td>" + rs.getString("owner") + "</td>";
				messageRow += "<td>" + rs.getString("updateDate") + "</td>";
				messageRow += "<td>" + rs.getString("endDate") + "</td>";
				messageRow += "<td>" + rs.getString("reportCUID") + "</td>";
				messageRow += "</tr>";
				messageRow += "\n";
				missingJobs += messageRow;
				missingJobCount = missingJobCount + 1;
				
				rs.next();
			}
			missingJobs += messageFooter;
			
			if (missingJobCount==0) {
				missingJobs = "No missing jobs found.";
			} else {
				purgeMissingRecurringJobs(myConn);
			}
			
		} catch (Exception ex) {
			if (DEBUG) {System.out.println("... EXCEPTION " + ex.getMessage());}	
			missingJobs = "ERROR " + ex.getMessage();
		}
		
		return missingJobs;
		
	}
	
	public void purgeMissingRecurringJobs(Connection myConn) {
		String myQuery = mySQLPurgeOldRecurring;
		
		if (DEBUG) {System.out.println("... Report Manager... purging deleted recurring jobs :: " + myQuery);}
		
		try {
			// Fetch existing record
			PreparedStatement psPurgeMissingRecurringJobs = myConn.prepareStatement(myQuery);
			if (DEBUG) {System.out.println("Purge missing jobs");}	   
			
			psPurgeMissingRecurringJobs.execute();

		} catch (Exception ex) {
			if (DEBUG) {System.out.println("... EXCEPTION " + ex.getMessage());}	
		}
		
	}
	
	public void catalogAllRecurringReportObjects(String myRecipient) {
		if (DEBUG) {System.out.println("... Report Manager... catalog all recurring reports");}
		
		String myQuery = getAllRecurringReports;

		if (DEBUG) {System.out.println("... Report Manager... query :: " + myQuery);}
		
		this.getObjectList("CUID", myQuery, myFields);
		if (DEBUG) {System.out.println("... Report Manager... recurring objects fetched - " + this.size() + " objects retrieved");}
		
		connectionManager myConn = new connectionManager();
		reportManager rm;
		
		try {
			for (int i=0;i<myObjectList.size();i++) {
				if (DEBUG) {System.out.println("... Report Manager... attempting to catalog report - " + myObjectList.get(i).getReportCUID() + " - " + myObjectList.get(i).getReportName());}
				
				rm = new reportManager(iStore);
				rm.getRecurringReportFromCUID(myObjectList.get(i).getReportCUID());
				rm.writeRecurringJobsToDatabase(myConn.getDataConnection(), rm);
			}
		} catch (Exception ex) {
			System.out.println("... Report Manager... ERROR... cannot get recurring report information " + ex.getMessage());
		}
		
		String missingReport = getMissingRecurringJobs(myConn.getDataConnection());
		com.idex.businessObjects.emailManager.emailManager.sendmessage(myRecipient, "** BUSINESS OBJECT XI - MISSING JOB ALERT ***", missingReport);
		
		myConn.closeDataConnection();
		
	}

	public void getFailedInstances(String myDateParam) {
		if (DEBUG) {System.out.println("... Report Manager... get failed instances");}
				
		String myQuery = getFailedInstances.replace("{date}", myDateParam);
		if (true) {System.out.println("... Report Manager... query :: " + myQuery);}
		
		this.getObjectList("recurringInstance", myQuery, myFields);
	}
	
	public void rescheduleInstances (String myCUID) {
		// Perform the query.
	    IInfoObjects results;
	    String myQuery = getReportFromCUID.replace("{fields}",  "*").replace("{CUID}", myCUID);
	    
	    try {
	    	results = iStore.query(myQuery);
	    	
	    	if (results.isEmpty()) {
		    	// Return an informative message if there are no sub-folders.
	        	System.out.println("No objects found matching these specifications");
	        	
	    	} else {
	    		IInfoObject infoObject = (IInfoObject)results.get(0);
	    		ISchedulingInfo mySchedule = infoObject.getSchedulingInfo();
	    		// Schedule SI_SCHEDULE_TYPE = 1		Hourly
	    		if (mySchedule.getType()==1) {
        			// SI_SCHEDULE_TYPE = Hourly... do not reschedule this report
	    			System.out.println("HOURLY REPORT... this report will not be rescheduled.");
        		} else {
        			try {
        	    	    // Get the user ID of the SI_OWNER
        				int myOwnerID = Integer.parseInt(infoObject.properties().getProperty("SI_OWNERID").getValue().toString());
        				mySchedule.setScheduleOnBehalfOf(myOwnerID);
        			} catch (Exception ex) {
    	    	    	// Do nothing... the report can still be rescheduled 
    	    	    }	
        			try {
        				mySchedule.setType(CeScheduleType.ONCE);
        				mySchedule.setRightNow(true);
        				iStore.schedule(results);
        			} catch (Exception ex) {
    	    	    	
    	    	    }
        		}
	    	}
	    } catch (Exception ex) {
	    	
	    }
	}

	
}
