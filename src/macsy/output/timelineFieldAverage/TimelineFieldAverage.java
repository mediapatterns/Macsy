package macsy.output.timelineFieldAverage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import macsy.blackBoardsSystem.*;
import macsy.module.BaseModule;
/**
 * Calculates the average of a field's values of interest to the dates in a specified period of days
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELD=The name of the desired fields of what the user wants to calculate the average. 
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest.
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * 
 * Output:
 * TXT_FILENAME=The path to a text file that will be REPLACED with module output.(Optional)
 * ON_SCREEN=TRUE if you want output also on screen (Optional)
 * 
 * @author Panagiota Antonakaki
 * Last Update: 03-10-2012
 */



public class TimelineFieldAverage extends BaseModule {
	// temporal variables to hold information given by the user
	static final String PROPERTY_TXT_FILENAME = "TXT_FILENAME";
	static final String PROPERTY_ON_SCREEN = "ON_SCREEN";
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public TimelineFieldAverage(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
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
		
		// make a list with desirable tags
		String tagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> inputTagIDs = new LinkedList<Integer>();
		for(String tagName : tagNames) 
		{
			if(!tagName.equals("")){
				int tagID = bb.getTagID(tagName);
				if( tagID==0){
					System.err.println("No known input tag");
					System.exit(0);
				}
				inputTagIDs.add(tagID);
			}
		}
		
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
				
		String str = dateFormat.format(calCurrentDate.getTime());
		
		Date dtFromDate = calCurrentDate.getTime();
		
		calCurrentDate.add(Calendar.DATE, 1);
		Date dtToDate = calCurrentDate.getTime();
		
		// find the documents in a desired period of time with the Tag list of interest 
		BBDocSet DocSet;
		BBDoc s;
		// in order to calculate the average we need both the sum of the field's value 
		// and the number of docs
		int count=0;
		double sum = 0;
		
		// do the same for the rest of the days
		while(calCurrentDate.before(calLastDate))
		{			
			str = dateFormat.format(dtFromDate.getTime());

			// find the documents in a desired period of time with the Tag list of interest 
			DocSet = bb.findDocsByFieldsTagsSet(dtFromDate, dtToDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);

			s = DocSet.getNext();

			count=0;
			sum = 0;
			while(s!=null){
				if((inputTagIDs!=null) && (s.getTagIDs()!=null) && (s.getAllTagIDs().containsAll(inputTagIDs) ))
				{
					Double temp = s.getFieldDouble(MODULE_INPUT_FIELDS);
					// check if it's null
					if(temp!=null){
						count++;
						sum = sum + temp;
					}
				}
				s = DocSet.getNext();

			}

			if(count == 0 )
				str = str + "\t" + Double.toString(0.0) + "\n";
			else
				str = str + "\t" + Double.toString(sum/count) + "\n";
			
			if(onScreen)
				System.out.print(str);
			if(outputBuffer!=null)
				outputBuffer.append(str);
			
			dtFromDate = calCurrentDate.getTime();
			calCurrentDate.add(Calendar.DATE, 1);
			dtToDate = calCurrentDate.getTime();
		}
		
		if(outputBuffer!=null)
		{
			File resultssubdir = new File( txtFilename );			
			BufferedWriter fp = new BufferedWriter(new FileWriter(resultssubdir));
			fp.write( outputBuffer.toString() );
			fp.close();
		}
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		TimelineFieldAverage module = new TimelineFieldAverage(args[0]);
		module.run();
	}

}