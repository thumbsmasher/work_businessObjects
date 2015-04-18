package com.idex.businessObjects.objectManager;

import java.util.Date;
	
public class user {
	
	private String userCUID;
	private int userID;
	private String userName;
	private String userFullName;
	private String userEmail;
	private String userLicenseType;
	private String userLocation;
	private String userCompany;
	
	private Date userLastLogonDate;
	private Date userLastUpdatedDate;
	
	public user() {
		// Default constructor
		
	}
	
	public user(String myCUID, int myID, String myUserName, String myFullName, String myEmail, String myLicense, String myLocation, String myCompany, Date myLastLogon, Date myLastUpdated) {
		this.userCUID = myCUID;
		this.userID = myID;
		this.userName = myUserName;
		this.userFullName = myFullName;
		this.userEmail = myEmail;
		this.userLicenseType = myLicense;
		this.userLocation = myLocation;
		this.userCompany = myCompany;
		this.userLastLogonDate = myLastLogon;
		this.userLastUpdatedDate = myLastUpdated;
	}
	
	public user(String myCUID, int myID, String myUserName, Date myLastLogon, Date myLastUpdated) {
		this.userCUID = myCUID;
		this.userID = myID;
		this.userName = myUserName;
		this.userFullName = "";
		this.userEmail = "";
		this.userLicenseType = "";
		this.userLocation = "";
		this.userCompany = "";
		this.userLastLogonDate = myLastLogon;
		this.userLastUpdatedDate = myLastUpdated;
	}
	
	public String getUserCUID() {
		return this.userCUID;
	}
	public int getUserID() {
		return this.userID;
	}
	public String getUserName() {
		return this.userName;
	}
	public String getUserFullName() {
		return this.userFullName;
	}
	public String getUserEmail() {
		return this.userEmail;
	}
	public Date getLastLogonDate() {
		return this.userLastLogonDate;
	}
	public Date getLastUpdatedDate() {
		return this.userLastUpdatedDate;
	}
	public String getLicenseType() {
		return this.userLicenseType;
	}
	public String getUserLocation() {
		return this.userLocation;
	}
	public String getUserCompany() {
		return this.userCompany;
	}
	
}

