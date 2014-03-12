package macsy.blackBoardsSystem;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;

/**
 * A generic Black Board record class.
 * 
 * The BB functions return objects extended from this class.
 * 
 * @author      Ilias Flaounas <iliasfl@bris.ac.uk>
 * @version     1.0                   
 * @since       2011-12-01
 * 
 */
public class BBDoc extends Object
{
	/**
	 * The data container. This is MongoDB specific.
	 */
	BasicDBObject dataObject;


	/**
	 * Constructor of a new document. 
	 * Assumes data of type BasicDBObject.
	 */
	BBDoc(Object data) 
	{
		dataObject = (BasicDBObject) data;
	}

	/**
	 * Constructor of a new, empty, document.
	 * 
	 * Use setter functions to populate.
	 */
	public BBDoc() 
	{
		dataObject = new BasicDBObject();
	}


	BBDoc(BBDoc clone) 
	{
		this.dataObject = clone.dataObject; 
	}




	/**
	 * Gets the ID of the document.
	 * @return The object ID
	 */
	public Object				getID()			{	return dataObject.get(  BlackBoard.DOC_ID ); 		}

	/**
	 * Gets the list of the tag IDs of the document.
	 * @return A List<Integer> with Tag IDs or null if empty.
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> 		getTagIDs()		
	{	
		return ( List<Integer> )dataObject.get( BlackBoard.DOC_TAGS ); 
	}

	/**
	 * Gets only control tags, i.e., with prefix "FOR>" or "POST>"
	 * @return A list of tag-IDs. 
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> getCtrlTagIDs()		
	{	
		return ( List<Integer> )dataObject.get( BlackBoard.DOC_FOR_TAGS ); 
	}


	/**
	 * Returns all tags, including ctrl tags.
	 * @return A list of all TagIDs
	 */
	@SuppressWarnings("unchecked")
	public List<Integer> getAllTagIDs()		
	{	
		List<Integer> tags = ( List<Integer> )dataObject.get( BlackBoard.DOC_TAGS );
		if(tags == null)
			tags = new LinkedList<Integer>();

		List<Integer> fortags = ( List<Integer> ) dataObject.get( BlackBoard.DOC_FOR_TAGS );
		if(fortags!=null)
			tags.addAll(fortags);

		return tags; 
	}

//	public void setID(Object ID) 				{	this.ID = ID;		}
//	public void setDate(Date date) 				{	this.date = date;	}
//	public void setTags(List<Integer> tagIDs)	{	this.tagIDs= tagIDs;	}

	/**
	 * Gets the value of the specified field of the document.
	 *  
	 * @param fieldName The name of the field to get.
	 * @return The value of the field as Object or null if empty.
	 */
	public Object getField(String fieldName)
	{		
		try
		{
			return dataObject.get( fieldName );
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the value of a sub-field.
	 * @param fieldName
	 * @param subfieldName
	 * @return The sub-field value or null if empty.
	 */
	public Object getSubField(String fieldName, String subfieldName)
	{		
		try
		{
			return ((BasicDBObject)dataObject.get( fieldName )).get(subfieldName); 
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the integer value of the specified field of the document.
	 *  
	 * @param fieldName The name of the field to get.
	 * @return The value of the field as Integer or null if it doesn't exist.
	 */
	public Integer getFieldInt(String fieldName)
	{	
		try
		{
			return dataObject.getInt( fieldName ); 
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the value of the specified field of the document as real number.
	 *  
	 * @param fieldName The name of the field to get.
	 * @return The real number value of the field or null if empty.
	 */
	public Double getFieldDouble(String fieldName)
	{		
		try 
		{
			return dataObject.getDouble( fieldName ); 
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the value of the specified field of the document as string.
	 *  
	 * @param fieldName The name of the field to get.
	 * @return The value of the field as string.
	 */
	public String getFieldString(String fieldName)
	{		
		try
		{
			return dataObject.getString( fieldName );
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets a list  of type List<String> with the names of the fields of the document.
	 *  
	 * @return A List<String> with field names.
	 */
	public List<String> getAllFieldNames()
	{		
		Set<String> fields_set = dataObject.keySet();

		return new LinkedList<String>(fields_set); 
	}


	/**
	 * Gets a list  of  the names of the subfields of the document.
	 * 
	 * @param fieldName The name of the field of interest.
	 * @return A list of Strings List<String>
	 */
	public List<String> getAllSubFieldNames(String fieldName)
	{
		Set<String> fields_set = ((BasicDBObject) dataObject.get(fieldName)).keySet();

		return new LinkedList<String>(fields_set);
	}


	/**
	 * Sets a field of the document to the specified value.
	 *  
	 * @param fieldName The name of the field to get.
	 */
	public void setField(String fieldName, Object fieldValue)
	{		
		dataObject.put(fieldName , fieldValue); 
	}

	/**
	 * Sets the ID of the document.
	 * 
	 * @param ID The ID of the document.
	 */
	public void setID(Object ID)
	{
		dataObject.put( BlackBoard.DOC_ID , ID); 
	}

	/**
	 * Sets the tags of the object.
	 *  
	 * @param tagIDs A list of type List<Integer> with the IDs of the tags 
	 * that will annotate the document.
	 */
	public void setTags(List<Integer> tagIDs)
	{		
		dataObject.put( BlackBoard.DOC_TAGS , tagIDs); 
	}

	/**
	 * Sets the Control tags of the object, ie tags with FOR> or POST> prefix.
	 *  
	 * @param tagIDs A list of type List<Integer> with the IDs of the tags 
	 * that will annotate the document.
	 */
	public void setCtrlTags(List<Integer> tagIDs)
	{		
		dataObject.put( BlackBoard.DOC_FOR_TAGS , tagIDs); 
	}	

	/**
	 *	Removes tag from doc. 
	 *	Assumes a non-control tag.
	 */
	public void removeTag(int tagID)
	{
		List<Integer> tags = getAllTagIDs();
		tags.remove( (Object) new Integer(tagID));	//to avoid tread tagID as index in list.
		
		this.setTags(tags);
	}
	
	/**
	 * Add tags to DOC.
	 * Assumes a non-control tag.
	 * 
	 * @param tagID
	 */
	public void addTag(int tagID)
	{
		List<Integer> tags = getAllTagIDs();
		
		if(tags.contains(tagID))
			return;
		
		tags.add( tagID );
		
		setTags(tags);
	}


	/**
	 * Adds a new field of type List to the document,
	 * and it populates it with the provided elements.
	 * 
	 * If the field already exists and contains the specified value, it is NOT reappended.
	 * If the field doesn't exist it is created as a new list with one object. 
	 * 
	 * @param docID	The ID of the document which will be updated.
	 * @param fieldName The name of the field that will store the list.
	 * @param fieldValue The values to store in the FIELD_NAME.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void appendToFieldList(String fieldName, Object fieldValue) throws Exception 
	{
		List<Object> oldList = (List<Object>) this.getField(fieldName);
		
		if(	oldList==null)
			oldList = new LinkedList<Object>();
		
		if(!oldList.contains(fieldValue))
			oldList.add( fieldValue );
		
		setField(fieldName, oldList);
	}

}
