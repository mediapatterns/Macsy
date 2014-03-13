package macsy.module.onlineRanking;

import java.util.Date;
import java.util.List;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.blackBoardsSystem.BlackBoardDateBased;

/** 
 * This is a class that helps the main module which implements the
 * ranking process between documents to communicate with the blackboard
 * 
 * @author Panagiota Antonakaki
 * Last update: 12-03-2014
 *
 */
public class OnlineRanker_StorageLayer {
	private BlackBoardDateBased inputbb = null;
	private BlackBoardDateBased outputbb = null;
	
	/**
	 * Initialise the blackboard
	 * @param bb
	 * @throws Exception
	 */
	public OnlineRanker_StorageLayer(BlackBoardDateBased inputbb,
			BlackBoardDateBased outputbb) throws Exception
	{
		this.inputbb = inputbb;
		this.outputbb = outputbb;
	}
	
	/**
	 * Find in the database the documents in the specific period of time
	 * @param fromDate
	 * @param toDate
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	public BBDocSet findDocsByFieldsTagsSet(Date fromDate, 
			Date toDate, List<Integer> withTags,   
			int limit) throws Exception
	{
		return inputbb.findDocsByFieldsTagsSet(fromDate, 
				toDate, 
				null, 
				null, 
				withTags, 
				null, 
				limit);
	}
	
	/**
	 * Adds a field on the specific document
	 * @param docID
	 * @param fieldName
	 * @param fieldValue
	 * @throws Exception
	 */
	public void addFieldToDoc(Object docID, 
			String fieldName, 
			Double fieldValue) throws Exception
	{
		//outputbb.addFieldToDoc(docID, fieldName, fieldValue);
	}
	
	/**
	 * Removes a tag from the specific document
	 * @param articleID
	 * @param tagID
	 * @throws Exception
	 */
	public void removeTagFromDoc(Object articleID, int tagID) throws Exception
	{
		//inputbb.removeTagFromDoc(articleID, tagID);
	}
	
	/**
	 * Adds a tag on the specific document
	 * @param articleID
	 * @param tagID
	 * @throws Exception
	 */
	public void addTagToDoc(Object articleID, int tagID) throws Exception
	{
		//outputbb.addTagToDoc(articleID, tagID);
	}
	
	/**
	 * Returns the id of the specific tag of 0 if it doesn't exist
	 * @param tagName
	 * @return
	 * @throws Exception
	 */
	public int getInputTagID(String tagName) throws Exception
	{
		int tagID =  inputbb.getTagID(tagName);
		if(tagID == BlackBoard.TAG_NOT_FOUND)
			tagID = 0;
		return tagID;
	}
	
	/**
	 * Returns the tag id of the specific tag
	 * (insert the tag if it doesn't exist)
	 * @param tagName
	 * @return
	 * @throws Exception
	 */
	public int getOutputTagID(String tagName) throws Exception
	{
		int tagID =  inputbb.getTagID(tagName);
		if(tagID == BlackBoard.TAG_NOT_FOUND)
			tagID = inputbb.insertNewTag( tagName );
		return tagID;
	}


        /**
	 * Get the set of docs with the specified tags (more than one) and the specified
	 * period of days
	 * @param inputTag_List
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	public BBDocSet getDocSetWithTags(Date fromDate, Date toDate,
			List<Integer> inputTag_List,
			int limit) throws Exception
	{
		return inputbb.findDocsByFieldsTagsSet(fromDate,
				toDate,
				null,
				null,
				inputTag_List,
				null,
				limit);
	}	

}

