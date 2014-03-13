package macsy.module;


import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.mongodb.DBObject;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.blackBoardsSystem.BlackBoardsAPI;


public class BaseModuleDAO {

//	private	DB mongo_db = null;
//	private static Mongo mongo_connection = null;

	private final String FIELD_MODULE_ID = "_id";
	private final String FIELD_MODULE_NAME = "Nm";
	private final String FIELD_MODULE_DESCRIPTION 	= "Ds";
	private final String FIELD_MODULE_INPUT_BBNAME 	= "InBB";
	private final String FIELD_MODULE_INPUT_TAGS 	= "InTgs";
	private final String FIELD_MODULE_INPUT_FIELDS 	= "InFds";
	private final String FIELD_MODULE_OUTPUT_BBNAME = "OutBB";
	private final String FIELD_MODULE_OUTPUT_TAGS 	= "OutTgs";
	private final String FIELD_MODULE_OUTPUT_FIELDS	= "OutFds";


	private final String FIELD_MODULE_RUN_ID 						= "_id";
	private final String FIELD_MODULE_RUN_MODULE_ID 				= "MID";
	private final String FIELD_MODULE_RUN_DURATION 					= "DUR";
	private final String FIELD_MODULE_RUN_LAST_START_EXECUTION_DATE = "LSED";
	private final String FIELD_MODULE_RUN_LAST_FINISH_EXECUTION_DATE= "LFED";
	private final String FIELD_MODULE_RUN_INPUT_SIZE				= "IN";
	private final String FIELD_MODULE_RUN_OUTPUT_SIZE				= "OUT";
	private final String FIELD_MODULE_RUN_COMMENT					= "COM";
	private final String FIELD_MODULE_RUN_USER						= "U";
	private final String FIELD_MODULE_RUN_SERVER					= "S";
	
	private final String FIELD_MODULE_SETTINGS  					= "Settings";



	private final String MODULE_COLLECTION_NAME= "MODULE";
	private final String MODULE_RUN_COLLECTION_NAME= "MODULE_RUN";

//	private DBCollection collModules = null;
//	private DBCollection collModulesRun = null;

//	private BlackBoardsAPI _bbAPI = null;
	private BlackBoard bbModules = null;
	private BlackBoard bbModulesRun = null;

	public BaseModuleDAO(BlackBoardsAPI _bbAPI) throws Exception
	{
//		this._bbAPI = _bbAPI;
		bbModules = _bbAPI.blackBoardLoad(MODULE_COLLECTION_NAME);
		bbModulesRun = _bbAPI.blackBoardLoad(MODULE_RUN_COLLECTION_NAME);
	}

//	public BaseModuleDAO(String settingsFilename) throws Exception
//	{
//	final String API_PASSWORD = ")Djmsn)p";


//	Properties props = new Properties();
//	props.load( new FileInputStream(new File(settingsFilename)));

//	String dburl = props.getProperty("dburl");
//	String dbname = props.getProperty("dbname");
//	String dbuser = props.getProperty("dbuser");
//	String dbpass = props.getProperty("dbpassword");

//	if(!dbuser.equals("dbadmin") )
//	dbpass+= API_PASSWORD;	//Append API-PASS extra layer


//	//TRY TO CONNECT TO MONGO DB
//	if(mongo_connection == null)
//	mongo_connection = new Mongo( new MongoURI(dburl) );

//	//SELECT DATABASE
//	mongo_db = mongo_connection.getDB( dbname );

//	boolean auth = mongo_db.authenticate( dbuser ,	dbpass.toCharArray() );
//	if(!auth) {
//	throw new Exception("Error in authentication!");
//	}

//	collModules = mongo_db.getCollection(MODULE_COLLECTION_NAME);
//	collModules.setObjectClass(BasicDBObject.class);

//	collModulesRun = mongo_db.getCollection(MODULE_RUN_COLLECTION_NAME);
//	collModulesRun.setObjectClass(BasicDBObject.class);
//	}



	public void RegisterModule(
			String moduleName,
			String description,
			String inputBBName,
			String inputTags,
			String inputFields,
			String outputBBName,
			String outputTags,
			String outputFields,
			DBObject settings) 
	throws Exception
	{	
		if(		(moduleName==null) ||
				(description==null) )
			throw new Exception("Module parameters should be set. Use empty string instead of null");
		if(inputBBName==null) 
			inputBBName = "";
		if	(inputTags==null) 
			inputTags="";
		if(inputFields==null) 
			inputFields="";
		if(outputBBName==null) 
			outputBBName="";
		if(outputTags==null) 
			outputTags ="";
		if(outputFields==null) 
			outputFields ="";


		Object modID = getModuleID(moduleName);
		if(modID == null)
		{
			insertNewModule(
					moduleName, 
					description,
					inputBBName,
					inputTags,
					inputFields,
					outputBBName,
					outputTags,
					outputFields,
					settings);
		}
		else
		{
			updateModule(
					modID, 
					description,
					inputBBName,
					inputTags,
					inputFields,
					outputBBName,
					outputTags,
					outputFields,
					settings);
		}
	}


