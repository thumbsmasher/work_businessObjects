package com.idex.businessObjects.objectManager;

public class folder {
	private String folderCUID;
	private int folderID;
	private String folderName;
	private String parentFolderCUID;
	private int parentFolderID;
	private String folderCompletePath;
	//private String folderPath;
	
	@SuppressWarnings("unused")
	private folder() {
		//Default constructor
		
	}
	
	public folder(String myfolderCUID, int myfolderID, String myfolderName, String myparentFolderCUID, int myparentFolderID, String myfolderCompletePath) {
		this.setFolderCUID(myfolderCUID);
		this.setFolderID(myfolderID);
		this.setFolderName(myfolderName);
		this.setParentFolderCUID(myparentFolderCUID);	
		this.setParentFolderID(myparentFolderID);
		this.setFolderCompletePath(myfolderCompletePath);
	}
	
	// Getter and Setter for folderCUID
	private void setFolderCUID(String myString) {
		this.folderCUID = myString;
	}
	public String getFolderCUID() {
		return folderCUID;
	}
	
	// Getter and Setter for folderID
	private void setFolderID(int myString) {
		this.folderID = myString;
	}
	public int getFolderID() {
		return folderID;
	}
	
	//Getter and Setter for folderName
	private void setFolderName(String myString) {
		this.folderName = myString;
	}
	public String getFolderName() {
		return this.folderName;
	}
	
	private void setFolderCompletePath(String myString) {
		this.folderCompletePath = myString;
	}
	public String getFolderCompletePath() {
		return this.folderCompletePath;
	}
	
	//Getter and Setter for parentFolderID
	private void setParentFolderCUID(String myString) {
		this.parentFolderCUID = myString;
	}
	public String getParentFolderCUID() {
		return parentFolderCUID;
	}
	//Getter and Setter for parentFolderID
	private void setParentFolderID(int myString) {
		this.parentFolderID = myString;
	}
	public int getParentFolderID() {
		return parentFolderID;
	}
	
	
}
