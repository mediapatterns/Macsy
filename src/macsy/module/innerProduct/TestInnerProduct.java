package macsy.module.innerProduct;

import java.io.*;
import java.util.*;

import macsy.blackBoardsSystem.*;
import macsy.module.BaseModule;
import macsy.module.featuresExtractorTFIDF.NGIndexer;

public class TestInnerProduct extends BaseModule{
	// temporal variables to hold information given by the user
		static final String PROPERTY_INPUT_FILENAME = "INPUT_TXT_FILENAME";
		static final String PROPERTY_INPUT_STOPWORDS_FILENAME = "INPUT_STOPWORDS_FILENAME";
		static final String PROPERTY_INPUT_VOCABULARY_FILENAME = "INPUT_VOCABULARY_FILENAME";
		static final String PROPERTY_INPUT_PRODUCT_COSINE = "INPUT_PRODUCT_COSINE";
		
		public TestInnerProduct(String propertiesFilename ) throws Exception {
			super(propertiesFilename);
		}
		
		@Override
		public void runModuleCore() throws Exception {
			//Load Black Board of interest (with data based)
			// only with articles
			BlackBoardDateBased bb = _bbAPI.blackBoardLoadDateBased(MODULE_INPUT_BLACKBOARD );
			//BlackBoard bb = _bbAPI.blackBoardLoad(MODULE_INPUT_BLACKBOARD );
			
			// make a list with desirable tags 
			List<Integer> Tag_List = new LinkedList<Integer>();
			Tag_List.add(bb.getTagID(MODULE_INPUT_TAGS));
			
			// get the name of the file of interest
			String txtFile = this.getProperty(PROPERTY_INPUT_FILENAME);
			String artText = LoadFile(txtFile);
			
			Integer outputTagID = bb.getTagID(MODULE_OUTPUT_TAGS);
			if(outputTagID==null)
				outputTagID=bb.insertNewTag(MODULE_OUTPUT_TAGS);
			
			//Input Voc / Stopwords
			String stopwordsFilename  = this.getProperty(PROPERTY_INPUT_STOPWORDS_FILENAME );
			String vocabularyFilename = this.getProperty(PROPERTY_INPUT_VOCABULARY_FILENAME);
			
			// variables to hold information about the number of data that were read and data
			// that were processed so that the module can print them on the screen
					
			//Initialize indexer
			NGIndexer _ng = new NGIndexer(	vocabularyFilename, stopwordsFilename );
			
			Map<Integer,Double> key_val = _ng.CreateBOW_Map( artText  );
			
			System.out.println("size : " + key_val.size());
		}
		
		private String LoadFile(String filename) 
		{
			StringBuffer stringBuffer = new StringBuffer();
			
			try {
				File file = new File(filename);
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				int lines = 0;
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					stringBuffer.append(line);
					stringBuffer.append("\n");
					lines++;
				}
				fileReader.close();
				System.out.println("lines" + lines);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return stringBuffer.toString();
		}
		
		public static void main(String[] args) throws Exception {
			TestInnerProduct module = new TestInnerProduct("C:\\Users\\Γιώτα\\EclipseWorkspace\\Macsy\\Projects\\TestModacleDB\\InnerProductTest.settings");
			//InnerProduct module = new InnerProduct("C:\\Users\\Γιώτα\\EclipseWorkspace\\Macsy\\Projects\\TestGiota\\InnerProductForJoy.settings");
			//InnerProduct module = new InnerProduct(args[0]);
			module.run();
		}

}
