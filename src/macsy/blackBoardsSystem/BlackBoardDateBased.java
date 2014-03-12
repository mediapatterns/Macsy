package macsy.blackBoardsSystem;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MapReduceCommand;
import com.mongodb.MongoException;

/**
 * The BlackBoardDateID is used as base class for BBs that use time-stamps as IDs.
 * Each document has as ID its creation time-stamp.
 * Also the table is horizontally separated based on Year.
 * 
 * @author      Ilias Flaounas <iliasfl@bris.ac.uk>
 * @version     1.0                   
 * @since       2012-06-23
 * 
 */
public class BlackBoardDateBased extends BlackBoard {


	//These are set to correct values by constructor.
	int MAX_ARTICLES_YEAR_IN_DB = 1900;//MAXIMUM  
	int MIN_ARTICLES_YEAR_IN_DB = 2100;//BASE		//These are fixed in constructor

	//Define the sorting of some searching // for internal use only
	static final int ORDER_NONE = 0;
	static final int ORDER_RECENT_FIRST = -1;
	static final int ORDER_OLD_FIRST = 1;

	/**
	 * Returns the collection of the specified year.
	 * It returns a collection object even if data do not exist at that year (this is useful in order to add data to a new year).
	 * 
	 * @param yearOfInterest The year of interest.  
	 * @return A collection of the data for the specified year.
	 */
	protected DBCollection getCollDocs(int yearOfInterest)
	{
		DBCollection coll = collDocs.get(yearOfInterest);
		if(coll == null)
		{
			coll =  mongo_db.getCollection(BB_NAME + "_" + yearOfInterest);
			coll.setObjectClass(BasicDBObject.class);
			collDocs.put(yearOfInterest, coll);
		}
		return coll;
	}


	/**
	 * Constructor
	 * @param mongo_database
	 * @param blackBoardName
	 * @param BulkJob
	 * @throws Exception
	 */
	BlackBoardDateBased(DB mongo_database,  String blackBoardName, boolean adminMode ) throws Exception 
	{
		super(mongo_database, blackBoardName, adminMode);

		//Check how many years are really in DB
		Set<String> existingDBs = mongo_db.getCollectionNames();
		for(String coll_n : existingDBs)
		{
			if(coll_n.startsWith(BB_NAME))
			{
				String suffix = coll_n.split("_")[1];
				try
				{
					int year = Integer.parseInt(suffix);
					if(year>MAX_ARTICLES_YEAR_IN_DB)
						MAX_ARTICLES_YEAR_IN_DB = year;
					if(year<MIN_ARTICLES_YEAR_IN_DB)
						MIN_ARTICLES_YEAR_IN_DB = year;
				}
				catch(NumberFormatException e)
				{
					//Not number. //Ignore 
				}
			}
		}
	}



	/**
	 * Returns the Year that the Object ID refers to.
	 * 
	 * @param docID The ID of a document
	 * @return The year part of the input ID.
	 */
	int getYearOfInterestByDocID(Object docID)
	{
		Calendar dt = new GregorianCalendar();
		dt.setTimeInMillis(((ObjectId)docID).getTime() );
		return dt.get( Calendar.YEAR);
	}

	/**
	 * Returns the Year of the Date.
	 * @param date A date of interest.
	 * @return The year part of the input date.
	 */
	int getYearOfInterestByDate(Date date)
	{
		Calendar dt = new GregorianCalendar();
		dt.setTimeInMillis( date.getTime() );
		return dt.get( Calendar.YEAR);
	}


	/**
	 * Searches  for a document that has a field with a specific value.
	 * 
	 * @param fromDate The start of the time period of interest.
	 * @param toDate  The end of the time period of interest.
	 * @param fieldName The name of the field we are interest in.
	 * @param fieldValue The value of the field.
	 * @return Returns a single docID or DOC_NOT_FOUND if no doc is found
	 */
	public final Object findDocIDByFieldValue(
			Date fromDate,
			Date toDate, 
			String fieldName, 
			Object fieldValue) throws Exception
			{
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(fromDate);
		int YearOfInterest =  cal.get(Calendar.YEAR);

		//	SEARCH CACHE
		if(lastQueryResult!=null) {
			Object storedField = lastQueryResult.get(fieldName);	
			if(		(storedField!=null) && // Field may not be there
					(storedField.equals(fieldValue)) )
				return lastQueryResult.get( DOC_ID );
		}

		//SEARCH DB
		BasicDBObject query = new BasicDBObject();

		ObjectId fromID = null;
		ObjectId toID = null; 
		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;
			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.put(DOC_ID, range );
		}
		else if(fromID!=null)
			query.put(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.put(DOC_ID,new BasicDBObject("$lt",toID));


		query.put(fieldName, fieldValue); 
		BasicDBObject res = (BasicDBObject) getCollDocs(YearOfInterest).findOne(query);

		//NOT FOUND NEITHER IN DB OR CACHE
		if(res==null)
			return DOC_NOT_FOUND;
		//FOUND IN DB
		lastQueryResult = res;		//SAVE RES TO CACHE

		return res.get( DOC_ID );
			}

	/**
	 * Searches  for a document that has a field with a specific value.
	 *
	 * @param yearOfInterest
	 * @param fieldName The name of the field we are interest in.
	 * @param fieldValue The value of the field.
	 * @return Returns a single docID or DOC_NOT_FOUND if no doc is found
	 */
	public final Object findDocIDByFieldValue(
			int yearOfInterest, 
			String fieldName, 
			Object fieldValue) throws Exception
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
		BasicDBObject res = (BasicDBObject) getCollDocs(yearOfInterest).findOne(query);

