package com.idex.businessObjects.objectManager;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;

import com.crystaldecisions.sdk.occa.infostore.*;
import com.crystaldecisions.sdk.occa.security.*;
import com.crystaldecisions.sdk.occa.security.internal.IRights;
import com.crystaldecisions.sdk.plugin.desktop.folder.IFolder;
import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.idex.businessObjects.objectManager.folder;

public class folderManager {
	//public ArrayList<folder> myFolderList;
	private ArrayList<folder> myObjectList;
	private ArrayList<String> myFields;
	private IInfoStore iStore;
	
	private boolean DEBUG = false;
	private boolean DEBUG_showQuery = false;
	
	// Get list of all folders in the system
	private final String getAllFolderList = "SELECT {fields} from CI_INFOOBJECTS where (SI_KIND='Folder' or SI_KIND='FavoritesFolder') and SI_ID NOT IN (18, 23, 45, 47, 48) and SI_NAME != '~Webintelligence' ORDER BY SI_PARENTID ";
	private final String getRootFolderList = "SELECT {fields} from CI_INFOOBJECTS where (SI_KIND='Folder' or SI_KIND='FavoritesFolder')  and SI_ID NOT IN (18, 23, 45, 47, 48) and SI_PARENTID=0 ORDER BY SI_NAME ";
	private final String getFolderByCUID = "SELECT {fields} from CI_INFOOBJECTS where (SI_KIND='Folder' or SI_KIND='FavoritesFolder')  and SI_CUID='{CUID}'";
	
	//Default constructor
	public folderManager(Object myiStore) throws Exception  {
		if (DEBUG) {System.out.println("... Folder Manager... constructor");}
		
		myFields = new ArrayList<String>();
		myFields.add("SI_CUID");
		myFields.add("SI_ID");
		myFields.add("SI_NAME");
		myFields.add("SI_PARENT_CUID");
		myFields.add("SI_PARENTID");
		myFields.add("SI_PATH");
		
		iStore = (IInfoStore)myiStore;
		
		myObjectList = new ArrayList<folder>();
		//myFolderHashTable = new Hashtable<String, folder>();
		//myFolderList.addAll(myObjectList);
	}
    
	public void getAllFolders() {
		// Return all folder objects
		this.getObjectList("folder", getAllFolderList, myFields);
		if (DEBUG) {System.out.println("... Folder Manager... objectlist built (all folders)");}
	}
	
	public void getRootFolders() {
		// Return all folder objects
		getObjectList("folder", getRootFolderList, myFields);
		if (DEBUG) {System.out.println("... Folder Manager... objectlist built (root folders) - " + myObjectList.size() + " objects");}
		for (int i=0;i<5;i++) {
			if (DEBUG) {System.out.println("... object => " + myObjectList.get(i).getFolderName());}
		}
		//myFolderList.addAll(myObjectList);
		//if (DEBUG) {System.out.println("... objectlist copied to myFolderList - " + myFolderList.size() + " objects");}
	}
	
