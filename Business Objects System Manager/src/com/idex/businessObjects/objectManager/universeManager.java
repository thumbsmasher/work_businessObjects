package com.idex.businessObjects.objectManager;

import java.util.ArrayList;
import java.util.List;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;

public class universeManager {

	private ArrayList<String> myFields;
	private List<universe> myObjectList;
	private IInfoStore iStore;
	
	private final String getUniverseFromCUID = "SELECT top 1 {fields} from CI_APPOBJECTS where SI_CUID='{CUID}'";
	
	private boolean DEBUG = false;
	private boolean DEBUG_showQuery = false;
	
	//Default constructor
	public universeManager(Object myiStore) throws Exception  {
		myFields = new ArrayList<String>();
		myFields.add("SI_CUID");
		myFields.add("SI_NAME");
		myFields.add("SI_KIND");
		
		myObjectList = new ArrayList<universe>();
		
		iStore = (IInfoStore)myiStore;
	}
	
	public void getUniverseFromCUID(String myCUID) {
		String myQuery = getUniverseFromCUID.replace("{CUID}", myCUID);
		if (DEBUG_showQuery) {System.out.println("...... Universe Manager query :: " + myQuery);}
		this.getObjectList("universe", myQuery, myFields);
	}
	
	public void getUniverseFromID(int myID) {
		// TODO Auto-generated method stub
		String myQuery = getUniverseFromCUID.replace("SI_CUID", "SI_ID");
		myQuery = myQuery.replace("{CUID}", Integer.toString(myID));
		if (DEBUG_showQuery) {System.out.println("...... Universe Manager query :: " + myQuery);}
		this.getObjectList("universe", myQuery, myFields);
	}
	
	
	public String getUniverseName(int myObjectIndex) {
		return myObjectList.get(myObjectIndex).getUniverseName();
	}
	
	private void getObjectList(String myObjectType, String myQuery, ArrayList<String> myFields) {
		// Perform the query.
	    IInfoObjects results;
	    
	    // Get the fields from those passed for use in the InfoObjects query
	    String myFieldsAsString = "";
	    for(int i=0;i<myFields.size();i++) {
	    	myFieldsAsString += myFields.get(i) + ", ";
	    }
	    myFieldsAsString = myFieldsAsString.substring(0, myFieldsAsString.length()-2);
	    
	    myQuery = myQuery.replace("{fields}", myFieldsAsString);
	    if (DEBUG_showQuery) {System.out.println("...... Universe Manager query :: " + myQuery);}
	    
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
		            
		            try {
		            	if (DEBUG_showQuery) {System.out.println(myObjectType + " : " + infoObject.getCUID() + " : " + infoObject.getTitle());}
		            	if (myObjectType.equals("universe")) { 
		            		myObjectList.add(new universe(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle()));
		            	} 
		            }
		            catch (Exception ex) {
		            	if (DEBUG) {System.out.println("... universeManager GENERAL ERROR (adding objects to objectList) " + ex.getClass() + " : " + ex.getMessage());}
		            }
		        }
		    	if (DEBUG) {System.out.println("... universeManager has " + Integer.toString(myObjectList.size()) + " objects");}
		    }
	    }
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... universeManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... universeManager GENERAL ERROR " + ex.getMessage());}
	    }
	}

	
}