		//NOT FOUND NEITHER IN DB OR CACHE
		if(res==null)
			return DOC_NOT_FOUND;
		//FOUND IN DB
		lastQueryResult = res;		//SAVE RES TO CACHE

		return res.get( DOC_ID );
			}


	/**
	 * Searches DB for article with a field that has specific value.
	 * It only search for the articles of the year in specified Date.
	 * 
	 * @param fromDate
	 * @param fieldName
	 * @param fieldValue
	 * @return An article that has the field with specific value. 
	 * @throws Exception
	 */
	public final BBDoc findDocByFieldValue(Date fromDate,String fieldName, Object fieldValue) throws Exception
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
		BasicDBObject res = (BasicDBObject) getCollDocs(getYearOfInterestByDate(fromDate)).findOne(query);

//		NOT FOUND NEITHER IN DB OR CACHE
		if(res==null)
			return (BBDoc)DOC_NOT_FOUND;	

//		FOUND 
		lastQueryResult = res;		//SAVE RES TO CACHE
		return new BBDoc(lastQueryResult);
	}


	/**
	 * Returns the total number of docs in the BlackBoard.
	 * 
	 * @return The number of docs as integer.
	 * @throws Exception
	 */
	public int getNumberOfDocs() throws Exception {
		int total = 0;

		for(int year = MIN_ARTICLES_YEAR_IN_DB; year <= MAX_ARTICLES_YEAR_IN_DB; year++)
		{
			total += getCollDocs(year).count();
		}

		return total;
	}


	/**
	 * Returns the date of the first document in BB.
	 * 
	 * @return The date.
	 * 
	 * @throws Exception
	 */
	public final Date getMinDocDate() throws Exception {

		DBCursor cur = getCollDocs(MIN_ARTICLES_YEAR_IN_DB).find().sort(new BasicDBObject("_id",1)).limit(1);

		if(cur==null)
			return null;

		BasicDBObject art = (BasicDBObject) cur.next();

		if(art==null)
			return null;

		return new Date(art.getObjectId( BlackBoard.DOC_ID ).getTime());
	}

	/**
	 * Returns the date of the last document in BB.
	 * @return The date of the last document.
	 * @throws Exception
	 */
	public final Date getMaxDocDate() throws Exception {

		DBCursor cur = getCollDocs(MAX_ARTICLES_YEAR_IN_DB).find().sort(new BasicDBObject("_id",-1)).limit(1);

		if(cur==null)
			return null;

		BasicDBObject art = (BasicDBObject) cur.next();

		if(art==null)
			return null;

		return new Date(art.getObjectId( BlackBoard.DOC_ID ).getTime());
	}

	/**
	 * Search the CACHE for a document that has a field with a specific string value.
	 * Then search the DB.
	 * 
	 * @param fieldName
	 * @param fieldValue
	 * @return Returns a single docID or DOC_NOT_FOUND if no doc is found
	 */
	@Deprecated
	public BBDoc findDocByFieldValue(String fieldName, Object fieldValue) throws Exception
	{
		throw new Exception("Function not applicable for articles BB. Date must be specified");
	}

	@Deprecated
	public Object findDocIDByFieldValue(String fieldName, Object fieldValue) throws Exception
	{
		throw new Exception("Function not applicable for articles BB. Date must be specified");
	}

	@Deprecated
	public void removeFieldInAllDocs(String fieldName) throws Exception
	{
		throw new Exception("Function not applicable for articles BB. Date must be specified");
	}

	@Deprecated
	public List<Object> findDocsByFieldsTags(	
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxDocs
	) throws Exception 
	{
		throw new Exception("Function not applicable for articles BB. Date must be specified");		
	}


	@Deprecated
	public void renameFieldInAllDocs(String oldFieldName, String newFieldName) throws Exception
	{
		throw new Exception("Function not applicable for articles BB. Date must be specified");		
	}


	/**
	 * Gets all the contents of an article.
	 *  
	 * @param docID The ID of the article to retrieve. 
	 * @return The result of the query as a BBDocArticle, or BlackBoard.DOC_NOT_FOUND if no 
	 * article was found.  
	 * @throws Exception
	 */
	public BBDoc findDocByID(Object docID) throws Exception
	{
		//SEARCH CACHE
		if( (lastQueryResult!=null) && (lastQueryResult.get( BlackBoard.DOC_ID).equals(docID)) )
			return new BBDoc(lastQueryResult);

		//SEARCH DB
		BasicDBObject where = new BasicDBObject();
		where.put(DOC_ID, docID); 
		BasicDBObject res = (BasicDBObject) getCollDocs(getYearOfInterestByDocID(docID)).findOne(where);

		if(res==null)
			return (BBDoc)BlackBoard.DOC_NOT_FOUND;

		lastQueryResult = res;

		return new BBDoc(lastQueryResult);
	}



	/**
	 * Adds a tag to a document.
	 * 
	 * No check to ensure the existance of tag for efficiency reasons
	 * 
	 * @param docID The ID of doc to which the tag will be added.
	 * @param tagID The ID of the tag.
	 * @throws Exception
	 */
	public void addTagToDoc(Object docID, int tagID) throws Exception 
	{
		if(getTagProperty(tagID,TAG_PROPERTY_CONTROL) == 1)
		{
			getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$addToSet", new BasicDBObject(DOC_FOR_TAGS, tagID)));
		}
		else
		{
			getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$addToSet", new BasicDBObject(DOC_TAGS, tagID)));
		}
	}


	/**
	 *  Removes a field from an article
	 *  	 
	 *  @param  docID The ID of the article from which the field will be removed.
	 *  @param fieldName The name of the field to be removed.
	 */
	public void removeFieldFromDoc(Object docID, String fieldName) throws Exception
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not remove field "+fieldName);

		getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
				new BasicDBObject("$unset",new BasicDBObject(fieldName, 1 )));

	}


	@Override
	public void removeTagFromDoc(Object docID, int tagID) throws Exception
	{
		if(getTagProperty(tagID, TAG_PROPERTY_CONTROL)==1)
			getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$pull",new BasicDBObject(DOC_FOR_TAGS, tagID )));
		else
			getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
					new BasicDBObject("$pull",new BasicDBObject(DOC_TAGS, tagID )));

	}
	
	
	
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
			throw new Exception("Can not append to field "+fieldName);

		getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID,docID),
				new BasicDBObject("$set",new BasicDBObject(fieldName,fieldValue)));
	}

	/**
	 * Adds a new real-value field to a document.
	 * 
	 * If field exists, it is updated to the provided value.
	 * 
	 * @param docID The ID of the doc which will be updated.
	 * @param fieldName The name of the field.
	 * @param fieldValue The value to set to the named field.
	 */
	public void addRealFieldToDoc(Object docID,String fieldName, double fieldValue) throws Exception
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not append to field "+fieldName);

		if(Double.isNaN(fieldValue) || Double.isInfinite(fieldValue))
			throw new Exception("NaN is not acceptable");

		getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID,docID),
				new BasicDBObject("$set",new BasicDBObject(fieldName,fieldValue)));
	}



	/**
	 * Adds a new field of type List to the document,
	 * and it populates it with the provided elements.
	 * 
	 * If the field already exists, the values of the fieldValue are appended to 
	 * the existing ones.
	 * 
	 * @param docID	The ID of the document which will be updated.
	 * @param fieldName The name of the field that will store the list.
	 * @param fieldValue The values to store in the FIELD_NAME.
	 * @throws Exception
	 */
	public void appendToFieldList(Object docID, String fieldName, Object fieldValue) throws Exception 
	{
		if(isNotUpdateableField(fieldName))
			throw new Exception("Can not append to field "+fieldName);

		getCollDocs(getYearOfInterestByDocID(docID)).update(	new BasicDBObject(DOC_ID, docID),
				new BasicDBObject("$addToSet", new BasicDBObject(fieldName, fieldValue)));

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
		for(int t  : tagIDs)
			addTagToDoc(docID,t);
	}



	/**
	 * Returns a set of docs within selected time period, and with fieldName
	 *  having a  specific value.
	 * 
	 * @param fromDate Begin of time period of interest.
	 * @param toDate End of time period of interest.
	 * @param fieldName The name of field of interest.
	 * @param fieldValue The value we want to have the field of interest
	 * @return A BBDocSet
	 * @throws Exception
	 */
	public BBDocSet findDocsByFieldValueSet(
			Date fromDate,
			Date toDate,
			String fieldName,
			Object fieldValue) throws Exception
			{
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(fromDate);
		int YearOfInterest =  cal.get(Calendar.YEAR);


		//SEARCH DB
		BasicDBObject query = new BasicDBObject();

		ObjectId fromID = null;
		ObjectId toID = null; 
		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;
			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.put(DOC_ID, range );
		}
		else if(fromID!=null)
			query.put(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.put(DOC_ID,new BasicDBObject("$lt",toID));


		query.put(fieldName, fieldValue); 
		DBCursor res = getCollDocs(YearOfInterest).find(query);

		return new BBDocSet(res);
			}


