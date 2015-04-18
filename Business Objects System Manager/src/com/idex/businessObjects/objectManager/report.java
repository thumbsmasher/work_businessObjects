package com.idex.businessObjects.objectManager;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class report {
	private String reportCUID;
	private int reportID;
	private String reportName;
	private String parentFolderCUID;
	private String parentReportCUID;
	private String parentReportName;
	private String folderPath;
	private String reportType;
	private String objectType;
	private Long reportSize;
	private Date reportCreationDate;
	private Date reportLastUpdatedDate;
	private Date reportLastScheduledDate;
	private Date reportNextRunDate;
	private Date reportEndDate;
	private String scheduledOutputFormat;
	private String owner;
	private String reportInputFRSPath;
	private int scheduleType; 
	private int scheduleInterval;
	private List<reportConnection> reportDataConnections;
	private List<String> reportTableList;
	private String destinationType;
	private String destinationEmailFrom;
	private String destinationEmailTo;
	private String destinationFileLocation;
	private String destinationFileName;
	
	@SuppressWarnings("unused")
	private report() {
		//Default constructor
		
	}
	
	public report(String myCUID, int myID, String myName, String myparentCUID) {
		// Basic report object
		this.setReportCUID(myCUID);
		this.setReportID(myID);
		this.setReportName(myName);
		this.setParentFolderCUID(myparentCUID);
	}
	
	public report(String myCUID, int myID, String myName, String myparentCUID, String myFolderPath, String owner, String myReportKind, Date myLastUpdatedDate) {
		this.setReportCUID(myCUID);
		this.setReportID(myID);
		this.setReportName(myName);
		this.setParentReportCUID(myparentCUID);	
		this.setFolderPath(myFolderPath);
		this.setReportKind(myReportKind);
		this.setOwner(owner);
		this.setReportLastUpdatedDate(myLastUpdatedDate);
		DateFormat df = new SimpleDateFormat("yyyy/mm/dd");
	    try {
			this.setReportLastScheduledDate(df.parse("1901/01/01"));
		} catch (ParseException e) {
			
		}
		this.reportInputFRSPath = "Not set";
		this.reportDataConnections = new ArrayList<reportConnection>();
		this.reportTableList = new ArrayList<String>();
		this.reportSize = Long.getLong("0");
	}
		
	public report(String myCUID, int myID, String myName, String myparentCUID, String myparentReportName, String myFolderPath, Date LastUpdatedDate, Date LastScheduledDate, Date creationDate, Date nextRunTime, Date endDate, String outputFormat, String owner, int scheduleType, int scheduleInterval, String destinationType, String emailTo, String emailFrom, String fileLocation, String fileName) {
		// Recurring Instance
		this.setReportCUID(myCUID);
		this.setReportID(myID);
		this.setReportName(myName);
		this.setParentReportCUID(myparentCUID);	
		this.setParentReportName(myparentReportName);
		this.setFolderPath(myFolderPath);
		this.setReportLastUpdatedDate(LastUpdatedDate);
		this.setReportLastScheduledDate(LastScheduledDate);
		this.setCreationDate(creationDate);
		this.setNextRunTime(nextRunTime);
		this.setEndDate(endDate);
		this.setoutputFormat(outputFormat);
		this.setOwner(owner);
		this.setScheduleType(scheduleType);
		this.setScheduleInterval(scheduleInterval);
		this.setDestinationType(destinationType);
		this.setDestinationEmailFrom(emailFrom);
		this.setDestinationEmailTo(emailTo);
		this.setDestinationFileLocation(fileLocation);
		this.setDestinationFileName(fileName);
		this.reportSize = Long.getLong("0");
	}
	
	private void setDestinationFileName(String myString) {
		// TODO Auto-generated method stub
		this.destinationFileName = myString;
	}
	public String getDestinationFileName() {
		return this.destinationFileName;
	}

	private void setDestinationFileLocation(String myString) {
		// TODO Auto-generated method stub
		this.destinationFileLocation = myString;
	}
	public String getDestinationFileLocation() {
		return this.destinationFileLocation;
	}	

	private void setDestinationEmailTo(String myString) {
		// TODO Auto-generated method stub
		this.destinationEmailTo = myString;
	}
	public String getDestinationEmailTo() {
		return this.destinationEmailTo;
	}

	private void setDestinationEmailFrom(String myString) {
		// TODO Auto-generated method stub
		this.destinationEmailFrom = myString;
	}
	public String getDestinationEmailFrom() {
		return this.destinationEmailFrom;
	}

	private void setDestinationType(String myString) {
		// TODO Auto-generated method stub
		this.destinationType = myString;
	}
	public String getDestinationType() {
		return this.destinationType;
	}

	private void setScheduleInterval(int myInt) {
		this.scheduleInterval = myInt;
	}
	public int getScheduleInterval() {
		return this.scheduleInterval;
	}
	
	private void setScheduleType(int myInt) {
		this.scheduleType = myInt;
	}
	public int getScheduleType() {
		return this.scheduleType;
	}
	
	private void setOwner(String myString) {
		this.owner = myString;
	}
	public String getOwner() {
		return this.owner;
	}
	private void setReportKind(String myString) {
		this.reportType = myString;
	}
	public String getReportType() {
		return this.reportType;
	}
	public void setObjectType(String myString) {
		this.objectType = myString;
	}
	public String getObjectType() {
		return this.objectType;
	}
	
	private void setoutputFormat(String myString) {
		this.scheduledOutputFormat = myString;
	}
	public String getOutputFormat() {
		return this.scheduledOutputFormat;
	}
	
	private void setEndDate(Date myDate) {
		this.reportEndDate = myDate;
	}
	public Date getEnddate() {
		return this.reportEndDate;
	}
	private void setReportLastUpdatedDate(Date myDate) {
		this.reportLastUpdatedDate = myDate;
	}
	public Date getReportLastUpdatedDate() {
		return this.reportLastUpdatedDate;
	}
	public void setReportLastScheduledDate(Date myDate) {
		this.reportLastScheduledDate = myDate;
	}
	public Date getReportLastScheduledDate() {
		return this.reportLastScheduledDate;
	}
	private void setNextRunTime(Date myDate) {
		this.reportNextRunDate = myDate;
	}
	public Date getNextRunTime() {
		return this.reportNextRunDate;
	}
	
	private void setFolderPath(String myString) {
		this.folderPath = myString;
	}
	public String getFolderPath() {
		return this.folderPath;
	}
	public void setReportSize(Long myValue) {
		this.reportSize = myValue;
	}
	public Long getReportSize() {
		return this.reportSize;
	}
	// Getter and Setter for folderID
	private void setReportCUID(String myString) {
		this.reportCUID = myString;
	}
	public String getReportCUID() {
		return reportCUID;
	}
	
	//Getter and Setter for folderName
	private void setReportName(String myString) {
		this.reportName = myString;
	}
	public String getReportName() {
		return reportName;
	}
	
	//Getter and Setter for report ID
	private void setReportID(int myInt) {
		this.reportID = myInt;
	}
	public int getReportID() {
		return reportID;
	}
	
	//Getter and Setter for creation date
	public void setCreationDate(Date myDate) {
		this.reportCreationDate = myDate;
	}
	public Date getCreationDate() {
		return reportCreationDate;
	}
	//Getter and Setter for parentID
	private void setParentFolderCUID(String myString) {
		this.parentFolderCUID = myString;
	}
	public String getParentFolderCUID() {
		return parentFolderCUID;
	}
	//Getter and Setter for parent CUID
	private void setParentReportCUID(String myString) {
		this.parentReportCUID = myString;
	}
	public String getParentReportCUID() {
		return parentReportCUID;
	}
	//Getter and Setter for parent report name
	private void setParentReportName(String myString) {
		this.parentReportName = myString;
	}
	public String getParentReportName() {
		return parentReportName;
	}
	//Add data connection 
	public void addDataConnection(int myConnectionProperty, int myConnectionType, String myConnectionName, String myConnectionDatabase, String myConnectionUserName) {
		reportConnection myRC = new reportConnection(myConnectionProperty, myConnectionType, myConnectionName, myConnectionDatabase, myConnectionUserName);
		reportDataConnections.add(myRC);
	}
	public int getNumberOfReportConnections() {
		return this.reportDataConnections.size();
	}
	public String getReportConnectionName(int myIndex) {
		return reportDataConnections.get(myIndex).getConnectionName();
	}
	public String getReportDatabaseName(int myIndex) {
		return reportDataConnections.get(myIndex).getConnectionDatabase();
	}
	public String getReportConnectionType(int myIndex) {
		return Integer.toString(reportDataConnections.get(myIndex).getConnectionType());
	}
	
	//Add table name
	public void addDataTable(String myTableName) {
		reportTableList.add(myTableName);
	}
	public int getReportNumberOfTables() {
		return this.reportTableList.size();
	}
	public String getReportTableName(int myIndex) {
		return this.reportTableList.get(myIndex);
	}
	
	// Set input FRS path
	public void setInputFRSPath(String myInputFRSPath) {
		this.reportInputFRSPath = myInputFRSPath;
	}
	public String getInputFRSPath() {
		return this.reportInputFRSPath;
	}
	
	
}
