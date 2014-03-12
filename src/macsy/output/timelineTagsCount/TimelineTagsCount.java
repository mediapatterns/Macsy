package macsy.output.timelineTagsCount;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.module.BaseModule;

/**
 * This module calculates the number of the documents in a specified period of time that
 * have a Tag name of interest. 
 * The result is the distribution of this specific tag per day, and can be displayed on
 * the screen if necessary.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * 
 * Output:
 * TXT_FILENAME=The path to a text file that will be REPLACED with module output.(Optional)
 * ON_SCREEN=TRUE if you want output also on screen (Optional)
 * 
 * @author Panagiota Antonakaki
 * Last Update: 08-10-2012
 */

public class TimelineTagsCount extends BaseModule{

	static final String PROPERTY_TXT_FILENAME = "TXT_FILENAME";
	static final String PROPERTY_ON_SCREEN = "ON_SCREEN";
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	
	public TimelineTagsCount(String propertiesFilename ) throws Exception 
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

		// check whether you also want txt output file
		StringBuilder outputBuffer = null;
		String txtFilename = this.getProperty(PROPERTY_TXT_FILENAME);
		if(txtFilename!=null)
		{
			outputBuffer = new StringBuilder();
		}


		//Load Black Board of interest (with data based)
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		
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
		
		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;
			
		// make the day in the desired format
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		// get the desired period of time
		Date fromDate = dateFormat.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = dateFormat.parse(this.getProperty(PROPERTY_ON_STOP_DATE));
		
		// set the beginning of the period
		Calendar calCurrentDate = Calendar.getInstance();
		calCurrentDate.setTime(fromDate);
		
		// each time add one day in the beginning
		Calendar calLastDate = Calendar.getInstance();
		calLastDate.setTime(toDate);
		calLastDate.add(Calendar.DATE, 1);
		
		// this counter holds the number of documents in the database 
		// that have the tag of interest
		long count;
		
		String str = dateFormat.format(calCurrentDate.getTime());
		
		Date dtFromDate = calCurrentDate.getTime();
		
		calCurrentDate.add(Calendar.DATE, 1);
		Date dtToDate = calCurrentDate.getTime();
			
		while(calCurrentDate.before(calLastDate))
		{
			str = dateFormat.format(dtFromDate.getTime()) + " to " + 
					dateFormat.format(dtToDate.getTime());
			// check if the tag list of names is empty
			if(Tag_List.isEmpty())
				count = bb.countDocs(dtFromDate, dtToDate,null, null);
			else
				count = bb.countDocs2(dtFromDate, dtToDate,Tag_List, null);
			
			str = str + " : " + count;
			// display on screen or write in a file if necessary
			if(onScreen)
				System.out.println(str);
			if(outputBuffer!=null)
				outputBuffer.append(str);
			dataRead = dataRead + (int) count;
			
			dtFromDate = calCurrentDate.getTime();
			calCurrentDate.add(Calendar.DATE, 1);
			dtToDate = calCurrentDate.getTime();
		}
		
		if(onScreen)
			System.out.println("");
		if(outputBuffer!=null)
			outputBuffer.append("\n");
		dataProcessed = dataRead;

		if(outputBuffer!=null)
		{
			File resultssubdir = new File( txtFilename );			
			BufferedWriter fp = new BufferedWriter(new FileWriter(resultssubdir));
			fp.write( outputBuffer.toString() );
			fp.close();
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
		TimelineTagsCount module = new TimelineTagsCount(args[0]);
		module.run();
	}

}
