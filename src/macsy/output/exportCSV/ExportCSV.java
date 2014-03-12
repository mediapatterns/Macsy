package macsy.output.exportCSV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.module.BaseModule;

/**
 * Exports a blackboard to a  Comma Separated Values (CSV) file.
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_TAGS=Export only those docs that have these tags.
 * INPUT_FIELDS=The fields to output
 * 
 * 
 * Output:
 * TXT_FILENAME=The path to a text file that will be REPLACED with module output.(Optional)
 * ON_SCREEN=TRUE if you want output also on screen (Optional)
 * 
 * @author Ilias Flaounas
 *
 */
public class ExportCSV extends BaseModule {

	static final String PROPERTY_TXT_FILENAME = "TXT_FILENAME";
	static final String PROPERTY_ON_SCREEN = "ON_SCREEN";

	public ExportCSV(String propertiesFilename ) throws Exception 
	{
		super(propertiesFilename);

	}

	public void runModuleCore() throws Exception 
	{
		boolean onScreen= false;
		String onScreen_str = this.getProperty(PROPERTY_ON_SCREEN);
		if(onScreen_str.equals("TRUE"))
			onScreen = true;

		StringBuffer outputBuffer = null;
		String txtFilename = this.getProperty(PROPERTY_TXT_FILENAME);
		if(!"".equals(txtFilename))
		{
			outputBuffer = new StringBuffer();
		}

		List<String> fieldsToExport = null;
		if(!this.MODULE_INPUT_FIELDS.equals(""))
		{
			fieldsToExport = new LinkedList<String>();
			String fields[] = MODULE_INPUT_FIELDS.split(",");
			for(String f :fields)
			{
				fieldsToExport.add( f );
			}
		}
		

		//Load Black Board of interest
		BlackBoard bb = _bbAPI.blackBoardLoad(  MODULE_INPUT_BLACKBOARD );


		//Header
		for(String f : fieldsToExport)
			System.out.print(f +"\t" );
		System.out.println("");
		

		int dataRead = 0;
		BBDocSet docs = bb.getAllDocs();
		BBDoc doc;
		while((doc=docs.getNext())!=null)
		{
			dataRead++;
			if((MODULE_DATA_PROCESS_LIMIT>0) && (dataRead > this.MODULE_DATA_PROCESS_LIMIT))
				break;

			List<String> fieldNames = fieldsToExport;
			if(fieldNames==null)
				fieldNames = doc.getAllFieldNames();
			
			for(String fieldName : fieldNames)
			{
				Object value = doc.getField(fieldName);
				if(value == null)
					continue;
				
				String valueSmall = value.toString();
				valueSmall = (valueSmall.length() > 100 ? valueSmall.substring(0,100) : valueSmall).replaceAll("\n"," ");
				
				if(onScreen)
					System.out.print(valueSmall+"\t");
				if(outputBuffer!=null)
					outputBuffer.append(valueSmall+"\t");
			}
			if(onScreen)
				System.out.println("");
			if(outputBuffer!=null)
				outputBuffer.append("\n");
		}


		if(outputBuffer!=null)
		{
			File resultssubdir = new File( txtFilename );			
			BufferedWriter fp = new BufferedWriter(new FileWriter(resultssubdir));
			fp.write( outputBuffer.toString() );
			fp.close();
		}



		this.saveModuleResults(dataRead, 0);
	}

	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		ExportCSV module = new ExportCSV(args[0]);
		module.run();
	}


}
