package macsy.output.destributionField;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoardDateBased;
import macsy.module.BaseModule;

/**
 * Exports the distribution of a given Field and exports a histogram (optional)
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that will be populated by the data points.
 * INPUT_FIELD=The name of the fields the user wants to calculate the distribution. 
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_START_DATE=The first day of interest.
 * INPUT_STOP_DATE=The last day of interest
 * HISTOGRAM=TRUE if you want visual output of the histogram (Optional)
 * BINS_NUM=The number of bins in case the values of the field are double so that we can have
 * 			a unified quantization of the results
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * 
 * Output:
 * TXT_FILENAME=The path to a text file that will be REPLACED with module output.(Optional)
 * ON_SCREEN=TRUE if you want output also on screen (Optional)
 * HISTOGRAM=TRUE if you want output as an image (Optional)
 * 
 * @author Panagiota Antonakaki
 * Last Update: 08-10-2012
 */

public class DistributionField extends BaseModule {
	// temporal variables to hold information given by the user
		static final String PROPERTY_TXT_FILENAME = "TXT_FILENAME";
		static final String PROPERTY_ON_SCREEN = "ON_SCREEN";
		static final String PROPERTY_ON_START_DATE = "START_DATE";
		static final String PROPERTY_ON_STOP_DATE = "STOP_DATE";
		static final String PROPERTY_HISTOGRAM = "HISTOGRAM";
		static final String PROPERTY_NUM_BINS = "BINS_NUM";
		
		
		public DistributionField(String propertiesFilename ) throws Exception 
		{
			super(propertiesFilename);
		}

		public void runModuleCore() throws Exception 
		{
			// see if the user wants the results to be displayed or not
			boolean onScreen= false;
			String onScreen_str = this.getProperty(PROPERTY_ON_SCREEN);
			if(onScreen_str.equals("TRUE"))
				onScreen = true;
			
			boolean Hist= false;
			String Hist_str = this.getProperty(PROPERTY_HISTOGRAM);
			if(Hist_str.equals("TRUE"))
				Hist = true;

			// check whether the user also wants txt output file
			StringBuilder outputBuffer = null;
			String txtFilename = this.getProperty(PROPERTY_TXT_FILENAME);
			if(txtFilename!=null)
			{
				outputBuffer = new StringBuilder();
			}
			
			String numBins = this.getProperty(PROPERTY_NUM_BINS);
			int numberOfBins = 5;
			double offset;
			if(numBins!=null)
				numberOfBins = Integer.parseInt(numBins);
			offset = 1 / numberOfBins;


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
			
			// get the next doc int the BBDocSet
			BBDoc s = DocSet.getNext();
			
			// create a map: <# of words in the title, # articles>
			HashMap<Double,Integer> value_counts = new HashMap<Double,Integer>();
				
			List<Double> keyList = new LinkedList<Double>();
			while(s!=null)
			{
				// increase the number of the data that were read
				dataRead++;
				
				// get the field's value (double value)
				Double dValue = s.getFieldDouble(MODULE_INPUT_FIELDS);
				
				// check if it's null
				if(dValue==null){
					// get the next article
					s = DocSet.getNext();
					continue;
				}
				dValue = mapValue(dValue,offset,numberOfBins);
				
				// increase the number of the data tat were proceed
				dataProcessed++;
				 			 
				// if the number of words already exists in the map
				if(value_counts.containsKey(dValue)==true){
					// increase the value by one
					int c = ((Integer)value_counts.get(dValue)).intValue();
					value_counts.put(dValue, c + 1);
				}
				else
					// put the new key - number of words
					value_counts.put(dValue, 1);
				
				// save the output in the desired field in the database
				s.setField(this.MODULE_OUTPUT_FIELDS, dValue);
				
				// get the next doc int the BBDocSet
				s = DocSet.getNext();
			}
			
			int maxMapValue = 0;
			int maxKeyValue = 0;
			// get information of the map / histogram
	        for (Map.Entry<Double,Integer> entry : value_counts.entrySet()) {
	        	if(entry.getValue()>maxMapValue)
	        		maxMapValue = entry.getValue();
	        	keyList.add(entry.getKey());
	        	String str = entry.getKey() + ":" + entry.getValue() + " "; 
				if(onScreen)
					System.out.print(str);
				if(outputBuffer!=null)
					outputBuffer.append(str);
				maxKeyValue = entry.getKey().intValue();
	        }
	        
	        // if the user chose them to be displayed, print them
			if(onScreen)
				System.out.println("");

			// if the user chose them to be saved in a file, save them
			if(outputBuffer!=null)
				outputBuffer.append("\n");
			if(outputBuffer!=null)
			{
				File resultssubdir = new File( txtFilename );			
				BufferedWriter fp = new BufferedWriter(new FileWriter(resultssubdir));
				fp.write( outputBuffer.toString() );
				fp.close();
			}
			
			// display histogram results to a jpg image
			
			// if the user chose to display histogram results
			if(Hist){
				// create a new object JFrame to paint our data
//				JFrame window = new JFrame();
//			    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			    
//			    window.setBounds(30, 30, 1000/maxMapValue, 700/(maxKeyValue+1));
//			    
//			    // set the window size
//			    window.setBounds(30, 30, (maxKeyValue +1)* 20 + 50, maxMapValue*15 + 60);
//			    // create a new object DrawRectangle to paint the data as we desire
//			    DrawRectangle rectangle = new DrawRectangle();
//			    // initialize the new object
//		    	rectangle.setValues(30, 30, maxKeyValue* 20 + 50, maxMapValue*15 + 60, 
//			    		value_counts, keyList, maxKeyValue,maxMapValue, offset);
//			    // paint the results
//			    window.getContentPane().add(rectangle);
//			    // display
//			    window.setVisible(true);
			    
			    // save image
			    /*Dimension size = window.getSize();
			    BufferedImage image = (BufferedImage)window.createImage(size.width, size.height);
			    try
			    {
		        ImageIO.write(image, "jpg", new File("MyFrame2.jpg"));
			    }
			    catch (IOException e)
			    {
			    	e.printStackTrace();
			    }*/
			}
				
			// display the number of input items and the number of output items
			this.saveModuleResults(dataRead, dataProcessed);
		}
		
		double mapValue(double dValue, double offset, int NumberOfBins){
			dValue = dValue*10;
			int temp = (int) Math.ceil(dValue);
			int BinNum = 0;
			for(int i = 0; i < NumberOfBins; i++){
				if(i*offset*10 > temp)
					BinNum = i; 
			}
			return ( offset*(BinNum-1) );
		}
		/**
		 * 
		 * @param args The settings file that contains I/O and parameters info.
		 * @throws Exception 
		 * 
		 */
		public static void main(String[] args) throws Exception {
			DistributionField module = new DistributionField(args[0]);
			module.run();
		}
}