//	/**
//	* Finds articles with specific annotations. 
//	* 
//	* You can specify: a) spacific time period, 
//	* b) and that have some annotations, c) and they miss
//	* some other annotations, d) and they have all of specified tags, 
//	* e) and and they don't have any of the specified tags. 
//	* 
//	* Retrives articles with dates: fromDate <= article_date < toDate
//	* 
//	* If a tag has the prefix FOR, then all tags are assumed to be control tags.
//	* 
//	* @param fromDate The start of the period of interest.
//	* @param toDate  The end of the period of interest.
//	* @param withFields A list of type List<String> that specifies the fileds that the article should have.
//	* @param withoutFields A list of type List<String> that specifies the fields that the article should not have.
//	* @param withTags A list of type  List<Integer> with the IDs of the tags that articles should carry. 
//	* @param withoutTags A list of type  List<Integer> with the IDs of the tags that articles should not carry.
//	* @param maxArticles The maximum number of articles to return (Upper limit = MAX_ARICLEIDS_IN_RESULT_LIST).
//	* @return List<Object> with IDs of the articles that match criteria.
//	*/
//	public BBDocSet findRecentDocsByFieldsTagsSet(
//	Date fromDate,
//	Date toDate,
//	List<String> withFields,
//	List<String> withoutFields,
//	List<Integer> withTags,
//	List<Integer> withoutTags,
//	int maxArticles	
//	)
//	throws Exception
//	{

//	List<DBCursor> results = new LinkedList<DBCursor>(); 

//	//if dates are not specified
//	if((fromDate==null) && (toDate==null))
//	{
//	fromDate = getMinDocDate();
//	toDate = getMaxDocDate();

//	int fromYear = getYearOfInterestByDate(fromDate);
//	int toYear = getYearOfInterestByDate(toDate);