	public void getAllChildFolders(String myParentFolderCUID, boolean addParentToList) {
		// Returns all folders under the specified parent folder
		// Start with the parent folder
		if (addParentToList) {
			String myQuery = getFolderByCUID.replace("{CUID}", myParentFolderCUID);
			this.getObjectList("folder", myQuery, myFields);
		}
		
		// Then get all immediate child folders
		//String myQuery = getRootFolderList.replace("SI_PARENTID=0", "SI_PARENT_CUID='" + myParentFolderCUID + "'");
		//this.getObjectList("folder", myQuery, myFields);
		
		// Now loop through the list of folders and continue to add sub folders to the objectList
		//Iterator<folder> it = myObjectList.iterator();
		int myIteratorLoop = 0;
		//it.next();
		
		try {
			//while (it.hasNext()) {
			while (myIteratorLoop<myObjectList.size()) {
				folder myFolderIterator = myObjectList.get(myIteratorLoop);
				//System.out.println("Getting folders for: " + myFolderIterator.getFolderName());
				
				if (DEBUG) {System.out.println("*** Get child folders for " + myFolderIterator.getFolderName() + " : " + myFolderIterator.getFolderCUID());}
				try {
					String myQueryChildFolder = getFolderByCUID.replace("SI_CUID='{CUID}'", "SI_PARENT_CUID='" + myFolderIterator.getFolderCUID() + "'");
					this.getObjectList("folder", myQueryChildFolder, myFields);
            	
				} catch (Exception ex) {

					if (DEBUG) {System.out.println("*** ERROR in Retrieval of CHILD FOLDERS : " + ex.getMessage());}
				}
				//System.out.println("<END> Getting folders for: " + myFolderIterator.getFolderName() + " : " + myObjectList.size() + " :: " + myIteratorLoop);
				myIteratorLoop++;
			}
		
		} catch (Exception ex ) {
			System.out.println("*** ERRROR " + ex.getMessage());
		}

	}
	public void getFolderByCUID(String myCUID) {
		// Return a single folder object
		String myQuery = getFolderByCUID.replace("{CUID}", myCUID); 
		if (DEBUG) {System.out.println("... Folder Manager... getFolderByCUID -> " + myQuery);}
		getObjectList("folder", myQuery, myFields);
		if (DEBUG) {System.out.println("... Folder Manager... objectlist built (folder by CUID)");}
		
	}
	
	public String getFolderCompletePath() {
		if (DEBUG) {System.out.println("......... folderManager :: getting complete folder path : " + myObjectList.size());}
		
		String completePath;
		
		// Set the path as the first folder name
		completePath = myObjectList.get(0).getFolderCompletePath();
		if (DEBUG) {System.out.println("......... folderManager :: Complete Path = " + completePath);}
		
		return completePath;
	}
	
	public String getFolderCUID(int myArrayIndex) {
		folder myFolder = (folder)myObjectList.get(myArrayIndex);
		return myFolder.getFolderCUID();
	}
	public int getFolderID(int myArrayIndex) {
		folder myFolder = (folder)myObjectList.get(myArrayIndex);
		return myFolder.getFolderID();
	}
	public int getParentFolderID(int myArrayIndex) {
		folder myFolder = (folder)myObjectList.get(myArrayIndex);
		return myFolder.getParentFolderID();
	}
	public String getFolderName(int myArrayIndex) {
		folder myFolder = (folder)myObjectList.get(myArrayIndex);
		return myFolder.getFolderName();
	}
	
