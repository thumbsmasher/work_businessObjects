package com.idex.businessObjects.objectManager;

public class reportConnection {
	private int connectionProperty;
	private int connectionType;
	private String connectionName;
	private String connectionDatabase;
	private String connectionUserName;
	
	@SuppressWarnings("unused")
	private reportConnection() {
		//Default constructor
	}
	
	public reportConnection(int myConnectionProperty, int myConnectionType, String myConnectionName, String myConnectionDatabase, String myConnectionUserName) {
		this.connectionProperty = myConnectionProperty;
		this.connectionType = myConnectionType;
		this.connectionName = myConnectionName;
		this.connectionDatabase = myConnectionDatabase;
		this.connectionUserName = myConnectionUserName;
	}
	
	public int getConnectionProperty() {
		return this.connectionProperty;
	}
	public int getConnectionType() {
		return this.connectionType;
	}
	public String getConnectionName() {
		return this.connectionName;
	}
	public String getConnectionDatabase() {
		return this.connectionDatabase;
	}
	public String getConnectionUserName() {
		return this.connectionUserName;
	}
}
