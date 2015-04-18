package com.idex.businessObjects.objectManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.properties.internal.SDKPropertyBag;

public class userManager {
	private ArrayList<String> myFields;
	private List<user> myUserList;
	private IInfoStore iStore;

	private final String getUserObjects = "SELECT top 10000 SI_CUID, SI_NAME, SI_LASTLOGONTIME, SI_NAMEDUSER, SI_ALIASES, SI_EMAIL_ADDRESS, SI_UPDATE_TS FROM CI_SYSTEMOBJECTS where SI_KIND='USER'";
	private final String getSessions = "SELECT SI_CUID, SI_ID, SI_USERID, SI_NAME, SI_LASTLOGONTIME, SI_LAST_ACCESS, SI_CONCURRENT FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'Connection' AND SI_FAILOVER_AVAILABLE_UNTIL = NULL AND SI_AUTHEN_METHOD != 'server-token' ORDER BY SI_NAME";
	private final String getSessionByUserID = "SELECT SI_CUID, SI_ID, SI_USERID, SI_NAME, SI_LASTLOGONTIME, SI_LAST_ACCESS, SI_CONCURRENT FROM CI_SYSTEMOBJECTS WHERE SI_KIND = 'Connection' AND SI_FAILOVER_AVAILABLE_UNTIL = NULL AND SI_AUTHEN_METHOD != 'server-token' and SI_NAME='{prompt1}' ORDER BY SI_NAME";
	
	private final String mySQLInsertUser = "INSERT INTO [BOE-XI_31_logs].dbo.KPI_Usage_UserArchive (companyName, locationCode, userID, licenseType, lastLogonTime, lastUpdated, effectiveDate) VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	private DateFormat df = new SimpleDateFormat("EEE MMM dd k:m:s z yyyy");
	private DateFormat dfDisplay = new SimpleDateFormat("MM-dd-yyyy k:m:s z");
	private DateFormat dfDisplayShort = new SimpleDateFormat("MM-dd-yyyy");
	
	private boolean DEBUG = true;
	private boolean DEBUG_showQuery = true;

	//Default constructor
	public userManager(Object myiStore) throws Exception  {
		
		myFields = new ArrayList<String>();
		
		iStore = (IInfoStore)myiStore;
		
		myUserList = new ArrayList<user>();
		
	}
	
	public void getUserList() {
		if (DEBUG) {System.out.println("userManager -> Getting User List");}
		myUserList = new ArrayList<user>();
		
		String myQuery = getUserObjects;
		if (DEBUG_showQuery) {System.out.println("...... User Manager query :: " + myQuery);}
		
		this.getObjectList("users", myQuery, myFields);
	}
	
	public int userCount() {
		return myUserList.size();
	}
	
	public int getUserID(int myUser) {
		return myUserList.get(myUser).getUserID();
	}
	public String getUser(int myUser) {
		return myUserList.get(myUser).getUserName();
	}
	public String getUserLastLogonDate(int myUser) {
		return dfDisplayShort.format(myUserList.get(myUser).getLastLogonDate());
	}
	public String getUserLastUpdated(int myUser) {
		return dfDisplayShort.format(myUserList.get(myUser).getLastUpdatedDate());
	}
	public String getUserCUID(int myUser) {
		return myUserList.get(myUser).getUserCUID();
	}
	public String getUserLicenseType(int myUser) {
		return myUserList.get(myUser).getLicenseType();
	}
	public String getUserLocation(int myUser) {
		return myUserList.get(myUser).getUserLocation();
	}
	public String getUserCompany(int myUser) {
		return myUserList.get(myUser).getUserCompany();
	}

	public void getActiveSessions() {
		if (DEBUG) {System.out.println("userManager -> Getting Active Sessions");}
		
		String myQuery = getSessions;
		if (DEBUG_showQuery) {System.out.println("...... User Manager query :: " + myQuery);}
		
		this.getObjectList("userSession", myQuery, myFields);
		
	}
	
