package macsy.module.innerProduct;

import java.io.*;
import java.util.*;

import macsy.blackBoardsSystem.*;
import macsy.lib.DataPoint;
import macsy.module.BaseModule;
import macsy.module.featuresExtractorTFIDF.NGIndexer;

/**
 * Computes the inner product or the cosine similarity of a text of interest w (provided as an external file) and
 * documents' field x in the BB and stores the result in specified output field name. 
 * 
 * Input:
 * INPUT_BLACKBOARD=The name of the BlackBoard that contains the documents of interest.
 * INPUT_TAG=Run module only on docs that have this tag.
 * INPUT_FIELD=The field that contains the x of the docs.
 * INPUT_STOPWORDS_FILENAME=The path and name of the file that stores stopwords that will be removed.
 * INPUT_VOCABULARY_FILENAME=The path and name of the file that stores a vocabulary with IDFs.
 * INPUT_TXT_FILENAME=The name of the file that contains w.
 * INPUT_PRODUCT_COSINE=Set to "COSINE" or "PRODCUT" to select the corresponding function.  DEFAULT=Inner Product
 * PROCESS_LIMIT=The max number of documents. Set to zero to get all
 * 
 * Output:
 * OUTPUT_FIELD=The field's name where the module is going to write the calculated result.
 * OUTPUT_TAGS=The comma seperated names of the tags that will be applied to processed docs.   
 * 
 * @author Panagiota Antonakaki
 * Last Update: 17-10-2012
 * 
 */


public class InnerProduct  extends BaseModule {
	// temporal variables to hold information given by the user
	static final String PROPERTY_INPUT_FILENAME = "INPUT_TXT_FILENAME";
	static final String PROPERTY_INPUT_STOPWORDS_FILENAME = "INPUT_STOPWORDS_FILENAME";
	static final String PROPERTY_INPUT_VOCABULARY_FILENAME = "INPUT_VOCABULARY_FILENAME";
	static final String PROPERTY_INPUT_PRODUCT_COSINE = "INPUT_PRODUCT_COSINE";
	
	public InnerProduct(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
	}
	
	@Override
	public void runModuleCore() throws Exception 
	{
//		Load Black Board of interest (with date based) // only with articles
		BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
		//BlackBoard bb = _bbAPI.blackBoardLoad(MODULE_INPUT_BLACKBOARD );
		
		
		//check the decision of the user (inner product or cosine)
		String product_or_cosine = this.getProperty(PROPERTY_INPUT_PRODUCT_COSINE);
		boolean isFunctionInnerProduct = true;	//Variable name clarification
		if("COSINE".equals(product_or_cosine))	//inverse to avoid seg.fault if variable==null
			isFunctionInnerProduct = false;
		
		
		//Prepare input tags 
		String inputTagNames[] = MODULE_INPUT_TAGS.split(",");
		List<Integer> inputTagIDs = new LinkedList<Integer>();	//Variable name clarification + Java style
		for(String tagName : inputTagNames) 
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
		
		//Prepare output tags 
		String outTagNames[] = MODULE_OUTPUT_TAGS.split(",");
		List<Integer> outputTagIDs = new LinkedList<Integer>();//Variable name clarification
		for(String tagName : outTagNames) 
		{
			if(!tagName.equals("")){
				int	outputTagID = bb.getTagID(tagName);
				// if the tag not already exists insert new tag 
				if( outputTagID==0)
					outputTagID=bb.insertNewTag(MODULE_OUTPUT_TAGS);
				outputTagIDs.add(outputTagID);
			}
		}
			
		//Initialize indexer (Input Voc / Stopwords)
		String stopwordsFilename  = this.getProperty(PROPERTY_INPUT_STOPWORDS_FILENAME );
		String vocabularyFilename = this.getProperty(PROPERTY_INPUT_VOCABULARY_FILENAME);
		NGIndexer _ng = new NGIndexer(	vocabularyFilename, stopwordsFilename );

		// get the name of the file of interest
		String artText = loadWFromFile( this.getProperty(PROPERTY_INPUT_FILENAME) );
		Map<Integer,Double> key_val = _ng.CreateBOW_Map( artText  ); 
		
		//transform the calculated it-idf into DataPoint format 
		DataPoint w = new DataPoint(key_val);

		//Find the documents in the BB that have that input tag (subset)
		BBDocSet DocSet = bb.findDocsByTagsSet(inputTagIDs, null, this.MODULE_DATA_PROCESS_LIMIT);

		// variables to hold information about the number of data that were read and data
		// that were processed so that the module can print them on the screen
		int dataRead = 0;
		int dataProcessed = 0;

		// as long as there are still docs in the BBDocSet
		// get the next doc int the BBDocSet
		BBDoc s;
		while((s = DocSet.getNext())!=null)
		{
			dataRead++;
			
			@SuppressWarnings("unchecked")
			//take the already calculated tf-idf of the docs in BB
			List<Double> tf_idf = (List<Double>) s.getField(MODULE_INPUT_FIELDS);
			// check if the list is null
			if(tf_idf==null)
				continue;
			
			//	transform x into point
			DataPoint x = new DataPoint(tf_idf);
			
			double res = (isFunctionInnerProduct ?  x.getDotProduct(w) : x.getCosineSimilarity(w) );	//Compact form
			
			//add the result to the proper field
			bb.addFieldToDoc(s.getID(), MODULE_OUTPUT_FIELDS, res);
			bb.addTagsToDoc(s.getID(), outputTagIDs);	//ILIAS: we add all output tags not only one!!!

			//Doc was processed output report
			if(dataProcessed++%1000==0)	//Progress report every 1K docs
				System.out.printf("%.2f\n", dataProcessed *100.0 / MODULE_DATA_PROCESS_LIMIT );
		}
		
		// display the number of input items and the number of output items //ILIAS: Actually it does a lot more. EVery module should run this at the end
		this.saveModuleResults(dataRead, dataProcessed);
	}
	
	/**
	 * This function takes a file name as parameter and it returns its contents 
	 * @param filename
	 * @return the string that was read by the file
	 */
	private String loadWFromFile(String filename)	//  Java style function name + clear function name
	{
		StringBuilder stringBuffer = new StringBuilder();	//StringBuilder better than StringBuffer
		
		try {
			File file = new File(filename);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append("\n");
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit( 0 ) ;
		}
		return stringBuffer.toString();
	}
	
	/**
	 * 
	 * @param args The settings file that contains I/O and parameters info.
	 * @throws Exception 
	 * 
	 */
	public static void main(String[] args) throws Exception {
		//InnerProduct module = new InnerProduct("C:\\Users\\�����\\EclipseWorkspace\\Macsy\\Projects\\TestGiota\\TimeLine\\InnerProductForAnger.settings");
		//InnerProduct module = new InnerProduct("C:\\Users\\�����\\EclipseWorkspace\\Macsy\\Projects\\TestGiota\\TimeLine\\InnerProductForDisgust.settings");
		InnerProduct module = new InnerProduct("C:\\Users\\�����\\EclipseWorkspace\\Macsy\\Projects\\TestGiota\\TimeLine\\InnerProductForFear.settings");
		//InnerProduct module = new InnerProduct(args[0]);
		module.run();
	}
}