//	for(int year = toYear; year>=fromYear; year--)
//	{
//	DBCursor res = findRecentDocsByFieldsTagsSetFullYear(year, withFields,withoutFields, withTags, withoutTags,maxArticles );
//	results.add(res);
//	}
//	}
//	else	// There are dates
//	{
//	int fromYear = getYearOfInterestByDate(fromDate);
//	int toYear = getYearOfInterestByDate(toDate);

//	//Dates within same year
//	if(fromYear == toYear)
//	{
//	DBCursor r = findRecentDocsByFieldsTagsSetSingleYear(	fromDate,
//	toDate,withFields, withoutFields,
//	withTags,	withoutTags,maxArticles );

//	//return returnType.cast(new BBDocSet(r) );
//	return new BBDocSet(r);
//	}


//	//3phases:
//	//A) startORDER_NONEing date until 31/12/year 

//	Date endFirstYear = new GregorianCalendar( 	
//	fromYear,11,31,	23,59,59).getTime();

//	results.add( 
//	findDocsByFieldsTagsSetSingleYear(	fromDate,
//	endFirstYear,
//	withFields, withoutFields,
//	withTags,	withoutTags,maxArticles )
//	);

//	//B) sum all full years 
//	for(int year = fromYear+1; year<=toYear-1; year++)
//	{
//	results.add( findDocsByFieldsTagsSetFullYear(year, 
//	withFields,withoutFields, withTags, withoutTags,maxArticles ) );
//	}

//	//C) rest of year
//	Date startFinalYear = new GregorianCalendar( 	
//	toYear,0,1,	0,0,0).getTime();

//	results.add(
//	findDocsByFieldsTagsSetSingleYear(	startFinalYear,
//	toDate,
//	withFields, withoutFields,
//	withTags,	withoutTags,maxArticles )
//	);

//	}

