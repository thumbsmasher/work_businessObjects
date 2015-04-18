package com.idex.businessObjects.objectManager;

public class universe {
	
	private String UniverseCUID;
	private int UniverseID;
	private String UniverseName;
	
	@SuppressWarnings("unused")
	private universe() {
		//Default constructor
	}
	
	public universe(String myCUID, int myID, String myName) {
		// Basic report object
		this.setUniverseCUID(myCUID);
		this.setUniverseID(myID);
		this.setUniverseName(myName);
	}
	
	private void setUniverseCUID(String myCUID) {
		this.UniverseCUID = myCUID;
	}
	private void setUniverseID(int myID) {
		this.UniverseID = myID;
	}
	private void setUniverseName(String myName) {
		this.UniverseName = myName;
	}
	
	public String getUniverseCUID() {
		return this.UniverseCUID;
	}
	public int getUniverseID() {
		return this.UniverseID;
	}
	public String getUniverseName() {
		return this.UniverseName;
	}
	
}