	public Integer size() {
		try {
			return (Integer)myObjectList.size();
		} catch (Exception ex) {
			if (DEBUG) {System.out.println("... Foldermanager GENERAL ERROR (size)" + ex.getMessage());}
			return -999;
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
	    if (DEBUG_showQuery) {System.out.println("...... folderManager query :: " + myQuery);}
	    
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
		            String myCompletePath = "";
		            //if (DEBUG) {System.out.println("FOLDER OBJECT KIND => " + infoObject.getKind());}
		            try {
		            	try {
		            		IFolder myFolder = (IFolder) infoObject;
		            		myCompletePath = myFolder.getTitle();
			            	
			            	// Loop through the path properties of the folder
			            	String[] myPaths = myFolder.getPath();
			            	for (int myPathLoop=0;myPathLoop<myPaths.length;myPathLoop++) {
			            		myCompletePath = myPaths[myPathLoop].toString() + " > " + myCompletePath;
			            	}
			            	
		            	} catch (Exception ex) {
		            		if (DEBUG) {System.out.println(".......... Unable to get complete folder path : " + ex.getMessage());}
		            	}
		            	if (DEBUG) {System.out.println("..........      Path: " + myCompletePath);}
		            	
		            	folder myFolder = new folder(infoObject.getCUID(), infoObject.getID(), infoObject.getTitle(), infoObject.getParentCUID(), infoObject.getParentID(), myCompletePath);
		            	
		            	myObjectList.add(myFolder);
		            	//System.out.println("     added " + myFolder.getFolderName());
		            	
		            }
		            catch (Exception ex) {
		            	if (true) {System.out.println("... objectManager GENERAL ERROR (adding objects to objectList) " + ex.getClass() + " : " + ex.getMessage());}
		            }
		        }
		    	if (DEBUG) {System.out.println("... objectManager built .... objectManager has " + Integer.toString(myObjectList.size()) + " objects");}
		    }
	    }
	    
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... objectManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... objectManager GENERAL ERROR " + ex.getMessage());}
	    }
	}
	
	public void checkRights(String myCUID) {
		// Perform the query.
	    System.out.println("Checking object rights");
				
		IInfoObjects results;
	    String myQuery = "SELECT * from CI_INFOOBJECTS where (SI_KIND='Folder' or SI_KIND='FavoritesFolder')  and SI_CUID='" + myCUID + "'";
	    
	    try
	    {
	        results = iStore.query(myQuery);
	        
		    if (results.isEmpty())
		    {
		    	// Return an informative message if there are no sub-folders.
			    System.out.println("No folders found");
		    } else {
		    	for(int i = 0; i < results.size(); i++)
		        {
		            IInfoObject iObject = (IInfoObject)results.get(i);
		            
		            System.out.println("GETTING SECURIY FOR => " + iObject.getTitle() + " : " + iObject.getKind());
		            
		            try {
		            	/* Get the principal's rights from the InfoObject.  */

		            	ISecurityInfo objectSecurityInfo = iObject.getSecurityInfo();
		            	
		            	IObjectPrincipals boObjectPrincipals = objectSecurityInfo.getObjectPrincipals();
		            	IObjectPrincipal boObjectPrincipal = null;
		            	
		            	Iterator iterator_principals = null;
		                
		                iterator_principals = boObjectPrincipals.iterator();
		                
		                while (iterator_principals.hasNext()) {
		                     boObjectPrincipal = (IObjectPrincipal) iterator_principals.next();
		                     String strInheritance = "Assigned";
		                     if (boObjectPrincipal.isInherited()) {
		                    	 strInheritance = "Inherited";
		                     }
		                     
		                     System.out.println(strInheritance + " :: " + boObjectPrincipal.getName() + " :: " + boObjectPrincipal.getRole().getDescription(Locale.ENGLISH));
		                     ISecurityRight boObjectRight = null;
		                     
		                     if (boObjectPrincipal.isInherited()) {
		                    	 Iterator iterator_rights = null;
			                     iterator_rights = boObjectPrincipal.getInheritedRights().iterator();
			                     while (iterator_rights.hasNext()) {
			                    	 boObjectRight = (ISecurityRight) iterator_rights.next();
			                    	 System.out.println("     " + boObjectRight.getID() + " :: " + boObjectRight.toString() + " :: " + boObjectRight.isGranted());
			                     }
		                     } else {
		                    	 Iterator iterator_rights = null;
			                     iterator_rights = boObjectPrincipal.getRights().iterator();
			                     while (iterator_rights.hasNext()) {
			                    	 boObjectRight = (ISecurityRight) iterator_rights.next();
			                    	 System.out.println("     " + boObjectRight.getID() + " :: " + boObjectRight.toString() + " :: " + boObjectRight.isGranted());
			                     }
		                     }
		                     
		                     
		                     
		                }

		            } catch (Exception exRightLoop) {
		            	System.out.println(exRightLoop.getMessage());
		            }
		        }
		    	if (DEBUG) {System.out.println("... objectManager built .... objectManager has " + Integer.toString(myObjectList.size()) + " objects");}
		    }
	    }
	    
	    // Return an error message if the query fails.
	    catch (SDKException e)
	    {
	    	if (DEBUG) {System.out.println("... objectManager ERROR " + e.getMessage());}
	    }
	    catch (Exception ex) {
	    	if (DEBUG) {System.out.println("... objectManager GENERAL ERROR " + ex.getMessage());}
	    }    
	    
	}
	
}