//	return new BBDocSet(results);
//	}





	/**
	 * Finds articles with specific annotations. 
	 * 
	 * You can specify: a) spacific time period, 
	 * b) and that have some annotations, c) and they miss
	 * some other annotations, d) and they have all of specified tags, 
	 * e) and and they don't have any of the specified tags. 
	 * 
	 * Retrives articles with dates: fromDate <= article_date < toDate
	 * 
	 * If a tag has the prefix FOR, then all tags are assumed to be control tags.
	 * 
	 * @param fromDate The start of the period of interest.
	 * @param toDate  The end of the period of interest.
	 * @param withFields A list of type List<String> that specifies the fileds that the article should have.
	 * @param withoutFields A list of type List<String> that specifies the fields that the article should not have.
	 * @param withTags A list of type  List<Integer> with the IDs of the tags that articles should carry. 
	 * @param withoutTags A list of type  List<Integer> with the IDs of the tags that articles should not carry.
	 * @param maxArticles The maximum number of articles to return (Upper limit = MAX_ARICLEIDS_IN_RESULT_LIST).
	 * @return List<Object> with IDs of the articles that match criteria.
	 */
	public BBDocSet findDocsByFieldsTagsSet(
			Date fromDate,
			Date toDate,
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxArticles	
	)
	throws Exception
	{
		List<DBCursor> results = new LinkedList<DBCursor>(); 

		//if dates are not specified
		if((fromDate==null) && (toDate==null))
		{
			fromDate = getMinDocDate();
			toDate = getMaxDocDate();

			int fromYear = getYearOfInterestByDate(fromDate);
			int toYear = getYearOfInterestByDate(toDate);

			for(int year = fromYear; year<=toYear; year++)
			{
				DBCursor res = findDocsByFieldsTagsSetFullYear(
						year, withFields,withoutFields, withTags, withoutTags,maxArticles, ORDER_NONE );
				results.add(res);
			}
		}
		else	// There are dates
		{
			int fromYear = getYearOfInterestByDate(fromDate);
			int toYear = getYearOfInterestByDate(toDate);

			if(fromYear == toYear)
			{
				DBCursor r = findDocsByFieldsTagsSetSingleYear(	fromDate,
						toDate,withFields, withoutFields,
						withTags,	withoutTags,maxArticles, ORDER_NONE );

				//return returnType.cast(new BBDocSet(r) );
				return new BBDocSet(r);
			}


			//3phases:
			//A) starting date until 31/12/year 

			Date endFirstYear = new GregorianCalendar( 	
					fromYear,11,31,	23,59,59).getTime();

			results.add( 
					findDocsByFieldsTagsSetSingleYear(	fromDate,
							endFirstYear,
							withFields, withoutFields,
							withTags,	withoutTags,maxArticles, ORDER_NONE )
			);

			//B) sum all full years 
			for(int year = fromYear+1; year<=toYear-1; year++)
			{
				results.add( findDocsByFieldsTagsSetFullYear(year, 
						withFields,withoutFields, withTags, withoutTags,maxArticles , ORDER_NONE) );
			}

			//C) rest of year
			Date startFinalYear = new GregorianCalendar( 	
					toYear,0,1,	0,0,0).getTime();

			results.add(
					findDocsByFieldsTagsSetSingleYear(	startFinalYear,
							toDate,
							withFields, withoutFields,
							withTags,	withoutTags,maxArticles, ORDER_NONE )
			);

		}

		return new BBDocSet(results);
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
	@Override
	public BBDocSet findDocsByTagsSet(	
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxDocs
	) throws Exception 
	{	
		return this.findDocsByFieldsTagsSet(null,null, null, null, withTags, withoutTags, maxDocs);
	}

	/**
	 * Use when dates are from the same year.
	 * @param fromDate
	 * @param toDate
	 * @param withFields
	 * @param withoutFields
	 * @param withTags
	 * @param withoutTags
	 * @param maxArticles
	 * @return
	 * @throws Exception
	 */
	DBCursor findDocsByFieldsTagsSetSingleYear(	
			Date fromDate,
			Date toDate,
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxArticles,	
			int order
	)
	throws Exception
	{	
		if(withTags!=null && withoutTags!=null)
			throw new Exception("findDocsByFieldsTagsSetSingleYear() should select either withTags or withoutTags");

		assert ( order==ORDER_NONE || order == ORDER_RECENT_FIRST || order == ORDER_OLD_FIRST  ) : "Wrong order in  findDocsByFieldsTagsSetSingleYear()";


		ObjectId fromID = null;
		ObjectId toID = null; 

		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);


		BasicDBObject query = new  BasicDBObject();

		int YearOfInterest = -1;
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;
			//		System.out.println("Year of interest="+YearOfInterest);

			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.put(DOC_ID, range );
		}
		else if(fromID!=null)
			query.put(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.put(DOC_ID,new BasicDBObject("$lt",toID));

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
			//Break tags in two lists // One for normal tags and one for Control Tags
			List<Integer> withTagsNormal = new LinkedList<Integer>();
			List<Integer> withTagsFor = new LinkedList<Integer>();
			for(int t=0;t<withTags.size();t++)
			{
				if(getTagProperty(withTags.get(t), TAG_PROPERTY_CONTROL)==1)
					withTagsFor.add(withTags.get(t));
				else
					withTagsNormal.add(withTags.get(t));
			}

			if(withTagsFor.size()>0)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTagsFor));
			if(withTagsNormal.size()>0)
				query.put(DOC_TAGS,new BasicDBObject("$all", withTagsNormal));

			//	if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
			//		query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			//	else
			//		query.put(DOC_TAGS,new BasicDBObject("$all", withTags));
		}
		if(withoutTags!=null)
		{
			if(getTagProperty(withoutTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$nin", withoutTags));
			else
				query.put(DOC_TAGS, new BasicDBObject("$nin", withoutTags));
		}


		DBCursor cursor = null;

		if(order == ORDER_NONE)
		{
			if(maxArticles>0)
				cursor = getCollDocs(YearOfInterest).find(query).limit( maxArticles ); 
			else
				cursor = getCollDocs(YearOfInterest).find(query);
		}
		else if((order == ORDER_RECENT_FIRST ) ||(order == ORDER_OLD_FIRST))	//recent
		{
			if(maxArticles>0)
				cursor = getCollDocs(YearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,order)).limit( maxArticles ); 
			else
				cursor = getCollDocs(YearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,order));
		}

		return cursor;
	}

	DBCursor findDocsByDatesSingleYearOnlyTags(	
			Date fromDate,
			Date toDate,
			int maxArticles
	)
	throws Exception
	{	
		ObjectId fromID = null;
		ObjectId toID = null; 

		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);


		BasicDBObject query = new  BasicDBObject();

		int YearOfInterest = -1;
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;
			//		System.out.println("Year of interest="+YearOfInterest);

			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.put(DOC_ID, range );
		}
		else if(fromID!=null)
			query.put(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.put(DOC_ID,new BasicDBObject("$lt",toID));

		DBCursor cursor = null;

		BasicDBObject results = new BasicDBObject();
		results.put( DOC_TAGS ,1);
		results.put( DOC_FOR_TAGS ,1);
		
		
		if(maxArticles>0)
			cursor = getCollDocs(YearOfInterest).find(query).limit( maxArticles ); 
		else
			cursor = getCollDocs(YearOfInterest).find(query);
		

		return cursor;
	}

	
	/**
	 * Get all docs from specified year.
	 * @param yearOfInterest
	 * @param withFields
	 * @param withoutFields
	 * @param withTags
	 * @param withoutTags
	 * @param maxArticles
	 * @param order : 0 = no order / -1 recent first / 1 old first
	 * @return
	 * @throws Exception
	 */
	DBCursor findDocsByFieldsTagsSetFullYear(	
			int yearOfInterest, 
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxArticles,
			int order	
	)
	throws Exception
	{	
		if(withTags!=null && withoutTags!=null)
			throw new Exception("findArticlesByFieldsTags() should select either withTags or withoutTags");

		assert ( order==ORDER_NONE || order == ORDER_RECENT_FIRST || order == ORDER_OLD_FIRST  ) : "Wrong order in  findDocsByFieldsTagsSetSingleYear()";


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
			//Break tags in two lists // One for normal tags and one for Control Tags
			List<Integer> withTagsNormal = new LinkedList<Integer>();
			List<Integer> withTagsFor = new LinkedList<Integer>();
			for(int t=0;t<withTags.size();t++)
			{
				if(getTagProperty(withTags.get(t), TAG_PROPERTY_CONTROL)==1)
					withTagsFor.add(withTags.get(t));
				else
					withTagsNormal.add(withTags.get(t));
			}

			if(withTagsFor.size()>0)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTagsFor));
			if(withTagsNormal.size()>0)
				query.put(DOC_TAGS,new BasicDBObject("$all", withTagsNormal));

			//	if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
			//		query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			//	else
			//		query.put(DOC_TAGS,new BasicDBObject("$all", withTags));
		}
		if(withoutTags!=null)
		{
			if(getTagProperty(withoutTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$nin", withoutTags));
			else
				query.put(DOC_TAGS, new BasicDBObject("$nin", withoutTags));
		}


		DBCursor cursor = null;
		if(order == ORDER_NONE )
		{
			if(maxArticles>0)
				cursor = getCollDocs(yearOfInterest).find(query).limit( maxArticles ); 
			else
				cursor = getCollDocs(yearOfInterest).find(query);
		}
		else if((order == ORDER_RECENT_FIRST ) ||(order == ORDER_OLD_FIRST))	//recent
		{
			if(maxArticles>0)
				cursor = getCollDocs(yearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,order)).limit( maxArticles ); 
			else
				cursor = getCollDocs(yearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,order));
		}
		else
			throw new Exception("Wrong order");


		return cursor;
	}


	/**
	 * Finds docs published within time period, that have specific sets of tags and fields.
	 * The results are sorted and newest docs are first.
	 * 
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param withFields
	 * @param withoutFields
	 * @param withTags
	 * @param withoutTags
	 * @param maxArticles -  Upper limit of MAX_ARICLEIDS_IN_RESULT_LIST articles per call.
	 * @return A list of article IDs.
	 * @throws Exception
	 */
	public BBDocSet findRecentDocsByFieldsTags(	
			Date fromDate,
			Date toDate,
			List<String> withFields,
			List<String> withoutFields,
			List<Integer> withTags,
			List<Integer> withoutTags,
			int maxArticles	
	)
	throws Exception
	{	
		if(withTags!=null && withoutTags!=null)
			throw new Exception("findArticlesByFieldsTags() should select either withTags or withoutTags");

		ObjectId fromID = null;
		ObjectId toID = null; 

		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);

		BasicDBObject query = new  BasicDBObject();

		int YearOfInterest = -1;
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;
			//		System.out.println("Year of interest="+YearOfInterest);

			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.put(DOC_ID, range );
		}
		else if(fromID!=null)
			query.put(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.put(DOC_ID,new BasicDBObject("$lt",toID));

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
			List<Integer> withTagsNormal = new LinkedList<Integer>();
			List<Integer> withTagsFor = new LinkedList<Integer>();
			for(int t=0;t<withTags.size();t++)
			{
				if(getTagProperty(withTags.get(t), TAG_PROPERTY_CONTROL)==1)
					withTagsFor.add(withTags.get(t));
				else
					withTagsNormal.add(withTags.get(t));
			}

			if(withTagsFor.size()>0)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTagsFor));
			if(withTagsNormal.size()>0)
				query.put(DOC_TAGS,new BasicDBObject("$all", withTagsNormal));

