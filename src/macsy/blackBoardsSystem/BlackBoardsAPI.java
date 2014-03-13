package macsy.blackBoardsSystem;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.ReadPreference;

/**
 * This class is the entry point for the Black Board (BB) API.
 * It connects to the database and it 
 * provides access to the main BBs, e.g., Articles, Feeds and Outlets.
 * 
 * Version 1.20 - Introduces the BlackBoardURLs class<br>
 * Version 1.21 - Introduces the BlackBoardTweets class<br>
 * Version 1.22 - Constructor selects the BBs that will be used, introduces Locations BB.<br>
 * Version 1.23 - Admin mode fixed<br>
 * Version 1.24 - Double Passwords fixed<br>
 * Version 1.25	- Min/Max article date / counts, Tweets  <br> 
 * Version 1.251- Geonames <br> 
 * Version 1.252- Map/Reduce <br> 
 * Version 1.253- Improved Sets<br> 
 * Version 1.254- TagSets, Querry for all tags.<br> 
 * Version 1.255- URLSets/FeedSets<br> 
 * Version 1.256- Improvements. In BDoc: getters will return null if field is not found.<br> 
 * Version 1.30	- Redesign of internal classes: Introduction of BlackBoardIntID / BlackBoardDateID / BBDocSet
 * Version 1.31 - Bug fixes
 * Version 1.32 - Update to support Mongo 2.2.0
 * 
 * @author      Ilias Flaounas, Tom Welfare
 * @version     1.32
 * @since       2014-03-12
 * 
 */
public final class BlackBoardsAPI {

	//Update version below each time, BEFORE you upload a version online
	/**
	 * Version of API
	 */
	public final static String API_VERSION = "1.32";

	/**
	 * The connection to the mongo DB
	 */
	private Mongo mongo_connection = null;

	
	/**
	 * Is this for adminMode? The default is false.
	 * adminMode allows API usage for admin purposes.
	 */
	private boolean adminMode  = false;

	/**
	 * This object is the connection to the DB.
	 * 
	 * It is used as input in constructor in case of creation of a new BlackBoard.
	 * 
	 * Should not be used for any other reason. 
	 * 
	 */
	private DB mongo_db = null;

//	/**
//	 * Returns a direct connection to the Database.
//	 * It is used as input to the creation of new BlackBoards.
//	 * 
//	 * @return The connection to the DB.
//	 */
//	private Object getDBConnection()
//	{
//		return (Object)mongo_db;
//	}

	//This is the password that is attached to each user-pasword
	private static final String API_PASSWORD = ")Djmsn)p";
	//This is the admin username that can connect as administrator
	private static final String API_ADMIN_USERNAME = "dbadmin";
	

//	/**
//	 * Mask for disabling all BBs. 
//	 */
//	public final static int USE_BB_NONE		= 	0;
	
//	/**
//	 * Mask for enabling all BBs. 
//	 */
//	public final static int USE_BB_ALL		= 	0xFFFFFFFF;

	
	/**
	 * This message is displayed when an admin function is called but the adminMode is false.
	 * 
	 */
	final static String ERROR_FUNCTION_FOR_ADMIN = ": This function is for admin purposes only.";


	//Strings of reserved BlackBoardNames - populated in constructor
	private List<String> BBProtectedNames = null; 
	
	//All user BBs are prefixed with this
	private String USER_BB_PREFIX = "";
	
//	private boolean FORCE_READ_PRIMARIES = false;
	
