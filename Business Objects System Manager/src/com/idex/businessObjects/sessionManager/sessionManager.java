package com.idex.businessObjects.sessionManager;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.framework.ISessionMgr;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.occa.managedreports.IReportAppFactory;
//import com.crystaldecisions.sdk.occa.security.ILogonTokenMgr;

//import com.crystaldecisions.sdk.properties.*;
//import com.crystaldecisions.sdk.occa.managedreports.*;

public class sessionManager {
	
	private static IEnterpriseSession enterpriseSession;
	private static ISessionMgr sm;
	private IInfoStore iStore;
	private String failure;
	
	private static final boolean DEBUG = false;
	
	private boolean loggedIn;
	
	// Default constructor
	public sessionManager() throws Exception  {
		String cms = "SCIDXCRE03:6400";
		String username = "dkirk";
		String password = "test12";
		String authentication = "secEnterprise";
		
		// create the enterprise Session
		new sessionManager(cms, username, password, authentication);
	}
	
	public sessionManager(String myCMS, String myUsername, String myPwd, String myAuthType) throws Exception  {
				
		// create the enterprise Session
		getSession(myCMS, myUsername, myPwd, myAuthType);
		if (DEBUG) {System.out.println("Session created");}
		
		// Create the IInfoStore object.
        setiStore((IInfoStore) enterpriseSession.getService("InfoStore"));
        
	}
	public IReportAppFactory getRASReportService() {
		try {
			return (IReportAppFactory)enterpriseSession.getService("", "RASReportService");
		} catch (SDKException SDKEx) {
			return null;
		}
	}
	private void getSession(String cms, String username, String password, String authentication)  {
		enterpriseSession = null;
		failure = null;
		loggedIn = false;
		if (DEBUG) {System.out.println("Starting getSession");}
		
		try
		{
			sm = CrystalEnterprise.getSessionMgr();
			if (DEBUG) {System.out.println("... have session manager");}
			
			//String[] myAuthIDs = sm.getInstalledAuthIDs();
			//for (int i=0;i<myAuthIDs.length;i++) {
			//	System.out.println("...... " + myAuthIDs[i].toString());
			//}
			
			enterpriseSession = sm.logon( username, password, cms, "secEnterprise" );
			if (DEBUG) {System.out.println("... logged on as " + enterpriseSession.getUserInfo().getUserName());}

			// Set loggedIn property
			loggedIn = true;
			
			// Initialize InfoStore
			setiStore((IInfoStore)enterpriseSession.getService("InfoStore"));
		}
		catch (SDKException sdkex) {
			if (DEBUG) {System.out.println("... *** SDK EXCEPTION " + sdkex.getMessage());}
			loggedIn = false;
	        setError(sdkex.getMessage());
		}
		catch (Exception error)
	    {
			if (DEBUG) {System.out.println("... *** GENERAL EXCEPTION " + error.getMessage());}
			loggedIn = false;
	        setError(error.getMessage());
	    }
	}
	
	public void closeSession() {
		try {
			enterpriseSession.logoff();
			destroyiStore();
			
			if (DEBUG) {System.out.println("... logged off");}
		} catch (Exception ex) {
			if (DEBUG) {System.out.println("... ERROR LOGGING OFF " + ex.getMessage());}
		}
		
	}
	
	public boolean isLoggedIn() {
		return loggedIn;
	}
	public String getLoggedOnUserName() {
		try {
			return enterpriseSession.getUserInfo().getUserName();
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	public String getError() {
		return failure;
	}
	private void setError(String myMsg) {
		this.failure = myMsg;
	}

	private void setiStore(IInfoStore iStore) {
		this.iStore = iStore;
	}
	private void destroyiStore() {
		this.iStore = null;
	}

	public Object getiStore() {
		return iStore;
	}
	
}
    