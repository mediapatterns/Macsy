package macsy.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import macsy.blackBoardsSystem.BlackBoardsAPI;



/**
 * This class is the entry point for a Noam Module.
 * 
 * 
 * @author      Ilias Flaounas <iliasfl@gmail.com>
 * @since       2012-09-01
 * 
 */
public abstract class BaseModule {

	// - Update version below each time, AFTER you upload a version online
	/**
	 * Version of BaseModule 
	 */
	public static String MODACLE_VERSION = "0.01";
	
	private DBObject MODULE_SETTINGS = new BasicDBObject();
	private List<String> COMMON_SETTINGS = new LinkedList<String>();

	//MODULE NAME
	public String	DB_SETTINGS = "" ;
	public String	MODULE_NAME = "ModuleName";
	public String 	MODULE_DESC = "Description of the module";
	public String	MODULE_INPUT_BLACKBOARD = "Test_in";	
	public String 	MODULE_INPUT_TAGS = "TagNameIn1,TagNameIn2";
	public String 	MODULE_INPUT_FIELDS = "FieldNameIn1,FieldNameIn2";
	public String 	MODULE_OUTPUT_BLACKBOARD = "Test_out";
	public String 	MODULE_OUTPUT_TAGS	= "TagNameOut1,TagNameOut2";
	public String 	MODULE_OUTPUT_FIELDS = "FieldNameOut1,FieldNameOut2";

	//The date that this module run for the last time
	public Date		MODULE_LAST_START_EXECUTION_DATE	= null;
	public Date		MODULE_LAST_FINISH_EXECUTION_DATE	= null;
	public int 		MODULE_LAST_RUN_DURATION_MIN = 0; 
	public String 	MODULE_LAST_RUN_SERVER = null;
	public String	MODULE_LAST_RUN_USERNAME = null;

	
	//PER RUN
	public int		MODULE_DATA_PROCESS_LIMIT 	= 1000;
	private int		MODULE_NUM_INPUT 	= 0;	//Updated through saveModuleResults
	private int		MODULE_NUM_OUTPUT 	= 0;	//Updated through saveModuleResults
	private String	MODULE_RUN_COMMENT 	= "";	//Updated through saveModuleResults
	private Object 	MODULE_RUN_ID 		= null;

	//PROPERTIES
	public String propertiesFileName = "";	//This is given in command line when the module runs.
	private Properties properties = null;		//Here we store all properties of the above file

	
	//List of Standard Property Names
	private final String PROPERTY_DB_SETTINGS 		= "DB_SETTINGS";
	private final String PROPERTY_MODULE_NAME 		= "NAME";
	private final String PROPERTY_MODULE_DESC 		= "DESCRIPTION";
	private final String PROPERTY_DATA_PROCESS_LIMIT= "PROCESS_LIMIT";
	private final String PROPERTY_INPUT_BLACKBOARD 	= "INPUT_BLACKBOARD";
	private final String PROPERTY_INPUT_TAGS 		= "INPUT_TAGS";
	private final String PROPERTY_INPUT_FIELDS 		= "INPUT_FIELDS";
	private final String PROPERTY_OUTPUT_BLACKBOARD = "OUTPUT_BLACKBOARD";
	private final String PROPERTY_OUTPUT_TAGS 		= "OUTPUT_TAGS";
	private final String PROPERTY_OUTPUT_FIELDS 	= "OUTPUT_FIELDS";


	private BaseModuleDAO _storage;

	//This is exported to the module functionality  
	public BlackBoardsAPI _bbAPI = null;


	/**
	 * 
	 * @param propertyFileName
	 * @throws Exception
	 */
	public BaseModule(String propertiesFileName) 
	throws Exception
	{
		this.propertiesFileName = propertiesFileName;
	}


	/**
	 * Returns the value of the named user-specified property.
	 * Returns null if the property is not set.
	 * @param propertyName
	 * @return
	 */
	public String getProperty(String propertyName)
	{
		return properties.getProperty(propertyName);
	}



	/**
	 * Updates the MODULE RUN table with information about this module execution.
	 * It adds the provided In/Out to the current ones, and appends comment to the executions 
	 * comments.
	 * 
	 * @param docsInput Number of inputs
	 * @param docsOutput Number of outputs
	 * 
	 * @throws Exception
	 */
	public void saveModuleResults(int docsInput, int docsOutput) throws Exception
	{
		MODULE_NUM_INPUT += docsInput;
		MODULE_NUM_OUTPUT+= docsOutput;
	}