 	public BlackBoardsAPI(String dbsettingsFileName,  boolean adminMode, boolean forceReadPrimaries) throws Exception 
	{
		System.out.println("Initializing NewsAgentAPI...");

		BBProtectedNames = new ArrayList<String>();
		BBProtectedNames.add("ARTICLE");
		BBProtectedNames.add("FEED");
		BBProtectedNames.add("OUTLET");
		BBProtectedNames.add("URL");
		BBProtectedNames.add("MODULE");
		BBProtectedNames.add("MODULE_RUN");
		//BBProtectedNames.add("GEONAMES");
		

		String dburl  = "mongodb://127.0.0.1:27017";
		String dbname = "Macsy";
		String dbuser = "dbadmin";
		String dbpass = "";

		
		Properties props = new Properties();
		if(dbsettingsFileName!=null) {
			props.load( new FileInputStream(new File(dbsettingsFileName)));
			dburl  = props.getProperty("dburl");
			dbname = props.getProperty("dbname");
			dbuser = props.getProperty("dbuser");
			dbpass = props.getProperty("dbpassword");
		}
		
		//Check for admin username
		if(adminMode && !dbuser.equals(API_ADMIN_USERNAME))
			throw new Exception("This is only for administrator. This attempt has been logged.");
		
		//Non-admin users have to use API.
		//Thus they have half the password and the rest lays within API.
		if(!dbuser.equals(API_ADMIN_USERNAME) )
			dbpass+= API_PASSWORD;	//Append API-PASS extra layer
		else//SET ADMIN MODE
			this.adminMode = true;//adminMode;

		
		//TRY TO CONNECT TO MONGO DB
		if(mongo_connection == null) {
			System.out.println("Setting up connection");
			mongo_connection = new Mongo( new MongoURI(dburl) );
		}

		
		//SELECT DATABASE
		mongo_db = mongo_connection.getDB( dbname );
		
	//	FORCE_READ_PRIMARIES = forceReadPrimaries;
		if(forceReadPrimaries)
			mongo_db.setReadPreference(ReadPreference.primary());
		else
			mongo_db.setReadPreference(ReadPreference.secondaryPreferred());


		//SET READ SLAVE-OK for all 
	//	mongo_db.setReadPreference( ReadPreference.SECONDARY );
		
		//Check if user has access to that DB.
		if(!"".equals(dbpass))
		{
			boolean auth = mongo_db.authenticate( dbuser ,	dbpass.toCharArray() );
			if(!auth) {
				throw new Exception("Error in authentication!");
			}
		}
		

		//Initiate the selected BlackBoards
		
//		if((bbToUse & USE_BB_ARTICLES ) > 0 )
//			articles 	= new BlackBoardArticles(	mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_FEEDS ) > 0 )
//			feeds 		= new BlackBoardFeeds(		mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_OUTLETS ) > 0 )
//			outlets 	= new BlackBoardOutlets(	mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_URLS ) >0 )
//			urls  		= new BlackBoardURLs(		mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_TWEETS ) > 0 )
//			tweets 		= new BlackBoardTweets(		mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_LOCATIONS ) > 0 )
//			locations 	= new BlackBoardLocations(	mongo_db, adminMode);
//
//		if((bbToUse & USE_BB_GEONAMES ) > 0 )
//			geonames 	= new BlackBoardGeonames(	mongo_db, adminMode);

		System.out.println("DONE");			
	}
	
 	/**
 	 * Force reading only primaries and not seconderies.   
 	 */
 	public void forceReadPrimaries() {
		mongo_db.setReadPreference(ReadPreference.primary());
 	}
 	
	/**
	 * Creates the API object that provides access to specific Black Boards.
	 * 
	 * @param dbsettingsFileName This is the path and the filename of the settings file that contains 
	 * connection settings.
	 * @param bbToUse Defines Which BBs are going to be used. For example to use Articles and Feeds BBs
	 *  you should set this parameter to  "BlackBoardsAPI.USE_BB_ARTICLES | BlackBoardsAPI.USE_BB_FEEDS".
	 * @param adminMode Set to true iff you are an administrator.
	 * @throws Exception
	 */
 	public BlackBoardsAPI(String dbsettingsFileName,  boolean adminMode) throws Exception 
	{
 		this(dbsettingsFileName, adminMode, false);
	}



//	/**
//	 * Creates the API object that provides access to specific Black Boards.
//	 *  
//	 * @param propertyFileName This is the path and the filename of the settings file that contains 
//	 * connection settings.
//	 * @param bbToUse Defines Which BBs are going to be used. For example to use Articles and Feeds BBs
//	 *  you should set this parameter to  "BlackBoardsAPI.USE_BB_ARTICLES | BlackBoardsAPI.USE_BB_FEEDS".
//	 *  
//	 */
//	public BlackBoardsAPI(String propertyFileName,  int bbToUse) throws Exception
//	{
//		this(propertyFileName,  false);
//	}

