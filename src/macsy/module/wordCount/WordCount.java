package macsy.module.wordCount;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.module.BaseModule;

/**
 * Add a field in each document between desired dates, that specifies the number of 
 * words of the desired field of input.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELD=The name of the fields the user wants to count the words. 
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest.
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * 
 * Output:
 * OUTPUT_FIELDS=The field's name where the module is going to write the number of words
 * ON_SCREEN=TRUE if you want output also on screen (Optional) 
 * 
 * @author Panagiota Antonakaki
 * Last Update: 08-10-2012
 */

public class WordCount extends BaseModule {
	// temporal variables to hold information given by the user
	static final String PROPERTY_ON_SCREEN = "ON_SCREEN";
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	
	public WordCount(String propertiesFilename ) throws Exception 
	{
		super(propertiesFilename);
	}

	public void runModuleCore() throws Exception 
	{
		// see if you want the results to be displayed or not
		boolean onScreen= false;
		String onScreen_str = this.getProperty(PROPERTY_ON_SCREEN);
		if(onScreen_str.equals("TRUE"))
			onScreen = true;
		
		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;
		
		//Prepare TAG 
		String tagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> Tag_List = new LinkedList<Integer>();		
		for(String tagName : tagNames) 
		{
			if(!tagName.equals("")){
				int tagID = bb.getTagID(tagName);
				if( tagID==0){
					System.err.println("No known input tag");
					System.exit(0);
				}
				Tag_List.add(tagID);
			}
		}
		
		// get the user choice for dates and transform them into the desirable form
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
		
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, Tag_List, null, this.MODULE_DATA_PROCESS_LIMIT);
		
		// get the next doc in the BBDocSet
		BBDoc s = DocSet.getNext();
		
		// variable to count the number of the words
		int count = 0;
			
		while(s!=null)
		{
			// increase the number of the data that were read
			dataRead++;
			
			// get the article's doc according to the input field
			String txt = s.getFieldString(MODULE_INPUT_FIELDS);
			
			// check if it's null
			if(txt==null){
				// get the next article
				s = DocSet.getNext();
				continue;
			}
			
			// increase the number of the data tat were proceed
			dataProcessed++;
			
			// use the split method to split the title into words
			String[] words = txt.split(" ");
			// find how many words in the title
			count = words.length;
			
			// save the output in the desired field in the database
			bb.addFieldToDoc(s.getID(), MODULE_OUTPUT_FIELDS, count);
			
			if(onScreen)
				System.out.println(count);
			
			// get the next doc int the BBDocSet
			s = DocSet.getNext();
		}

		// display the number of input items and the number of output items
		this.saveModuleResults(dataRead, dataProcessed);
	}
	

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		WordCount module = new WordCount(args[0]);
		module.run();
	}

}