//			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
//			query.put(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
//			else
//			query.put(DOC_TAGS,new BasicDBObject("$all", withTags));
		}
		if(withoutTags!=null)
		{
			if(getTagProperty(withoutTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.put(DOC_FOR_TAGS,new BasicDBObject("$nin", withoutTags));
			else
				query.put(DOC_TAGS, new BasicDBObject("$nin", withoutTags));
		}

		DBCursor cur ;		
		if(maxArticles>0)
			cur = getCollDocs(YearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,-1)).limit( maxArticles );
		else
			cur = getCollDocs(YearOfInterest).find(query).sort(new BasicDBObject(DOC_ID,-1));

		return new BBDocSet(cur);
	}

	/**
	 * Removes a doc from the BB.
	 * @param docID
	 * @throws Exception
	 */
	public void removeDoc(Object docID) throws Exception
	{
		if(adminMode==false)
			throw new Exception("This function is for admin purposes only.");

		getCollDocs(getYearOfInterestByDocID(docID)).remove( new BasicDBObject(DOC_ID, docID) );

	}

	/**
	 * Counts number of articles based on specified criteria that need to match.
	 * 
	 * If a tag has the prefix FOR, then all tags are assumed to be control tags.
	 * 
	 * @param fromDate The start of time period of interest (set to null to ignore)   
	 * @param toDate The end of time period of interest (set to null to ignore)
	 * @param withTags The set of tags that all articles found must carry (set to null to ignore)
	 * @param withoutTags The set of tags that articles found must NOT carry (set to null to ignore)
	 * @return The number of articles that match criteria. 
	 * @throws Exception
	 */
	public long countDocs(
			Date fromDate, 
			Date toDate, 
			List<Integer> withTags,
			List<Integer> withoutTags
	)
	throws Exception  
	{
		long totalCount = 0;

		//if dates are not specified
		if((fromDate==null) && (toDate==null))
		{
			fromDate = getMinDocDate();
			toDate = getMaxDocDate();

			int fromYear = getYearOfInterestByDate(fromDate);
			int toYear = getYearOfInterestByDate(toDate);

			for(int year = fromYear; year<=toYear; year++)
				totalCount += countDocsFullYear(year,withTags, withoutTags );
		}
		else	// There are dates
		{
			int fromYear = getYearOfInterestByDate(fromDate);
			int toYear = getYearOfInterestByDate(toDate);

			if(fromYear == toYear)
				return countDocsWithinSingleYear(	fromDate,toDate,withTags,	withoutTags);


			//3phases:
			//A) starting date until 31/12/year 

			Date endFirstYear = new GregorianCalendar( 	
					fromYear,11,31,	23,59,59).getTime();

			totalCount +=countDocsWithinSingleYear(	fromDate,endFirstYear,withTags,	withoutTags);

			//B) sum all full years 
			for(int year = fromYear+1; year<=toYear-1; year++)
			{
				totalCount +=countDocsFullYear(	year, withTags,	withoutTags);
			}

			//C) rest of year
			Date startFinalYear = new GregorianCalendar( 	
					toYear,0,1,	0,0,0).getTime();

			totalCount +=countDocsWithinSingleYear(startFinalYear,toDate,withTags,	withoutTags);
		}

		return totalCount;
	}


	public long countDocs2(
			Date fromDate, 
			Date toDate, 
			List<Integer> withTags,
			List<Integer> withoutTags
	)
	throws Exception  
	{
		long totalCount = 0;

		int fromYear = getYearOfInterestByDate(fromDate);
		int toYear = getYearOfInterestByDate(toDate);

		if(fromYear != toYear)
			throw new Exception("Accespts only dates from same year"); 


		BBDocSet articles = new BBDocSet(findDocsByDatesSingleYearOnlyTags(fromDate, toDate,0));
		BBDoc article;
		while((article=articles.getNext()) != null)
		{
			if(article.getAllTagIDs().containsAll(withTags))
				totalCount++;
		}


		return totalCount;
	}

	/**
	 * Counts the docs of the specified date.
	 * That is from 0:0:0 until 23:59:59 of the date of interest.
	 * 
	 * @param fromDate 	The date of interest
	 * @param withTags	Count docs with these tagIDs
	 * @param withoutTags Count docs without these tagIDs.
	 * @return	The number of docs.
	 * @throws Exception
	 */
	public long countDocs(
			Date fromDate, 
			List<Integer> withTags,
			List<Integer> withoutTags
	)
	throws Exception  
	{
		Calendar fromcal = Calendar.getInstance();  
		fromcal.setTime(fromDate);  
		fromcal.set(Calendar.HOUR_OF_DAY, 0);  
		fromcal.set(Calendar.MINUTE, 0);  
		fromcal.set(Calendar.SECOND, 0);  
		fromcal.set(Calendar.MILLISECOND, 0);  
		fromDate = fromcal.getTime();

		Calendar tocal = Calendar.getInstance();  
		tocal.setTime(fromDate);  
		tocal.set(Calendar.HOUR_OF_DAY,23);  
		tocal.set(Calendar.MINUTE, 59);  
		tocal.set(Calendar.SECOND, 59);  
		tocal.set(Calendar.MILLISECOND, 0);  
		Date toDate = tocal.getTime();


		ObjectId fromID = null;
		ObjectId toID = null; 

		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);

		BasicDBObjectBuilder query = new BasicDBObjectBuilder();

		int YearOfInterest = -1;
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			YearOfInterest = fromYear;