	/**
	 * Creates an instance of the API with access to all basic Black Boards, i.e. 
	 * Articles, Feeds and Outlets 
	 * For efficiency reasons use the constructor that allows connection to the specific Black Boards 
	 * that your program is going to use.
	 * 
	 * @param propertyFileName This is the path and the filename of the settings file that contains 
	 * connection settings.
	 * @throws Exception
	 */
	public BlackBoardsAPI(String propertyFileName) throws Exception 
	{
		this(propertyFileName,  false);
	}
	
	/**
	 * Loads the named module Black Board.
	 * If BB is not present it is created.
	 * 
	 * @param BBname The name of the Black Board.
	 * @return A BlackBoard Object.
	 * @throws Exception
	 */
	public BlackBoard blackBoardLoad(String BBname) throws Exception
	{
		if(BBname.contains("$") || BBname.contains(" ") )
			throw new Exception("Wrong chars in BBname");
		
		//Default user bb
		String bbType;
		String fullBB;
		
		if( BBProtectedNames.contains( BBname) )
		{
			fullBB =  BBname;
			if(fullBB.equalsIgnoreCase("ARTICLE"))
				bbType = BlackBoard.BLACKBOARD_TYPE_DATE_BASED;
			else
				bbType = BlackBoard.BLACKBOARD_TYPE_STANDARD;
		}
		else
		{
			fullBB =  USER_BB_PREFIX + BBname;
			bbType = BlackBoard.getBlackBoardType(mongo_db, fullBB, null);	//Discover BB Type or set to some default if value is not set.
		}
		
		if(bbType.equals( BlackBoard.BLACKBOARD_TYPE_DATE_BASED  ))
			return new BlackBoardDateBased(mongo_db, fullBB, adminMode);
		
		return new BlackBoard(mongo_db, fullBB, adminMode);
	}
	
	/**
	 * Loads the named module Black Board.
	 * If BB is not present it is created.
	 * 
	 * @param BBname The name of the Black Board.
	 * @return A BlackBoard Object.
	 * @throws Exception
	 */
	public BlackBoardDateBased blackBoardLoadDateBased(String BBname) throws Exception
	{
		if(BBname.contains("$") || BBname.contains(" ") )
			throw new Exception("Wrong chars in BBname");
		
		String bbPrefix = USER_BB_PREFIX;
		if( BBProtectedNames.contains( BBname) )
			bbPrefix = "";
		
		String fullBB = bbPrefix + BBname;
		
		String 	bbType = BlackBoard.getBlackBoardType(mongo_db, fullBB,	BlackBoard.BLACKBOARD_TYPE_DATE_BASED );	//Discover BB Type or set default to DataBased
		
		if(bbType.equals( BlackBoard.BLACKBOARD_TYPE_STANDARD ))
			throw new Exception("Blackbord "+ BBname + " is Standard Type, not DateBased. Use blackBoardLoad() to load it, or fix its type.");
		
		BlackBoardDateBased bb = new BlackBoardDateBased(mongo_db, fullBB, adminMode);
		return bb; 
	}
	
	/**
	 * Deletes the named Black Board.
	 * If BB is not present it is created.
	 * 
	 * @param BBname The name of the module Black Board to delete.
	 * @throws Exception
	 */
	public void blackBoardDrop(String BBname) throws Exception
	{
		/*
		if(BBname.contains("$") || BBname.contains(" ") )
			throw new Exception("Wrong chars in BBname");
		
		String fullBB = "MODULE_BB_" + BBname;
		
		DBCollection bb = mongo_db.getCollection( fullBB );
		bb.drop();		
		bb = mongo_db.getCollection( fullBB +"_COUNTER");
		bb.drop();
		bb = mongo_db.getCollection( fullBB +"_TAGS");
		bb.drop();
		*/
	}
}