	public void setUserNamedLicense(String userName, int setAsNamedUser) {
		if (DEBUG) {System.out.println("userManager -> Getting User Object: " + userName);}
		
		String myQuery = "SELECT top 5 * FROM CI_SYSTEMOBJECTS where SI_KIND='USER' and SI_NAME='" + userName + "'";
		
		if (DEBUG_showQuery) {System.out.println("...... User Manager query :: " + myQuery);}
		
		try {
			IInfoObjects userObjects = iStore.query(myQuery);
			if (userObjects.size() == 0) {
				System.out.println("No users found matching name: " + userName);
			} else if (userObjects.size() > 1){
				System.out.println("Multiple user objects found matching name: " + userName);
			} else {
				IInfoObject userObject = (IInfoObject) userObjects.get(0);
				System.out.print("Updating properties for: " + userObject.getTitle().toString());
				
				int changeFlag = 0;
				boolean namedUserPropertyFlag = setAsNamedUser==1;
				
				if (setAsNamedUser==1 && userObject.properties().getProperty("SI_NAMEDUSER").toString().equals("false")) {
					System.out.print("    ...Setting to named user");
					userObject.properties().setProperty("SI_NAMEDUSER", namedUserPropertyFlag);
					changeFlag=1;
				} else if (setAsNamedUser==0 && userObject.properties().getProperty("SI_NAMEDUSER").toString().equals("true")) {
					System.out.print("    ...Setting to concurrent user");
					userObject.properties().setProperty("SI_NAMEDUSER", namedUserPropertyFlag);
					changeFlag=1;
				} else {
					System.out.println("    ...No change required");
				}
				if (changeFlag==1) {
					iStore.commit(userObjects);
					System.out.println("    ...Complete");
				}
				
			}
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			System.out.println("ERROR Updating User Licence Concurren/Named... " + ex.getMessage());
		}
		
		
		
	}
	
	public void deleteUser(String myUserName) {
		String protectedUsers = "Administrator,Guest,dkirk,QAAWS,PMUSER";
		try {
			if (protectedUsers.toUpperCase().contains(myUserName.toUpperCase())) {
				
			} else {
				// Check to see if the user owns any objects
				IInfoObjects infoObjectsByOwner = iStore.query("SELECT top 5 * from CI_INFOOBJECTS where SI_KIND != 'PersonalCategory' and SI_KIND != 'FavoritesFolder' and SI_KIND != 'Inbox' and SI_NAME!='~Webintelligence' and SI_OWNER = '" + myUserName + "'");
				System.out.print(myUserName + " has " + infoObjectsByOwner.size() + " objects - ");
				
				if (infoObjectsByOwner.size() == 0) {
					IInfoObjects userObject = iStore.query("Select TOP 1 * from CI_SYSTEMOBJECTS where SI_PROGID = 'CrystalEnterprise.User' and SI_NAME = '" + myUserName + "'");
					// Delete the User
					userObject.delete((IInfoObject)userObject.get(0));
					
					//Commit the changes to the InfoStore.
			        iStore.commit(userObject);
			        System.out.println("User has been removed!");
				} else {
					System.out.println("Cannot delete " + myUserName);
				}
				
			}
		} catch (Exception ex) {
			System.out.println("ERROR : userManager : deleteUser : Could not delete user - " + ex.getMessage());
		}
		
        
	}
	