//			if((fromID!=null) && (toID!=null) ) {
			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.add(DOC_ID, range );
		}
		else if(fromID!=null)
			query.add(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.add(DOC_ID,new BasicDBObject("$lt",toID));

		if(withTags!=null) {
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.add(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.add(DOC_TAGS,new BasicDBObject("$all",withTags));
		}


		if(withoutTags!=null)
			query.add(DOC_TAGS,new BasicDBObject("$nin",withoutTags));

		//	System.out.println( query.get() );
		long count = getCollDocs(YearOfInterest).count(query.get());			

		return count;
	}


	private long countDocsWithinSingleYear(
			Date fromDate, 
			Date toDate, 
			List<Integer> withTags,
			List<Integer> withoutTags
	)
	throws Exception  
	{
		ObjectId fromID = null;
		ObjectId toID = null; 

		if(fromDate!=null)
			fromID = new ObjectId(fromDate);
		if(toDate!=null)
			toID = new ObjectId(toDate);

		BasicDBObjectBuilder query = new BasicDBObjectBuilder();


		int YearOfInterest = -1;
		if((fromID!=null) && (toID!=null) ) {
			int fromYear = getYearOfInterestByDocID(fromID);
			int toYear = getYearOfInterestByDocID(toID);
			if(fromYear != toYear)
				throw new Exception("Year field of fromDate and toDate must be equal");
			YearOfInterest = fromYear;

//			if((fromID!=null) && (toID!=null) ) {
			BasicDBObject range = new BasicDBObject();
			range.put("$gte", fromID);
			range.put("$lt", toID);
			query.add(DOC_ID, range );
		}
		else if(fromID!=null)
			query.add(DOC_ID,new BasicDBObject("$gte",fromID));
		else if(toID!=null)
			query.add(DOC_ID,new BasicDBObject("$lt",toID));

		if(withTags!=null) {
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.add(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.add(DOC_TAGS,new BasicDBObject("$all",withTags));
		}

		if(withoutTags!=null)
			query.add(DOC_TAGS,new BasicDBObject("$nin",withoutTags));

		System.out.println( query.get() );

		long count = getCollDocs(YearOfInterest).count(query.get());			

		return count;
	}

	private long countDocsFullYear(
			int YearOfInterest, 
			List<Integer> withTags,
			List<Integer> withoutTags
	)
	throws Exception  
	{
		BasicDBObjectBuilder query = new BasicDBObjectBuilder();

		if(withTags!=null) {
			if(getTagProperty(withTags.get(0), TAG_PROPERTY_CONTROL)==1)
				query.add(DOC_FOR_TAGS,new BasicDBObject("$all", withTags));
			else
				query.add(DOC_TAGS,new BasicDBObject("$all",withTags));
		}

		if(withoutTags!=null)
			query.add(DOC_TAGS,new BasicDBObject("$nin",withoutTags));

		//	System.out.println( query.get() );

		long count = getCollDocs(YearOfInterest).count(query.get());			

		return count;
	}


	/**
	 * Run a Map/Reduce job.
	 * The result is stored using the specified collection name.
	 * If the collection exists, new results will be merged with old using the reduce function. 
	 * 
	 * @param name A name of the job. If a job has run with same name it is deleted.
	 * @param map The map function in Javascript
	 * @param reduce The reduce function in Javascript
	 * @throws MongoException
	 */
	public void runMapReduce(String name,String map,String reduce) throws MongoException
	{
		String fullCollName = BB_NAME+"_MapReduce_"+name;
		//	DBCollection mapReduceColl = mongo_db.getCollection( fullCollName );
		//	mapReduceColl.drop();

		for(int yearOfInterest=MIN_ARTICLES_YEAR_IN_DB; yearOfInterest<=MAX_ARTICLES_YEAR_IN_DB; yearOfInterest++)
		{
			System.out.println("Submitting Map/Reduce for year="+yearOfInterest);
			MapReduceCommand cmd = new MapReduceCommand(
					getCollDocs(yearOfInterest),
					map,
					reduce,
					fullCollName,
					MapReduceCommand.OutputType.REDUCE,
					new  BasicDBObject());
			getCollDocs(yearOfInterest).mapReduce(cmd);
		}
	}


	/**
	 * Deletes the result of a previous Map/Reduce job.
	 * @param name
	 * @throws MongoException
	 */
	public void deleteMapReduceResult(String name) throws MongoException
	{
		String fullCollName = BB_NAME+"_MapReduce_"+name;
		DBCollection mapReduceColl = mongo_db.getCollection( fullCollName );
		mapReduceColl.drop();
	}

	/**
	 * Lists the completed results of previous Map/Reduce jobs.
	 * @return A List<String> with the completed M/R jobs.
	 * @throws MongoException
	 */
	public List<String> listMapReduceResults() throws MongoException
	{
		List<String> results = new LinkedList<String>();
		Set<String> existingDBs = mongo_db.getCollectionNames();
		for(String coll_n : existingDBs)
		{
			if(coll_n.contains(BB_NAME + "_MapReduce_"))
				results.add( coll_n );
		}
		return results;
	}


	/**
	 * Gets all the results of the named Map/Reduce job.
	 *  
	 * @param name The name of the Map/Reduce job. 
	 * @return A BBDocArticleSet with the results.
	 * @throws MongoException
	 */
	public BBDocSet getMapReduce(String name) throws MongoException
	{
		String fullCollName = BB_NAME+"_MapReduce_"+name;

		DBCollection mapReduceColl = mongo_db.getCollection( fullCollName );

		DBCursor cursor = mapReduceColl.find();

		return new BBDocSet(cursor);
	}


	/**
	 * Searches a MR result for a specific result based on ID.
	 * 
	 * @param name The name of the MR job.
	 * @param docID	The ID of doc of intererest.
	 * @return A BBDocArticle object.
	 * @throws MongoException
	 */
	public BBDoc getMapReduceDoc(String name, Object docID) throws
	MongoException
	{
		String fullCollName = BB_NAME+"_MapReduce_"+name;

		DBCollection mapReduceColl = mongo_db.getCollection( fullCollName );

		BasicDBObject where = new BasicDBObject();
		where.put(DOC_ID, docID);

		DBObject result = mapReduceColl.findOne( where );

		return new BBDoc(result);
	}

	/**
	 * Checks that all indexes of BB are in place.
	 * @param YearOfInterest
	 * @throws Exception
	 */
	void checkIndexes(int YearOfInterest) throws Exception
	{
		if(!adminMode)
			throw new Exception("checkTweetsIndexes" + BlackBoardsAPI.ERROR_FUNCTION_FOR_ADMIN );

	}

	/**
	 * For admin usage only.
	 * Checks if all indexes are in place.
	 * 
	 * @throws Exception
	 */
	@Override
	public void checkIndexes() throws Exception
	{
		if(!adminMode)
			throw new Exception("buildIndex" + BlackBoardsAPI.ERROR_FUNCTION_FOR_ADMIN );	

		for(int year=MIN_ARTICLES_YEAR_IN_DB; year<=MAX_ARTICLES_YEAR_IN_DB ; year++)
		{
			checkIndexes(year);
		}
	}


	/**
	 * Checks if index on field "fieldName" exists.
	 * If it doesn't it creates it.
	 * This runs for all Years
	 * 
	 * @param fieldName The name of the field to be indexed.
	 * @throws Exception 
	 */
	 // @Override
	void ensureIndex(String fieldName) throws Exception
	{
		if(!adminMode)
			throw new Exception("buildIndex" + BlackBoardsAPI.ERROR_FUNCTION_FOR_ADMIN );	

		BasicDBObject index;
		BasicDBObject options;

		for(int year=MIN_ARTICLES_YEAR_IN_DB; year<=MAX_ARTICLES_YEAR_IN_DB ; year++)
		{
			System.out.println("Checking/ Index for field'"+ fieldName +"' on BB " + BB_NAME);

			index = new BasicDBObject();
			index.put(fieldName, 1);
			options = new BasicDBObject();
			options.put("background",true);
			getCollDocs(year).ensureIndex(index, options);
		}

	}

	@Override
	public List<Object> getAllDocIDs()  throws Exception
	{
		throw new Exception("Unsopported function for this Black Board. Use sets.");
	}

	/**
	 * Gets the ID of the Doc as Date.
	 * @return The publication date.
	 */
	public Date convertID2Date(Object id) 
	{ 
		return new Date( ((ObjectId)id).getTime() ); 
	}

	/**
	 * Gets the ID of the Doc as Date.
	 * @return The publication date.
	 */
	public Object convertDate2ID(Date date) 
	{ 
		return new ObjectId(date); 
	}



}