	public Object getModuleID(String moduleName) throws Exception 
	{
//		BasicDBObject query = new BasicDBObject();
//		query.put(FIELD_MODULE_NAME, moduleName); 
//		BasicDBObject res = (BasicDBObject) collModules.findOne(query);

		BBDoc mod = bbModules.findDocByFieldValue(FIELD_MODULE_NAME, moduleName);

		if(mod==null)
			return null;

		return mod.getField(FIELD_MODULE_ID);
	}


	private synchronized Object insertNewModule(
			String moduleName, 
			String description,
			String inputBBName,
			String inputTags,
			String inputFields,
			String outputBBName,
			String outputTags,
			String outputFields,
			DBObject settings
	) 
	throws Exception
	{
		Object moduleID = getModuleID(moduleName);
		if(moduleID!=null)	//Tag
			throw new Exception("Module :"+moduleName +" already exists.");

//		BasicDBObject doc = new BasicDBObject();
//		doc.put(FIELD_MODULE_NAME, moduleName );
//		doc.put(FIELD_MODULE_DESCRIPTION, description );
//		doc.put(FIELD_MODULE_INPUT_BBNAME, inputBBName );
//		doc.put(FIELD_MODULE_INPUT_TAGS, inputTags );
//		doc.put(FIELD_MODULE_INPUT_FIELDS, inputFields );
//		doc.put(FIELD_MODULE_OUTPUT_BBNAME, outputBBName );
//		doc.put(FIELD_MODULE_OUTPUT_TAGS, outputTags );
//		doc.put(FIELD_MODULE_OUTPUT_FIELDS, outputFields );

		BBDoc mod = new BBDoc();
		mod.setField(FIELD_MODULE_NAME, moduleName );
		mod.setField(FIELD_MODULE_DESCRIPTION, description );
		mod.setField(FIELD_MODULE_INPUT_BBNAME, inputBBName );
		mod.setField(FIELD_MODULE_INPUT_TAGS, inputTags );
		mod.setField(FIELD_MODULE_INPUT_FIELDS, inputFields );
		mod.setField(FIELD_MODULE_OUTPUT_BBNAME, outputBBName );
		mod.setField(FIELD_MODULE_OUTPUT_TAGS, outputTags );
		mod.setField(FIELD_MODULE_OUTPUT_FIELDS, outputFields );
		
		mod.setField(FIELD_MODULE_SETTINGS, settings);

		//collModules.insert(doc);
		bbModules.insertNewDoc(mod);

		return getModuleID(moduleName);
	}



