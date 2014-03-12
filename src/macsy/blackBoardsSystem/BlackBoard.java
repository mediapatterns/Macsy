package macsy.blackBoardsSystem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;

/**
 * A general purpose Black Board.
 * 
 * It provides some basic functionality and some abstract methods. 
 * Ideally all BBs should extend it. 
 * 
 * @author      Ilias Flaounas, Tom Welfare
 * @version     1.3                
 * @since       2014-03-12
 * 
 */
public class BlackBoard {

	public static final BBDoc 	DOC_NOT_FOUND = null;
	public static final Integer DOC_EXISTS = -1;
	public static final int		TAG_NOT_FOUND = 0;
	public static final int		TAG_EXISTS = -1;

	

	//	public boolean DB_WriteConcern = true; 
	boolean adminMode = false;

	DB mongo_db = null;
	String BB_NAME = null;	//The name of the BB / or the BB prefix if BB is splitted by year.

	//YEar to DBCollection or 0 for single Collection Blackboard
	Map<Integer,DBCollection> collDocs = null;
	//	DBCollection collDocs = null;	//Assumes one Collection for documents.

	public final static String DOC_ID = "_id";
	public final static String DOC_TAGS = "Tg"; //binary tags
	final static String DOC_FOR_TAGS = "FOR"; //binary control tags

	DBCollection collTags = null;
	final static String TAG_ID = "_id"; 	// 	"Tag ID"
	final static String TAG_NAME = "Nm"; 	// 	"Tag Name

	/**
	 * Field name of the "control" property of the FOR tag.
	 * If value of the Ctrl field is 1 then this is a control property
	 */
	public final static String TAG_PROPERTY_CONTROL = "Ctrl";

	/**
	 * Field name of the "Down Inheritance" property of a tag.
	 * If the value of the field is 1 then this tag should be inhereted from
	 * Outlet to Feed, and from Feed to Article.
	 */
	public final static String TAG_PROPERTY_DOWN_INHERITANCE = "DInh";

	/**
	 * Signals that the document should be processed by a module.
	 */
	final static String CONTROL_TAGS_PREFIX = 	"FOR>";	

	/**
	 * Signals that the document has been processed by a module.
	 */	
	final static String CONTROL_TAGS_PREFIX2 = 	"POST>";

	DBCollection collCounter = null;
	static final String NEXT_ID = "NEXT_ID";
	static final String BLACKBOARD_TYPE = "BLACKBOARD_TYPE";
	static final String BLACKBOARD_TYPE_STANDARD = "STANDARD";
	static final String BLACKBOARD_TYPE_DATE_BASED = "DATE_BASED";



	/**
	 * Returns Black Board Type. If no BBType is set then the default "STANDARD" is set and returned.
	 * @param mongo_db
	 * @param bbName
	 * @return
	 */
	static String getBlackBoardType(DB mongo_db, String bbName, String defaultType)
	{
		DBCollection collCounter = mongo_db.getCollection(bbName+"_COUNTER");
		collCounter.setObjectClass(BasicDBObject.class);

		String bbType = null;
		try
		{
			BasicDBObject query_bbParams = new BasicDBObject();
			query_bbParams.put( "_id", BLACKBOARD_TYPE);
			DBObject bbParams = collCounter.findOne( query_bbParams );
			if(bbParams!=null)
				bbType = (String) bbParams.get( BLACKBOARD_TYPE );
		}
		catch(Exception e)	//No BlackBoard  - empty - return STANDARD
		{
			return BLACKBOARD_TYPE_STANDARD;
		}

		if(bbType==null)
		{
			if(defaultType==null)
				bbType = BLACKBOARD_TYPE_STANDARD;
			else
				bbType = defaultType;
			
			BasicDBObject docID = new BasicDBObject();
			docID.put( "_id", BLACKBOARD_TYPE);
			docID.put( BLACKBOARD_TYPE, bbType);	//1 is the first ID
			//			docID.put( "tag_counter", 1);	//1 is the first ID
			collCounter.insert( docID );
		}

		return bbType;
	}

	/**
	 * CACHE 
	 * 
	 * Currently only last accessed Document.
	 * 
	 */
	BasicDBObject lastQueryResult = null;



	BlackBoard()
	{

	}

	DBCollection getCollDocs()
	{
		return collDocs.get(0);
	}


	
	/**
	 * List of Field Names that shouldn't be altered by API.
	 */
	List<String> FINAL_FIELDS; 

	//Stores the hash of a combination of index fields.
	List<Integer> indexHashes = new ArrayList<Integer>();

	boolean indexHashExists(Integer hashedIndexFields)
	{
		return indexHashes.contains(hashedIndexFields);
	}

	void addIndexHash(Integer hashedIndexFields)
	{
		if(!indexHashes.contains(hashedIndexFields)) {
			indexHashes.add( hashedIndexFields );
		}
	}

	
	