	/**
	 * Updates the MODULE RUN table with information about this module execution.
	 * It adds the provided In/Out to the current ones, and appends comment to the executions 
	 * comments.
	 * 
	 * @param docsInput Number of inputs
	 * @param docsOutput Number of outputs
	 * @param comment Comments on the execution 
	 * 
	 * @throws Exception
	 */
	public void saveModuleResults(int docsInput, int docsOutput, String comment) throws Exception
	{
		MODULE_NUM_INPUT += docsInput;
		MODULE_NUM_OUTPUT+= docsOutput;
		MODULE_RUN_COMMENT += comment;
	}


	/**
	 * Executes the module. This is a three phase process:
	 * Some initialization is made first,including the storing of the current timestamp;
	 * Then the core module code is executed; and finally some cleanup is made including the storing 
	 * of the timestamp at the end of the program.
	 *     
	 * @throws Exception
	 */
	public void run() throws Exception {
		preRunModule();		//Initializes 

		runModuleCore();	// This is defined in the implementation of a specific module

		postRunModule();
	}

	/**
	 * Module should implement its functionality in here.
	 *
	 * Parameters should be past through class constructor.
	 *
	 */
	abstract public void runModuleCore() throws Exception;


	/**
	 * Actions to be performed before the runModuleCore()
	 * 
	 * @throws Exception
	 */
	private void preRunModule() throws Exception 
	{
		//Load user properties
		loadProperties();

		//Initializes API
		this._bbAPI = new BlackBoardsAPI( this.DB_SETTINGS );
		
		//
		_storage = new BaseModuleDAO(_bbAPI);

		//LOG who started module and from which machine
		MODULE_LAST_RUN_USERNAME = System.getProperty("user.name"); 
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		MODULE_LAST_RUN_SERVER =localMachine.getHostName();

		_storage.RegisterModule(
				MODULE_NAME,
				MODULE_DESC,
				MODULE_INPUT_BLACKBOARD,
				MODULE_INPUT_TAGS,
				MODULE_INPUT_FIELDS,
				MODULE_OUTPUT_BLACKBOARD,
				MODULE_OUTPUT_TAGS,
				MODULE_OUTPUT_FIELDS,
				MODULE_SETTINGS);



		MODULE_LAST_START_EXECUTION_DATE = new Date();
		System.out.println("Start executing Module: "+ MODULE_NAME);
		System.out.println("Current time: "+ MODULE_LAST_START_EXECUTION_DATE);
		System.out.println("Data Process Limit: "+ MODULE_DATA_PROCESS_LIMIT);
		System.out.println("User:"+MODULE_LAST_RUN_USERNAME);
		System.out.println("Server:"+MODULE_LAST_RUN_SERVER);

		MODULE_RUN_ID = _storage.saveModuleStartRun(	
				MODULE_NAME, 
				MODULE_LAST_START_EXECUTION_DATE,
				MODULE_LAST_RUN_USERNAME,
				MODULE_LAST_RUN_SERVER
		);

		//_storage.ensureIndexes();

	}

