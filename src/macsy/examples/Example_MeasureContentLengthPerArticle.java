package macsy.examples;

import macsy.module.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;

/**
 * In this example, we will create a module that simply annotates documents
 * with the length in chars of their values of the field specified by the user.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELDS=The name of the field whose chars we want to calculate.
 * OUTPUT_FIELDS=The name of the field we want to store the number of the length of the input field.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * @author Ilias Flaounas
 */
public class Example_MeasureContentLengthPerArticle extends BaseModule {
	static final String PROPERTY_ON_START_DATE = "START_DATE";
	static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
	
	public Example_MeasureContentLengthPerArticle(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception {
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );

		// get the user choice for dates and transform them into the desirable form
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
		Date fromDate = df.parse(this.getProperty(PROPERTY_ON_START_DATE));
		Date toDate = df.parse(this.getProperty(PROPERTY_ON_STOP_DATE));


		System.out.println("Searching articles...");
		BBDocSet DocSet = bb.findDocsByFieldsTagsSet(fromDate, toDate, null, null, null, null, this.MODULE_DATA_PROCESS_LIMIT);

		String in_fieldName = MODULE_INPUT_FIELDS;
		// We define the name of the field that will be the output of the module
		String out_fieldName = MODULE_OUTPUT_FIELDS;            
		System.out.println("Applying annotation "+ out_fieldName);
		
		BBDoc s;
		while((s = DocSet.getNext())!=null)
		{
			String content = s.getFieldString(in_fieldName);
			if(content!=null) {
				// We annotate each article with their content length
				bb.addFieldToDoc(s.getID(), out_fieldName, content.length() );
				System.out.println(content.length());
			}
		}
	}

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */	
	public static void main(String[] args) throws Exception 
	{
		Example_MeasureContentLengthPerArticle module = new Example_MeasureContentLengthPerArticle(args[0]);
		module.run();
	}
}