	/**
	 * Constructor for the Black Board with given name.
	 *  
	 * If BlackBoard with given name already exists it is used as is, 
	 * otherwise a new Black Board is created.
	 * 
	 * @param dbConnection	The connection to the database. This can be obtained from NewsJunkieAPI main class.
	 * @param blackBoardName A string that represents the name of the BlackBoard.
	 * @param adminMode True if there going to be a lot of inserts.
	 * 
	 * @throws Exception
	 */
	public BlackBoard( 	
			Object dbConnection,	
			String blackBoardName, 
			boolean adminMode  )
					throws Exception
					{
		System.out.println("Initialising "+ blackBoardName+ " Black Board." );

		this.adminMode = adminMode;
		this.mongo_db = (DB)dbConnection;
		this.BB_NAME  = blackBoardName;

		DBCollection singleCollection = mongo_db.getCollection(BB_NAME);
		singleCollection.setObjectClass(BasicDBObject.class);
		collDocs = new TreeMap<Integer,DBCollection>();
		collDocs.put(0,singleCollection );

		collTags = mongo_db.getCollection(BB_NAME+"_TAGS");
		collTags.setObjectClass(BasicDBObject.class);


		collCounter = mongo_db.getCollection(BB_NAME+"_COUNTER");
		collCounter.setObjectClass(BasicDBObject.class);

		BasicDBObject isThereNextIDQuery = new BasicDBObject();
		isThereNextIDQuery.put( "_id", NEXT_ID);
		DBObject isThereNextID = collCounter.findOne(isThereNextIDQuery);
		if(isThereNextID == null)
		{
			BasicDBObject docID = new BasicDBObject();
			docID.put( "_id", NEXT_ID);
			docID.put( "doc_counter", 1);	//1 is the first ID
			docID.put( "tag_counter", 1);	//1 is the first ID
			collCounter.insert( docID );
		}

		BasicDBObject isThereBlackBoardTypeQuery = new BasicDBObject();
		isThereBlackBoardTypeQuery.put( "_id", NEXT_ID);
		DBObject isThereBlackBoardType = collCounter.findOne(isThereBlackBoardTypeQuery);
		if(isThereBlackBoardType == null)
		{
			BasicDBObject docID = new BasicDBObject();
			docID.put( "_id", BLACKBOARD_TYPE);
			docID.put( BLACKBOARD_TYPE, BLACKBOARD_TYPE_STANDARD);	//1 is the first ID
			//			docID.put( "tag_counter", 1);	//1 is the first ID
			collCounter.insert( docID );
		}



		//MAINTAIN A LIST OF FIELD NAMES THAT SHOULD NOT CHANGE
		if(FINAL_FIELDS==null)
			FINAL_FIELDS = new LinkedList<String>();
		FINAL_FIELDS.add(DOC_ID);
		FINAL_FIELDS.add(DOC_TAGS);
		FINAL_FIELDS.add(DOC_FOR_TAGS);


		//	long bef;
		BasicDBObject index;
		BasicDBObject options;

		//	bef = System.currentTimeMillis();
		//	System.out.print("Checking Index for TAG_NAME for "+ blackBoardName);
		index = new BasicDBObject();
		index.put(TAG_NAME, 1);
		options = new BasicDBObject();
		options.put("background",true);
		collTags.ensureIndex(index, options);
		//	System.out.println(" DONE in (ms) "+ (System.currentTimeMillis() - bef));

		System.out.print("Checking indexes...");
		List<String> indexedFields = new LinkedList<String>();
		indexedFields.add(DOC_TAGS);
		indexedFields.add(DOC_ID);
		ensureIndex(indexedFields);
		
		indexedFields = new LinkedList<String>();
		indexedFields.add(DOC_FOR_TAGS);
		indexedFields.add(DOC_ID);
		ensureIndex(indexedFields);
		System.out.println("DONE indexes");

			
		
					}

	/**
	 * Constructor for the Black Board with given name.
	 * 
	 * If BlackBoard with given name already exists it is used as is, 
	 * otherwise a new Black Board is created.
	 * 
	 * @param dbConnection	The connection to the database. This can be obtained from NewsJunkieAPI main class.
	 * @param blackBoardName A string that represents the name of the BlackBoard.
	 * @throws Exception
	 */
	public BlackBoard( 	Object dbConnection,	String blackBoardName  )
			throws Exception
			{
		this(dbConnection, blackBoardName, false);
			}


	/**
	 * Returns the total number of docs in the BlackBoard.
	 * 
	 * @return The number of docs as integer.
	 * @throws Exception
	 */
	public int getNumberOfDocs() throws Exception {
		return (int) getCollDocs().getCount();
	}

	/**
	 * Returns the total number of different tags in the BlackBoard.
	 * This is not the sum of the tags in each article, but the number of 
	 * district tags in system.
	 * 
	 * @return The number of tags as integer.
	 * @throws Exception
	 */
	public int getNumberOfTags() throws Exception {
		return (int) collTags.getCount();
	}

	//	/**
	//	* Checks if doc with specific title exists
	//	* 
	//	* Inserts new article.
	//	* 
	//	* @param docTitle
	//	* @param docDate
	//	* @return Return the docID of the new doc or DOC_EXISTS if the doc is already there.
	//	* @throws Exception
	//	*/
	//	public synchronized Object insertNewDoc(String docTitle, Date docDate) throws Exception 
	//	{ 
	//	throw new Exception("Unimplemented");
	//	//	return null;		// Return the ID of the new article:
	//	}

	/**
	 * Adds a new field to a document.
	 * 
	 * If field exists, it is updated to the provided value.
	 * 
	 * @param docID The ID of the doc which will be updated.
	 * @param fieldName The name of the field.
	 * @param fieldValue The value to enter to the named field.
	 */
	public void addFieldToDoc(Object docID,String fieldName, Object fieldValue) throws Exception
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not update field "+fieldName);