	/**
	 * Loads the user properties.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadProperties() throws FileNotFoundException, IOException
	{
		File fileDesc = new File(propertiesFileName);
		if(!fileDesc.exists())	//Create default values
		{
			System.out.println("Settings file not found!");
//			StringBuffer defaultProperties = new StringBuffer();
//			
//			defaultProperties.append( PROPERTY_MODULE_NAME +"="+MODULE_NAME +"\n");
//			defaultProperties.append( PROPERTY_MODULE_DESC +"="+MODULE_DESC +"\n");
//			defaultProperties.append( PROPERTY_DB_SETTINGS +"="+DB_SETTINGS +"\n");
//			defaultProperties.append( PROPERTY_DATA_PROCESS_LIMIT +"="+MODULE_DATA_PROCESS_LIMIT +"\n");
//			defaultProperties.append( PROPERTY_INPUT_BLACKBOARD +"="+MODULE_INPUT_BLACKBOARD +"\n");
//			defaultProperties.append( PROPERTY_INPUT_TAGS +"="+MODULE_INPUT_TAGS +"\n");
//			defaultProperties.append( PROPERTY_INPUT_FIELDS +"="+MODULE_INPUT_FIELDS +"\n");
//			defaultProperties.append( PROPERTY_OUTPUT_BLACKBOARD +"="+MODULE_INPUT_BLACKBOARD +"\n");
//			defaultProperties.append( PROPERTY_OUTPUT_TAGS +"="+MODULE_INPUT_TAGS +"\n");
//			defaultProperties.append( PROPERTY_OUTPUT_FIELDS +"="+MODULE_INPUT_FIELDS +"\n");
//			
//			BufferedWriter fp = new BufferedWriter(new FileWriter(fileDesc));
//			fp.write(defaultProperties.toString());
//			fp.close();
		}
		
		//Load Properties from file
		properties = new Properties();
		properties.load( new FileInputStream( fileDesc ));
		
		COMMON_SETTINGS.add(PROPERTY_DB_SETTINGS);
		//Update standard properties
		DB_SETTINGS = properties.getProperty(PROPERTY_DB_SETTINGS);

		COMMON_SETTINGS.add(PROPERTY_MODULE_NAME);
		MODULE_NAME = "Macsy Module";
		if(properties.getProperty(PROPERTY_MODULE_NAME)!=null)
			MODULE_NAME	= properties.getProperty(PROPERTY_MODULE_NAME);
			
		COMMON_SETTINGS.add(PROPERTY_MODULE_DESC);
		MODULE_DESC = "A macsy module without description...";
		if(properties.getProperty(PROPERTY_MODULE_DESC)!=null)
			MODULE_DESC = properties.getProperty(PROPERTY_MODULE_DESC);
		
		COMMON_SETTINGS.add(PROPERTY_DATA_PROCESS_LIMIT);
		MODULE_DATA_PROCESS_LIMIT= 0;
		if(properties.getProperty(PROPERTY_DATA_PROCESS_LIMIT)!=null)
			MODULE_DATA_PROCESS_LIMIT = Integer.parseInt(properties.getProperty(PROPERTY_DATA_PROCESS_LIMIT));
		
		MODULE_INPUT_BLACKBOARD = 					properties.getProperty(PROPERTY_INPUT_BLACKBOARD);
		MODULE_INPUT_TAGS 		= 					properties.getProperty(PROPERTY_INPUT_TAGS);
		MODULE_INPUT_FIELDS 	= 					properties.getProperty(PROPERTY_INPUT_FIELDS);
		MODULE_OUTPUT_BLACKBOARD= 					properties.getProperty(PROPERTY_OUTPUT_BLACKBOARD);
		MODULE_OUTPUT_TAGS 		= 					properties.getProperty(PROPERTY_OUTPUT_TAGS);
		MODULE_OUTPUT_FIELDS	= 					properties.getProperty(PROPERTY_OUTPUT_FIELDS);
		
		COMMON_SETTINGS.add(PROPERTY_INPUT_BLACKBOARD);
		COMMON_SETTINGS.add(PROPERTY_INPUT_TAGS);
		COMMON_SETTINGS.add(PROPERTY_INPUT_FIELDS);
		COMMON_SETTINGS.add(PROPERTY_OUTPUT_BLACKBOARD);
		COMMON_SETTINGS.add(PROPERTY_OUTPUT_TAGS);
		COMMON_SETTINGS.add(PROPERTY_OUTPUT_FIELDS);
		
		@SuppressWarnings("resource")
		BufferedReader in
		   = new BufferedReader(new FileReader(propertiesFileName));
		String line = null;
		String PropertyName = null;
		Object PropertyValue = null;
		
		while((line = in.readLine()) != null){
			String toks[] = line.split("=");
			PropertyName = toks[0];
			if(toks.length > 1 &&
					!COMMON_SETTINGS.contains(PropertyName)){
				PropertyValue = toks[1];
				MODULE_SETTINGS.put(PropertyName, PropertyValue);
			}
		}
	}


	/**
	 * Actions to be performed after the runModuleCore()
	 * @throws Exception
	 */
	private void postRunModule() throws Exception  {
		MODULE_LAST_FINISH_EXECUTION_DATE = new Date();

		System.out.println("Finished: " + MODULE_NAME );
		System.out.println("Current time: "+ MODULE_LAST_FINISH_EXECUTION_DATE);

		MODULE_LAST_RUN_DURATION_MIN = (int)Math.round((	MODULE_LAST_FINISH_EXECUTION_DATE.getTime() - 
				MODULE_LAST_START_EXECUTION_DATE.getTime()) /
				60000.0);

		_storage.saveModuleRunResult(
				MODULE_RUN_ID,
				MODULE_LAST_FINISH_EXECUTION_DATE,
				MODULE_LAST_RUN_DURATION_MIN,
				MODULE_NUM_INPUT,
				MODULE_NUM_OUTPUT,
				MODULE_RUN_COMMENT
		);


		System.out.printf("Input items: %d\n", MODULE_NUM_INPUT);
		System.out.printf("Output items: %d\n", MODULE_NUM_OUTPUT);
		System.out.printf("Total execution time: %d minutes.\n", MODULE_LAST_RUN_DURATION_MIN);
	}



	

}