	private void getObjectList(String myObjectType, String myQuery, ArrayList<String> myFields) {
		// Perform the query.
	    IInfoObjects results;
	    
	    // Get the fields from those passed for use in the InfoObjects query
	    try
	    {
	        results = iStore.query(myQuery);
	        
	        if (results.isEmpty())
		    {
		    	// Return an informative message if there are no sub-folders.
			    
		    } else {
		    	for(int i = 0; i < results.size(); i++)
		        {
		            IInfoObject infoObject = (IInfoObject)results.get(i);
		            
		            DateFormat df2 = new SimpleDateFormat("MM-dd-yyyy");
            		Date lastLogonTime = df2.parse("01-01-1901");
            		Date lastUpdatedTime = df2.parse("01-01-1901");
            		String userLicenserType = "CONCURRENT";
            		String userLocation = "";
            		String userCompany = "";
            		
            		if (myObjectType.equals("userSession")) {
		            	try {
		            		lastLogonTime = df.parse(infoObject.properties().getProperty("SI_LASTLOGONTIME").toString());
		            		lastUpdatedTime = df.parse(infoObject.properties().getProperty("SI_LAST_ACCESS").toString());
		            		
			            	if (DEBUG) {System.out.println(myObjectType + " : " + infoObject.getID() + " : " + infoObject.getTitle() + " : " + dfDisplay.format(lastLogonTime));}
		            		myUserList.add(new user(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle().toUpperCase(), lastLogonTime, lastUpdatedTime));
			            }
			            catch (Exception ex) {
			            	if (DEBUG) {System.out.println("... userManager GENERAL ERROR (adding user to objectList) " + ex.getClass() + " : " + ex.getMessage());}
			            }
		            } else if (myObjectType.equals("users")) {
		            	// SI_NAME, SI_LASTLOGONTIME, SI_NAMEDUSER, SI_ALIASES, SI_EMAIL_ADDRESS, SI_UPDATE_TS
		            	try {
		            		
		            		try {
		            			lastLogonTime = df.parse(infoObject.properties().getProperty("SI_LASTLOGONTIME").toString());
		            		} catch (Exception dateEX) {
		            			//  Unable to parse lastLogonTime
		            		}
		            		try {
		            			lastUpdatedTime = df.parse(infoObject.properties().getProperty("SI_UPDATE_TS").toString());
		            		} catch (Exception dateEX) {
		            			// Unable to parse lastUpdatedTime
		            		}
		            		if (infoObject.properties().getProperty("SI_NAMEDUSER").toString().equals("true")){
		            			userLicenserType = "NAMED";
		            		}
			            	if (DEBUG_showQuery) {System.out.println(myObjectType + " : " + infoObject.getCUID() + " : " + infoObject.getTitle() + " : " + df2.format(lastLogonTime));}

			            	SDKPropertyBag myInfoObjectPropertyBag = (SDKPropertyBag) infoObject.properties();
			            	SDKPropertyBag myUserAliasPropertyBag = (SDKPropertyBag)myInfoObjectPropertyBag.getItem("SI_ALIASES").getPropertyBag();
			            	
			            	for (int myAliasLoop = 1; myAliasLoop < myUserAliasPropertyBag.size(); myAliasLoop++) {
			            		SDKPropertyBag myUserAliasKeys = (SDKPropertyBag) myUserAliasPropertyBag.getItem(Integer.toString(myAliasLoop)).getPropertyBag();
			            		String myUserAlias = myUserAliasKeys.getItem("SI_NAME").getValue().toString();
			            		if (myUserAlias.contains("secWinAD:")) {
			            			// Parse the secWinAD alias to get the user location and company
			            			int startOfOU = myUserAlias.indexOf("OU=");
			            			int startOfDC = myUserAlias.indexOf("DC=");
			            			String userOUPath = myUserAlias.substring(startOfOU, startOfDC-1);
			            			
			            			String[] userOUComponents = userOUPath.split(",OU=");
			            			userLocation = userOUComponents[userOUComponents.length-2]; 
			            			userCompany = userOUComponents[userOUComponents.length-1];
			            		} else {
			            			System.out.println("NOT FOUND IN " + myUserAlias);
			            		}
			            	}
			            	
			            	//String myCUID, String myUserID, String myFullName, String myEmail, Date myLastLogon, Date myLastUpdated
			            	myUserList.add(new user(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle().toUpperCase(), "", infoObject.properties().getProperty("SI_EMAIL_ADDRESS").toString(), userLicenserType, userLocation, userCompany, lastLogonTime, lastUpdatedTime));		            	 
		            	} catch (Exception ex) {
		            		if (true) {System.out.println("... userManager GENERAL ERROR (adding user to userList) " + infoObject.getTitle() + " : " + ex.getClass() + " : " + ex.getMessage());}
		            	}
		            }
		        }
		    	if (DEBUG) {System.out.println("... userManager has " + Integer.toString(myUserList.size()) + " objects");}
		    }
	    }
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... userManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... userManager GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	
	public void deleteUserSession(int myID) {
		// Perform the query.
	    try {
	    	IInfoObjects oSessions = (IInfoObjects) iStore.query("SELECT SI_ID from CI_SYSTEMOBJECTS WHERE SI_ID =" + myID);
	    	
	    	IInfoObject oSession = (IInfoObject)oSessions.get(0);
	    	
	    	oSessions.delete(oSession);
	    	
	    	iStore.commit (oSessions);
	    	
	    	if (DEBUG) {System.out.println("... userManager -> session deleted for ID " + myID);} 

	    }
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... userManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... userManager GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void deleteUserSessionsByUserID(String myUserID) {
		if (DEBUG) {System.out.println("userManager -> Getting Active Sessions for User - " + myUserID);}
		
		String myQuery = getSessionByUserID.replace("{prompt1}", myUserID);
		 if (DEBUG) {System.out.println("   Delete user sessions query => " + myQuery);}
		// Perform the query.
	    IInfoObjects results;
	    
	    try
	    {
	        results = iStore.query(myQuery);
	        if (DEBUG) {System.out.println("Found " + results.getResultSize() + " user sessions");}
	        if (results.isEmpty())
		    {
		    	// Return an informative message if there are no sub-folders.
			    
		    } else {
		    	for(int i = 0; i < results.size(); i++)
		        {
		            IInfoObject infoObject = (IInfoObject)results.get(i);
		            
		            try {
		            	Date lastLogonTime = df.parse(infoObject.properties().getProperty("SI_LASTLOGONTIME").toString());
		            	DateFormat df = new SimpleDateFormat("MM-dd-yyyy k:m:s z");
		        		
		            	if (true) {System.out.println("Deleting Object -> " + infoObject.getKind() + " : " + infoObject.getID() + " : " + infoObject.getTitle() + " : " + df.format(lastLogonTime));}
		            	if (infoObject.getKind().equals("Connection")) { 
		            		// Delete User Session
		            		deleteUserSession(infoObject.getID());
		        	    	if (true) {System.out.println(".... Session deleted");}
		            	}
		            }
		            catch (Exception ex) {
		            	if (DEBUG) {System.out.println("... userManager GENERAL ERROR (adding user to objectList) " + ex.getClass() + " : " + ex.getMessage());}
		            }
		        }	
		    }
	    }
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... userManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... userManager GENERAL ERROR " + ex.getMessage());}
	    }
		
	}

	public int writeUserListToDatabase(Connection myConn) {
		if (DEBUG) {System.out.println("Starting write user list to database");}
		int myRecords = 0;
		
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		String myDate = df2.format(new Date());
		
		for (int myLoop=0;myLoop<myUserList.size();myLoop++) {
			
			if (DEBUG) {System.out.println("User: " + myLoop + " : " + myUserList.get(myLoop).getUserID());}
			
			try {
				if (DEBUG) {System.out.println("INSERTING: " + mySQLInsertUser);}
				PreparedStatement psInsert = myConn.prepareStatement(mySQLInsertUser);
				//PARAMS: (companyName, locationCode, userName, licenseType, lastLogonTime, lastUpdated, effectiveDate)
				psInsert.setString(1, myUserList.get(myLoop).getUserCompany());
				psInsert.setString(2, myUserList.get(myLoop).getUserLocation());
				psInsert.setString(3, myUserList.get(myLoop).getUserName());
				psInsert.setString(4, myUserList.get(myLoop).getLicenseType());
				psInsert.setString(5, df2.format(myUserList.get(myLoop).getLastLogonDate()));
				psInsert.setString(6, df2.format(myUserList.get(myLoop).getLastUpdatedDate()));
				psInsert.setString(7, myDate);

				myRecords = psInsert.executeUpdate();
				if (DEBUG) {System.out.println("       " + myUserList.get(myLoop).getUserID()+ " : " + myRecords + " records inserted");}
					
			} catch (Exception ex) {
				if (true) {System.out.println("*** ERROR 100  - cannot add user record to database" + ex.getMessage());}
				if (true) {System.out.println("             " + myUserList.get(myLoop).getUserCompany() + " : " + myUserList.get(myLoop).getUserLocation() + " : " + myUserList.get(myLoop).getUserID()  + " : " + myUserList.get(myLoop).getLicenseType() + " : " + df2.format(myUserList.get(myLoop).getLastLogonDate()) + " : " + df2.format(myUserList.get(myLoop).getLastUpdatedDate()));}
			}	
		}
		return 0;
	}
}