		getCollDocs().update(	new BasicDBObject(DOC_ID,docID),
				new BasicDBObject("$set",new BasicDBObject(fieldName,fieldValue)));
	}


	/**
	 * Adds a new real number field to a document.
	 * 
	 * If field exists, it is updated to the provided value.
	 * 
	 * @param docID The ID of the doc which will be updated.
	 * @param fieldName The name of the field.
	 * @param fieldValue The value to enter to the named field.
	 */
	public void addRealFieldToDoc(Object docID,String fieldName, double fieldValue) throws Exception
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not update field "+fieldName);

		if(Double.isNaN(fieldValue))
			throw new Exception("NaN is not acceptable");

		getCollDocs().update(	new BasicDBObject(DOC_ID,docID),
				new BasicDBObject("$set",new BasicDBObject(fieldName,fieldValue)));
	}


	/**
	 * Adds a new field of type List to the document,
	 * and it populates it with the provided elements.
	 * 
	 * If the field already exists, the values of the fieldValue are appended to 
	 * the existing ones. If the field doesn't exist it is created as a new list with one object. 
	 * 
	 * @param docID	The ID of the document which will be updated.
	 * @param fieldName The name of the field that will store the list.
	 * @param fieldValue The values to store in the FIELD_NAME.
	 * @throws Exception
	 */
	public void appendToFieldList(Object docID, String fieldName, Object fieldValue) throws Exception 
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not update field "+fieldName);

		getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
				new BasicDBObject("$addToSet", new BasicDBObject(fieldName, fieldValue)));

	}


	/**
	 * Adds a tag to a document.
	 * Tags with prefix FOR are treated as control tags.
	 * 
	 * No check to ensure the existence of tag for efficiency reasons
	 * 
	 * @param docID The ID of doc to which the tag will be added.
	 * @param tagID The ID of the tag.
	 * @throws Exception
	 */
	public void addTagToDoc(Object docID, int tagID) throws Exception 
	{
		if(tagID<=0)
			throw new Exception("TagID should be >0");
		
		if(getTagProperty(tagID,TAG_PROPERTY_CONTROL) == -1)
		{
			getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$addToSet", new BasicDBObject(DOC_TAGS, tagID)));
		}
		else
		{
			getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$addToSet", new BasicDBObject(DOC_FOR_TAGS, tagID)));
		}
	}




	/**
	 * Adds the provided tags to the specified document.
	 * 
	 * @param docID The ID of the document to which the tags will be added.
	 * @param tagIDs A list of type List<Integer> with the IDs of the tags that will be added to the document.  
	 * @throws Exception
	 */
	public void addTagsToDoc(Object docID, List<Integer> tagIDs) throws Exception 
	{
		List<Integer> ctrlTags = new LinkedList<Integer>();
		List<Integer> normalTags = new LinkedList<Integer>();
		
		for(int tagID : tagIDs)
		{
			if(tagID<=0)
				throw new Exception("TagID should be >0");
			
			if(getTagProperty(tagID,TAG_PROPERTY_CONTROL) == -1) { 	//NOrmal Tag
				normalTags.add( tagID );
			}
			else {
				ctrlTags.add( tagID );
			}
		}

		if(normalTags.size()  > 0) {
			getCollDocs().update(new BasicDBObject(DOC_ID, docID),
				new BasicDBObject("$addToSet", new BasicDBObject(DOC_TAGS, 
						new BasicDBObject("$each", normalTags))));
		}
		
		if(ctrlTags.size() > 0)	{
			getCollDocs().update(new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$addToSet", new BasicDBObject(DOC_FOR_TAGS, 
							new BasicDBObject("$each", ctrlTags))));
		}
		
	}


	/**
	 * Gets the ID of the tag with given name.
	 * 
	 * @param tagName The string name of the tag.
	 * @return The TagID, or TAG_NOT_FOUND if no tag with such a name is found.
	 * @throws Exception
	 */
	public int getTagID(String tagName) throws Exception {
		BasicDBObject query = new BasicDBObject();
		query.put(TAG_NAME, tagName); 
		BasicDBObject res = (BasicDBObject) collTags.findOne(query);

		if(res==null)
			return TAG_NOT_FOUND;	

		return res.getInt(TAG_ID);
	}


	/**
	 * Gets the name of the tag with the specified tag ID.
	 *  
	 * @param tagID The ID of the tag.
	 * @return The string value that represents the tag. 
	 * @throws Exception
	 */
	public String getTagName(int tagID) throws Exception {
		BasicDBObject query = new BasicDBObject();
		query.put(TAG_ID, tagID); 
		BasicDBObject res = (BasicDBObject) collTags.findOne(query);

		if(res==null)
			return null;	

		return res.getString(TAG_NAME);
	}


	/**
	 * Gets a property of a tag.
	 * 
	 * @param tagID The ID of the tag.
	 * @param tagPropertyName The name of the property to get.
	 * @return The property value, or 0 if the property is not set (and saves the 0). 
	 * @throws Exception
	 */
	public int getTagProperty(int tagID, String tagPropertyName) throws Exception {
		BasicDBObject query = new BasicDBObject();
		query.put(TAG_ID, tagID); 
		BasicDBObject res = (BasicDBObject) collTags.findOne(query);

		if(res==null)
			return -1;	//Tag not found

		Object propValue = res.get(tagPropertyName);

		if(propValue==null)	 { //Tag found but property not set
			propValue = -1;
		}
		

		//		System.out.println("TagID="+ getTagName(tagID));
		//		System.out.println("tagProperty="+tagPropertyName);
		//		System.out.println("Value="+propValue);
		return (Integer)propValue;
	}

	/**
	 * Sets the property of a tag.
	 * Value -1 is reserved to mark unset property.
	 * 
	 * @param tagID The ID of the tag.
	 * @param tagPropertyName The name of the property to get.
	 * @param tagPropertyValue The value to set the property.
	 * 
	 * @throws Exception
	 */
	public void setTagProperty(int tagID, String tagPropertyName, int tagPropertyValue) throws Exception 
	{
		collTags.findAndModify(new BasicDBObject(TAG_ID,tagID), 
				new BasicDBObject("$set", new BasicDBObject(tagPropertyName, tagPropertyValue) ));
		return;
	}





	/**
	 * Searches for the document with the given ID.
	 * It returns the value of the specified fieldName.
	 *  
	 * @param docID
	 * @param fieldName
	 * @return Return the value of the field or NULL if not found
	 * 
	 * @throws Exception
	 */
	public Object findDocByIDGetField(Object docID, String fieldName) throws Exception
	{
		//SEARCH CACHE
		if((lastQueryResult!=null) && (lastQueryResult.get(BlackBoard.DOC_ID).equals( docID ) ))
		{
			return lastQueryResult.get( fieldName ); 
		}

		//SEARCH DB
		BBDoc res = findDocByID(docID);

		//NOT FOUND
		if(res == null)
			return DOC_NOT_FOUND;

		//FOUND
		return res.getField( fieldName );
	}


	/**
	 * Queries DB to retrieve all fields of a document.
	 * 
	 * @param docID The ID of the document to find.
	 * @return The BBDoc that contains all the information of the docuemnt. 
	 * @throws Exception
	 */
	public BBDoc findDocByID(Object docID) throws Exception
	{
		//SEARCH CACHE
		if( (lastQueryResult!=null) && (lastQueryResult.get(BlackBoard.DOC_ID).equals(docID)) )
			return new BBDoc(lastQueryResult);

		//SEARCH DB
		BasicDBObject where = new BasicDBObject();
		where.put(DOC_ID, docID); 
		BasicDBObject res = (BasicDBObject) getCollDocs().findOne(where);

		//	System.out.println(where.toString());

		if(res==null)
			return DOC_NOT_FOUND;

		lastQueryResult = res;

		return new BBDoc(lastQueryResult);
	}










	/*
	 * Finds documents based on specified parameters.
	 * 
	 * Set to null unused parameters or 0 for resPos/resSize. 
	 * @param withTags A list of type List<Integer> that specifies the tags that the returned documents should carry.
	 * @param withoutTags  A list of type List<Integer> that specifies the tags that the returned documents should not carry.
	 * @param resPos Ignore 
	 * @param resSize The maximum number of documents to return.
	 * @return A list of type List<Object with doc IDs.
	 * @throws Exception
	 */
	/*
	public List<Object> findDocs(
			List<Integer> withTags,
			List<Integer> withoutTags, 
			int resPos,
			int resSize)
			throws Exception  
			{
		List<Object> res = new LinkedList<Object>();

		BasicDBObject query = new BasicDBObject();
		//	if(fromDate!=null)
		//		query.put(DOC_DATE,new BasicDBObject("$gt",fromDate));
		//	if(toDate!=null)
		//		query.put(DOC_DATE,new BasicDBObject("$lt",toDate));
		if(withTags!=null)
			query.put(DOC_TAGS,new BasicDBObject("$all",withTags));
		if(withoutTags!=null)
			query.put(DOC_TAGS,new BasicDBObject("$nin",withoutTags));

		if(resPos!=0)
		{
			throw new Exception("resPos not implemented yet!");
		}

		if(resSize!=0)
		{
			for(DBCursor cur = getCollDocs().find(query, new BasicDBObject().append(DOC_ID,"1") ).limit(resSize); cur.hasNext(); )
				res.add(  ((BasicDBObject)cur.next()).get(DOC_ID)  );

		}
		else
		{
			for(DBCursor cur = getCollDocs().find(query, new BasicDBObject().append(DOC_ID,"1") ); cur.hasNext(); )
				res.add(  ((BasicDBObject)cur.next()).get(DOC_ID)  );
		}

		return res;
			}
	 */

	/**
	 * Get the tags of the document ID as a list of strings.
	 * 
	 * @param docID The ID of the document of interest.
	 * 
	 * @return Return list of tags as strings, or null if no tags are present. 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<String> getDocStringTags(Object docID) throws Exception
	{
		List<Integer> tagIDs = (List<Integer>) findDocByIDGetField( docID, DOC_TAGS);
		if(tagIDs==null)
			return null;

		List<String> tags = new LinkedList<String>();
		for(int i=0;i<tagIDs.size();i++)
		{
			tags.add( getTagName(tagIDs.get(i)));
		}

		return tags;
	}

	/**
	 * Inserts a new tag in the system.
	 * If Tag starts with FOR then it is considered a control tag.
	 * 
	 * @param tagName
	 * @return The new TagID or Exception if tag exists
	 * @throws Exception
	 */
	public synchronized int insertNewTag(String tagName) throws Exception 
	{
		Integer tagID = getTagID(tagName);
		if(tagID!=TAG_NOT_FOUND)	//Tag
			throw new Exception("Tag :"+tagName +" already exists.");


		//		GENERATE UNIQUE ID
		tagID = (Integer) GenerateUniqueTagID();

		BasicDBObject doc = new BasicDBObject();
		doc.put(TAG_ID, tagID );
		doc.put(TAG_NAME, tagName );

		collTags.insert(doc);

		if(tagName.startsWith(CONTROL_TAGS_PREFIX) || tagName.startsWith(CONTROL_TAGS_PREFIX2))
			setTagProperty(tagID, TAG_PROPERTY_CONTROL, 1 );
		else
			setTagProperty(tagID, TAG_PROPERTY_CONTROL, 0 );

		
		return getTagID(tagName);
	}	

	/**
	 * Admin use only.
	 * Sets the tag counter.
	 * 
	 * @param count
	 * @throws Exception
	 */
	public void setTagCounter(int count) throws Exception 
	{
		if(adminMode==false)
			throw new Exception("This function is for admin purposes only.");

		collCounter.findAndModify(new BasicDBObject("_id",NEXT_ID), 
				new BasicDBObject("$set", new BasicDBObject("tag_counter",count) ));
	}	


	/**
	 * Assumes tag is not present
	 * Inserts a new tag in the system.
	 * If Tag starts with FOR then it is considered a control tag.
	 * 
	 * @param tagName
	 * @return The new TagID or Exception if tag exists
	 * @throws Exception
	 */
	public synchronized int insertNewTag(String tagName, int tagID) throws Exception 
	{
		BasicDBObject doc = new BasicDBObject();
		doc.put(TAG_ID, tagID );
		doc.put(TAG_NAME, tagName );

		collTags.insert(doc);

		if(tagName.startsWith(CONTROL_TAGS_PREFIX) || tagName.startsWith(CONTROL_TAGS_PREFIX2))
		{
			setTagProperty(tagID, TAG_PROPERTY_CONTROL, 1 );
		}

		//UPDATE NEXT TAG ID

		DBObject c = collCounter.findOne( new BasicDBObject("_id",NEXT_ID) );

		int nextTagID = ((Integer) c.get("tag_counter")).intValue();

		if(tagID > nextTagID)
			setTagCounter(tagID + 1);


		return getTagID(tagName);
	}	

	/**
	 * For admin purposes only.
	 * @param tagNames
	 * @param tagIDs
	 * @throws Exception
	 */
	public synchronized void insertNewTagsFast(List<String> tagNames, List<Integer> tagIDs) throws Exception 
	{
		if(adminMode==false)
			throw new Exception("This function is for admin purposes only.");

		List<DBObject> tags = new LinkedList<DBObject>();

		int maxTagID = 0;
		int tagID = 0;

		for(int i=0;i<tagNames.size();i++)
		{
			tagID = tagIDs.get(i);
			if(tagID>maxTagID)
				maxTagID  = tagID;
			BasicDBObject tag = new BasicDBObject();
			tag.put(TAG_ID, tagID );
			tag.put(TAG_NAME, tagNames.get(i) );
			tags.add(tag);
		}

		collTags.insert(tags);

		//UPDATE NEXT TAG ID
		DBObject c = collCounter.findOne( new BasicDBObject("_id",NEXT_ID) );
		int nextTagID = ((Integer) c.get("tag_counter")).intValue();

		if(maxTagID > nextTagID)
			setTagCounter(maxTagID + 1);
	}	


	/**
	 * Returns all docIDs in the BlackBoard.
	 * 
	 * @return A list with docIDs
	 */
	public List<Object> getAllDocIDs()  throws Exception
	{
		List<Object> res = new LinkedList<Object>();

		BasicDBObject query = new BasicDBObject();

		for(DBCursor cur = getCollDocs().find(query, new BasicDBObject().append(DOC_ID,"1") );
				cur.hasNext(); )
			res.add(  cur.next().get(DOC_ID)  );			

		return res;
	}

	/**
	 * Returns all docs in the BlackBoard.
	 * 
	 * @return A BBDocSet with all docs.
	 */
	public BBDocSet getAllDocs()  throws Exception
	{
		DBCursor cur = getCollDocs().find();
		return new BBDocSet(cur);
	}



	/**
	 * Returns all tag IDs in the BlackBoard as a List<Integer>.
	 * 
	 * Results are up to 1000 tags. Use getAllTags() to get them all.
	 * 
	 * @return A list with docIDs
	 */
	@Deprecated
	public List<Integer> getAllTagIDs()  throws Exception
	{
		List<Integer> res = new LinkedList<Integer>();

		//	BasicDBObject query = new BasicDBObject();

		for(DBCursor cur = collTags.find().limit(1000);	cur.hasNext(); )
			res.add( ((BasicDBObject) cur.next()).getInt(TAG_ID)  );			

		return res;
	}

	/**
	 * Returns all tag names in the BlackBoard as a List<String>.
	 * 
	 * Results are up to 1000 tags. Use getAllTags() to get them all.
	 * 
	 * @return A list with docIDs
	 */
	@Deprecated
	public List<String> getAllTagNames()  throws Exception
	{
		List<String> res = new LinkedList<String>();

		//BasicDBObject query = new BasicDBObject();

		for(DBCursor cur = collTags.find().limit(1000); cur.hasNext(); )
			res.add( ((BasicDBObject) cur.next()).getString(TAG_NAME)  );			

		return res;
	}

	/**
	 * Returns a set of all tags in BB. The result contains both names and IDs of tags.
	 * 
	 * @return A BBDocTagSet object that contains all tags in the Black Board.
	 * @throws Exception
	 */
	public BBDocTagSet getAllTags()  throws Exception
	{
		DBCursor cur = collTags.find();

		return new BBDocTagSet(cur);
	}




	/**
	 * Removes a field from a document.
	 * 
	 * @param docID The ID of the doc from which we want to remove the field.
	 * @param fieldName	The name of field to be removed.
	 * @throws Exception
	 */
	public void removeFieldFromDoc(Object docID, String fieldName) throws Exception
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not remove field "+fieldName);

		getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
				new BasicDBObject("$unset",new BasicDBObject(fieldName, 1 )));
	}

	/**
	 * Removes a tag from a document.
	 * 
	 * @param docID The ID of the document from which we want to remove a tag.
	 * @param tagID The ID of the tag we want to remove.
	 * @throws Exception
	 */
	public void removeTagFromDoc(Object docID, int tagID) throws Exception
	{
		if(getTagProperty(tagID, TAG_PROPERTY_CONTROL)==1)
			getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$pull",new BasicDBObject(DOC_FOR_TAGS, tagID )));
		else
			getCollDocs().update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$pull",new BasicDBObject(DOC_TAGS, tagID )));


	}


	/**
	 * Removes all tags from doc.
	 * TODO: Improve performance 
	 * 
	 * @param docID
	 * @param tagIDs
	 * @throws Exception
	 */
	public void removeTagsFromDoc(Object docID, List<Integer> tagIDs) throws Exception 	{
		for(Integer tagID : tagIDs)
			removeTagFromDoc(docID, tagID);
	}
	
	
	/**
	 * Generates a unique Tag ID for inserting new tags
	 * @return
	 */
	synchronized Integer GenerateUniqueTagID() throws Exception	{
		DBObject id = collCounter.findAndModify(new BasicDBObject("_id",NEXT_ID), 
				new BasicDBObject("$inc", new BasicDBObject("tag_counter",1) ));

		return (Integer)id.get("tag_counter");
	}

	/**
	 * Returns true if the specified field can be updated by API, otherwise false.
	 * @param fieldName The name of the field to be checked.
	 * @return
	 */
	boolean isNotUpdateableField(String fieldName )
	{
		if(FINAL_FIELDS.contains(fieldName))
			return true;
		return false;
	}

	/**
	 * Converts a String representation of an ID to an Object ID.
	 * 
	 * @param stringID - The ID of a document as String
	 * @return	The ID of the document as Object
	 */
	public Object convertToID(String stringID)
	{
		ObjectId id = new ObjectId(stringID);

		return id;
	}


	/**
	 * Checks if index on field "fieldName" exists.
	 * If it doesn't it creates it.
	 * 
	 * @param fieldName The name of the field to be indexed.
	 * @throws Exception 
	 */
	public void ensureIndex(List<String> fieldNames) throws Exception
	{
		//if(!adminMode)
		//	throw new Exception("buildIndex" + BlackBoardsAPI.ERROR_FUNCTION_FOR_ADMIN );	

		BasicDBObject index;
		BasicDBObject options;

		System.out.print("Checking/ Index for field '");
		for(String fieldName : fieldNames)
			System.out.print(fieldName +" ");
		System.out.println("'on BB " + BB_NAME);

		index = new BasicDBObject();
		for(String fieldName : fieldNames)
			index.put(fieldName, 1);

		options = new BasicDBObject();
		options.put("background",true);
		getCollDocs().ensureIndex(index, options);
	}

	/**
	 * For admin usage only.
	 * Checks if all indexes are in place.
	 * 
	 * @throws Exception
	 */
	public void checkIndexes() throws Exception
	{
		if(!adminMode)
			throw new Exception("buildIndex" + BlackBoardsAPI.ERROR_FUNCTION_FOR_ADMIN );	

	}

	/**
	 * Searches for a document that has a field with a specific value.
	 * Then search the DB.
	 * 
	 * @param fieldName The name of the field. 
	 * @param fieldValue The required value of the field. 
	 * @return Returns a BBDoc instance or DOC_NOT_FOUND=null if no doc is found.
	 */
	public BBDoc findDocByFieldValue(String fieldName, Object fieldValue) throws Exception
	{
		//	SEARCH CACHE
		if(lastQueryResult!=null) {
			//	try{
			Object storedField = lastQueryResult.get( fieldName );	
			if(		(storedField!=null) && // Field may not be there
					(storedField.equals(fieldValue)) )
				return new BBDoc(lastQueryResult);
		}

		//SEARCH DB
		BasicDBObject query = new BasicDBObject();
		query.put(fieldName, fieldValue); 
		BasicDBObject res = (BasicDBObject) getCollDocs().findOne(query);


		//		NOT FOUND NEITHER IN DB OR CACHE
		if(res==null)
			return DOC_NOT_FOUND;	

		//		FOUND 
		lastQueryResult = res;		//SAVE RES TO CACHE
		return new BBDoc(lastQueryResult);
	}



	/**
	 * Searches for a document that has a field with a specific string value.
	 * Then search the DB.
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Returns a single docID or DOC_NOT_FOUND if no doc is found
	 */
	public Object findDocIDByFieldValue(String fieldName, Object fieldValue) throws Exception
	{
		//	SEARCH CACHE
		if(lastQueryResult!=null) {
			Object storedField = lastQueryResult.get(fieldName);	
			if(		(storedField!=null) && // Field may not be there
					(storedField.equals(fieldValue)) )
				return lastQueryResult.get( DOC_ID );
		}

		//SEARCH DB
		BasicDBObject query = new BasicDBObject();
		query.put(fieldName, fieldValue); 
		BasicDBObject res = (BasicDBObject) getCollDocs().findOne(query);

		//NOT FOUND NEITHER IN DB OR CACHE
		if(res==null)
			return DOC_NOT_FOUND;

		//FOUND IN DB
		lastQueryResult = res;		//SAVE RES TO CACHE

		return res.get( DOC_ID );
	}


	/**
	 * Removes the specified field from all documents.
	 * 
	 * @param fieldName The name of the field to be removed.
	 * @throws Exception
	 */
	public void removeFieldInAllDocs(String fieldName) throws Exception
	{
		if(adminMode==false)
			throw new Exception("This function is for admin purposes only.");

		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not remove field "+fieldName);


		List<String> withFields = new LinkedList<String>();
		withFields.add(fieldName);

		System.out.println("Searching for docs with field " + fieldName);
		List<Object> docIDs = findDocsByFieldsTags(
				withFields,
				null,
				null,
				null,
				0);

		System.out.println("Removing from "+docIDs.size());


		for(int i=0;i<docIDs.size();i++)
		{
			if(i%1000==0)
				System.out.printf("%.2f\n",i*100.0/docIDs.size());

			getCollDocs().update(	new BasicDBObject(DOC_ID,docIDs.get(i)),
					new BasicDBObject("$unset",new BasicDBObject(fieldName, 1 )));
		}
	}

	/**
	 * Searches for a document with specified annotations.
	 * The order of tags is important for speed: use the more rare tags first and the most frequent last. 
	 * 
	 * Set parameter to null to ignore fields.
	 * @param withFields A list of type List<String> with field names, that the documents returned should carry.
	 * @param withoutFields A list of type List<String> with field names, that the documents returned should not carry.
	 * @param withTags Return articles that have all specified tags.
	 * @param withoutTags  Return articles that have none of the specified tags.
	 * @param maxDocs Set to zero to get all. 
	 * @return A list with DocIDs as List<Object>.
	 * @throws Exception 
	 */
	public List<Object> findDocsByFieldsTags(	
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxDocs
			) throws Exception 
			{	
		BasicDBObject query = new  BasicDBObject();
		if(withFields!=null)
		{
			for(int i=0; i<withFields.size();i++)
				query.put( withFields.get(i), new BasicDBObject("$exists", true));
		}
		if(withoutFields!=null)
		{
			for(int i=0; i<withoutFields.size();i++)
				query.put( withoutFields.get(i), new BasicDBObject("$exists", false));
		}
		if(withTags!=null)
		{
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.put(DOC_TAGS,new BasicDBObject("$all", withTags));
		}
		if(withoutTags!=null)
		{
			if(getTagProperty(withoutTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$nin", withoutTags));
			else
				query.put(DOC_TAGS, new BasicDBObject("$nin", withoutTags));
		}


		BasicDBObject return_field = new BasicDBObject().append(DOC_ID, 1);

		List<Object> res = new LinkedList<Object>();
		if(maxDocs>0)
			for(DBCursor cur = getCollDocs().find(query, return_field).limit( maxDocs ) ;	cur.hasNext(); ) 
			{
				Object id =   ((BasicDBObject) cur.next()).get(DOC_ID); 
				res.add( id );
			}
		else
			for(DBCursor cur = getCollDocs().find(query, return_field) ;	cur.hasNext(); ) 
			{
				Object id =   ((BasicDBObject) cur.next()).get(DOC_ID); 
				res.add( id );
			}

		return res;
			}

	
	
	public BBDocSet findDocIDsByTags( 
			List<Integer> withTags,
			int maxDocs
			) throws Exception 
			{	
		BasicDBObject query = new  BasicDBObject();
		
		List<Integer> normalTags = new LinkedList<Integer>();
		List<Integer> ctrlTags = new LinkedList<Integer>();
		
		for(int tagID : withTags)
		{
			if(getTagProperty(tagID, TAG_PROPERTY_CONTROL)==1)
				ctrlTags.add( tagID );
			else
				normalTags.add(tagID);
		}
		
		if(normalTags.size() > 0)
		{
			query.put(DOC_TAGS,new BasicDBObject("$all", normalTags));
		}
		
		if(ctrlTags.size() > 0)
		{
			query.put(DOC_FOR_TAGS,new BasicDBObject("$all", ctrlTags));
		}
		
		
		BasicDBObject return_field = new BasicDBObject().append(DOC_ID, 1);

		
		
		DBCursor cur;
		if(maxDocs>0) 
			cur = getCollDocs().find(query, return_field).limit( maxDocs ); 
		else {
			cur = getCollDocs().find(query, return_field);
		}



		return new BBDocSet(cur);
	}


	/**
	 * Searches for documents that carry the specified tags.
	 * The order of tags is important for speed: use the more rare tags first and the most frequent last. 
	 * 
	 * Set parameter to null to ignore fields.
	 * @param withTags Return articles that have all specified tags.
	 * @param withoutTags  Return articles that have none of the specified tags.
	 * @param maxDocs Set to zero to get all. 
	 * @return A BBDocSet with the results.
	 * @throws Exception 
	 */
	public BBDocSet findDocsByTagsSet(	
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxDocs
			) throws Exception 
			{	
		BasicDBObject query = new  BasicDBObject();
		if(withTags!=null)
		{
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.put(DOC_TAGS,new BasicDBObject("$all", withTags));
		}
		if(withoutTags!=null)
		{
			if(getTagProperty(withoutTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$nin", withoutTags));
			else
				query.put(DOC_TAGS, new BasicDBObject("$nin", withoutTags));
		}

		//System.out.println( query );

		DBCursor cur = null;
		if(maxDocs>0)
			cur = getCollDocs().find(query).limit( maxDocs );
		else
			cur = getCollDocs().find(query);
		
		System.out.println( cur );

		return new BBDocSet(cur);
			}

	/**
	 * Returns the n=maxDocs docs with highest values in field.
	 * 
	 * If field is not indexed, an index is created.
	 * 
	 * @param field
	 * @param maxDocs
	 * @return
	 * @throws Exception
	 */
	public BBDocSet findDocsOrderedByFieldValue(	
			String fieldName,
			int maxDocs
			) throws Exception 
			{	
		//Ensures index...
		System.out.print("Checking index...");
		BasicDBObject index = new BasicDBObject();
		index.put(fieldName, -1);
		BasicDBObject options = new BasicDBObject();
		options.put("background",true);
		getCollDocs().ensureIndex(index, options);
		System.out.println("DONE");


		DBCursor cur = null;
		if(maxDocs>0)
			cur = getCollDocs().find().sort(new BasicDBObject( fieldName , -1)).limit( maxDocs );
		else
			cur = getCollDocs().find().sort(new BasicDBObject( fieldName , -1)); 

		return new BBDocSet(cur);
			}

	/**
	 * Return the docs that are annotated with all specified tags, ordered by fieldName.
	 * @param withTagIDs
	 * @param fieldName
	 * @param maxDocs
	 * @return
	 * @throws Exception
	 */
	public BBDocSet findDocsOrderedByFieldValueHavingTags(
			List<Integer> withTagIDs,
			String fieldName,
			int maxDocs
			) throws Exception 
			{	
		if(withTagIDs == null)
			throw new Exception("findDocsOrderedByFieldValueHavingTags needs at least one tag");

		//Ensures index...
		int indexHash = (DOC_TAGS+fieldName).hashCode();
		if(!indexHashExists(indexHash) ) {
			System.out.print("Checking index:" + DOC_TAGS +" and " + fieldName);
			BasicDBObject index = new BasicDBObject();
			index.put(DOC_TAGS, 1);
			index.put(fieldName, -1);
			BasicDBObject options = new BasicDBObject();
			options.put("background",true);
			getCollDocs().ensureIndex(index, options);
			System.out.println("DONE");
			addIndexHash( indexHash );
		}



		BasicDBObject query = new  BasicDBObject();

		query.put(DOC_TAGS,new BasicDBObject("$all", withTagIDs));

		DBCursor cur = null;
		if(maxDocs>0)
			cur = getCollDocs().find(query).sort(new BasicDBObject( fieldName , -1)).limit( maxDocs );
		else
			cur = getCollDocs().find(query).sort(new BasicDBObject( fieldName , -1)); 

		return new BBDocSet(cur);
			}


	/**
	 * Generates a unique ID for inserting new objects
	 * @return
	 */
	synchronized Integer GenerateUniqueDocID() throws Exception
	{
		DBObject id = collCounter.findAndModify(new BasicDBObject("_id", NEXT_ID), 
				new BasicDBObject("$inc", new BasicDBObject("doc_counter",1) ));

		return (Integer)id.get("doc_counter");
	}

	/**
	 * Renames a field in all docs in the BlackBoard.
	 * This can be a slow process.
	 * 
	 * @param oldFieldName The name of the field that will be renamed.
	 * @param newFieldName The new name of the field.
	 */
	public void renameFieldInAllDocs(String oldFieldName, String newFieldName) throws Exception
	{
		if(isNotUpdateableField(oldFieldName))
			throw new Exception("Can not rename field "+oldFieldName);
		if(isNotUpdateableField(newFieldName))
			throw new Exception("Can not rename that field"+ newFieldName);

		List<String> withFields = new LinkedList<String>();
		withFields.add(oldFieldName);

		List<Object> docIDs = findDocsByFieldsTags(
				withFields,
				null,
				null,
				null,
				0);


		for(int i=0;i<docIDs.size();i++)
		{
			if(i%1000==0)
				System.out.printf("%.2f\n",i*100.0/docIDs.size());

			getCollDocs().update(	new BasicDBObject(DOC_ID,docIDs.get(i)),
					new BasicDBObject("$rename",new BasicDBObject(oldFieldName,newFieldName)));
		}
	}


	/**
	 * Inserts new doc in Black Board.
	 * If the doc doesn't have an ID, one is added. The new ID is accessed in doc.
	 * If the doc has ID, if there is an existing doc in DB with same ID it is replaced.
	 * 
	 * No other checks of any kind.
	 * 
	 * @param doc The document to insert
	 * @throws Exception
	 */
	public void insertNewDoc(BBDoc doc) throws Exception 
	{
		if(!this.getClass().getName().equals("macsy.blackBoardsSystem.BlackBoard") )
			throw new Exception("Function insertNewDoc() can be used only from BlackBoard class.");

		getCollDocs().insert( doc.dataObject , new WriteConcern(true));
	}

	/**
	 * Inserts new docs in Black Board.
	 * If the doc doesn't have an ID, one is added. The new ID is accessed in doc.
	 * If the doc has ID, if there is an existing doc in DB with same ID it is replaced.
	 * 
	 * No other checks of any kind.
	 * 
	 * @param doc The document to insert
	 * @throws Exception
	 */

	public void insertNewDocs(List<BBDoc> docs) throws Exception 
	{
		if(!this.getClass().getName().equals("macsy.blackBoardsSystem.BlackBoard") )
			throw new Exception("Function insertNewDocs() can be used only from BlackBoard class.");

		List<DBObject> docsData = new ArrayList<DBObject>();
		for(BBDoc doc : docs)
			docsData.add(doc.dataObject);

		getCollDocs().insert( docsData , new WriteConcern(true));
	}


	/**
	 * Returns the number of docs that have all stated tags.
	 * @param withTags
	 * @return
	 * @throws Exception
	 */
	public long countDocs(List<Integer> withTags) throws Exception  
	{
		if(withTags==null)
			return this.getNumberOfDocs();

		BasicDBObjectBuilder query = new BasicDBObjectBuilder();

		if(withTags!=null) {
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.add(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.add(DOC_TAGS,new BasicDBObject("$all",withTags));
		}

		//	System.out.println( query.get() );

		long count = getCollDocs().count(query.get());			

		return count;
	}

	public void removeDoc(Object docID) throws Exception
	{
		if(adminMode==false)
			throw new Exception("This function is for admin purposes only.");

		getCollDocs().remove( new BasicDBObject(DOC_ID, docID) );

	}

	/**
	 * Finds a set of docs that contain a list-field named "listFieldName" that contains the value "listFieldValue".
	 * 
	 * @param listFieldName : The list of names of the fields of interest.
	 * @param listFieldValue : The list of values of the fields of interest.
	 * @param withTagIDs : A list of type  List<Integer> with the IDs of the tags that articles should carry.
	 * @param withoutTagIDs : A list of type  List<Integer> with the IDs of the tags that articles should not carry.
	 * @param resSize : The maximum size of articles to return - Max is set to MAX_ARICLEIDS_IN_RESULT_LIST.
	 * @return A BBDocSet of results.
	 * @throws Exception
	 */
	public BBDocSet findDocsWithFieldNamesAndValues(
			String listFieldName, 
			Object listFieldValue,
			List<Integer> withTagIDs,
			List<Integer> withoutTagIDs, 
			int resSize
	) throws Exception  
	{
//		LinkedList<Object> res = new LinkedList<Object>();
		BasicDBObject query = new BasicDBObject();

		//We want the listFieldName to contain the listFieldValue
		List<Object> listOfValues = new LinkedList<Object>();
		listOfValues.add(listFieldValue);
		query.put( listFieldName, new BasicDBObject("$in",listOfValues));
		

		if(withTagIDs!=null)
			query.put(DOC_TAGS,new BasicDBObject("$all",withTagIDs));
		if(withoutTagIDs!=null)
			query.put(DOC_TAGS,new BasicDBObject("$nin",withoutTagIDs));

		//System.out.println(query);
	
		if(resSize!=0)
		{
			DBCursor cur = getCollDocs().find(query).limit(resSize);
			return new BBDocSet(cur);

		}
		else
		{
			DBCursor cur = getCollDocs().find(query);
			return new BBDocSet(cur);
		}
	}

	
}