	private synchronized void updateModule(
			Object modID,
			String description,
			String inputBBName,
			String inputTags,
			String inputFields,
			String outputBBName,
			String outputTags,
			String outputFields,
			DBObject settings) 
	throws Exception
	{
//		DBObject m = collModules.findOne(new BasicDBObject(FIELD_MODULE_ID, modID));

//		m.put(FIELD_MODULE_DESCRIPTION, description 	);
//		m.put(FIELD_MODULE_OUTPUT_BBNAME, outputBBName	);
//		m.put(FIELD_MODULE_OUTPUT_TAGS, outputTags 		);
//		m.put(FIELD_MODULE_OUTPUT_FIELDS, outputFields 	);
//		m.put(FIELD_MODULE_INPUT_BBNAME, inputBBName	);
//		m.put(FIELD_MODULE_INPUT_TAGS, inputTags 		);
//		m.put(FIELD_MODULE_INPUT_FIELDS, inputFields	);
//		collModules.update(new BasicDBObject(FIELD_MODULE_ID, modID), m );

		bbModules.addFieldToDoc(modID, FIELD_MODULE_DESCRIPTION, description );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_INPUT_BBNAME, inputBBName );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_INPUT_TAGS, inputTags );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_INPUT_FIELDS, inputFields );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_OUTPUT_BBNAME, outputBBName );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_OUTPUT_TAGS, outputTags );
		bbModules.addFieldToDoc(modID, FIELD_MODULE_OUTPUT_FIELDS, outputFields );

		bbModules.addFieldToDoc(modID, FIELD_MODULE_SETTINGS, settings);
		
		return ;
	}



	/**
	 * Store event that a module has started running.
	 * 
	 * @param MODULE_NAME
	 * @param MODULE_LAST_START_EXECUTION_DATE
	 * @return
	 * @throws Exception
	 */
	public Object saveModuleStartRun(
			String MODULE_NAME, 
			Date MODULE_LAST_START_EXECUTION_DATE,
			String userName,
			String serverName) 
	throws Exception
	{
//		BasicDBObject run = new  BasicDBObject();
//		run.put(FIELD_MODULE_RUN_MODULE_ID  , getModuleID(MODULE_NAME));
//		run.put(FIELD_MODULE_RUN_LAST_START_EXECUTION_DATE, MODULE_LAST_START_EXECUTION_DATE);
//		run.put(FIELD_MODULE_RUN_USER, userName);
//		run.put(FIELD_MODULE_RUN_SERVER, serverName);
//		collModulesRun.insert(run);
//		return run.get("_id");

		BBDoc run = new BBDoc();
		run.setField(FIELD_MODULE_RUN_MODULE_ID  , getModuleID(MODULE_NAME));
		run.setField(FIELD_MODULE_RUN_LAST_START_EXECUTION_DATE, MODULE_LAST_START_EXECUTION_DATE);
		run.setField(FIELD_MODULE_RUN_USER, userName);
		run.setField(FIELD_MODULE_RUN_SERVER, serverName);

		bbModulesRun.insertNewDoc(run);

		return run.getID();
	}


	/**
	 * 
	 * @param MODULE_RUN_ID
	 * @param MODULE_NAME
	 * @param MODULE_LAST_START_EXECUTION_DATE
	 * @param MODULE_LAST_FINISH_EXECUTION_DATE
	 * @param DURATION
	 * @param docsInput
	 * @param docsOutput
	 * @param comment
	 * @throws Exception
	 */
	public void saveModuleRunResult(
			Object MODULE_RUN_ID,
//			String MODULE_NAME, 
//			Date MODULE_LAST_START_EXECUTION_DATE,
			Date MODULE_LAST_FINISH_EXECUTION_DATE,
			long DURATION,
			int docsInput,
			int docsOutput,
			String comment
	) throws Exception
	{
//		DBObject r = collModulesRun.findOne(new BasicDBObject(FIELD_MODULE_RUN_ID, MODULE_RUN_ID));

//		r.put(FIELD_MODULE_RUN_LAST_FINISH_EXECUTION_DATE, MODULE_LAST_FINISH_EXECUTION_DATE);
//		r.put(FIELD_MODULE_RUN_DURATION, DURATION);
//		r.put(FIELD_MODULE_RUN_INPUT_SIZE, docsInput);
//		r.put(FIELD_MODULE_RUN_OUTPUT_SIZE, docsOutput);
//		r.put(FIELD_MODULE_RUN_COMMENT, comment);

//		collModulesRun.update(new BasicDBObject(FIELD_MODULE_RUN_ID, MODULE_RUN_ID), r );


		bbModulesRun.addFieldToDoc(MODULE_RUN_ID,FIELD_MODULE_RUN_LAST_FINISH_EXECUTION_DATE, MODULE_LAST_FINISH_EXECUTION_DATE);
		bbModulesRun.addFieldToDoc(MODULE_RUN_ID,FIELD_MODULE_RUN_DURATION, DURATION);
		bbModulesRun.addFieldToDoc(MODULE_RUN_ID,FIELD_MODULE_RUN_INPUT_SIZE, docsInput);
		bbModulesRun.addFieldToDoc(MODULE_RUN_ID,FIELD_MODULE_RUN_OUTPUT_SIZE, docsOutput);
		bbModulesRun.addFieldToDoc(MODULE_RUN_ID,FIELD_MODULE_RUN_COMMENT, comment);
	}

	void ensureIndexes() throws Exception
	{

		List<String> fieldsToIndex = new LinkedList<String>();
		fieldsToIndex.add(FIELD_MODULE_OUTPUT_BBNAME);
		fieldsToIndex.add(FIELD_MODULE_NAME);
		bbModules.ensureIndex(fieldsToIndex);


		fieldsToIndex = new LinkedList<String>();
		fieldsToIndex.add(FIELD_MODULE_RUN_MODULE_ID);
		fieldsToIndex.add(FIELD_MODULE_RUN_ID);
		bbModulesRun.ensureIndex(fieldsToIndex);
	}

}
