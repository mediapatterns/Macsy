package macsy.module.featuresExtractorTFIDF;
/**
 * 
 * Module for creating TF/IDF features of a text field.  
 * 
 * Input:
 * INPUT_BLACKBOARD=Input BlackBoard 
 * INPUT_FIELD=Comma separated text-fields that will be concatenatred and converted to a features vector. 
 * INPUT_TAG=Run module only on docs that have this tag. The tag of the docs will be removed after 
 * the module have run.
 * INPUT_STOPWORDS_FILENAME=The path and name of the file that stores stopwords that will be removed.
 * INPUT_VOCABULARY_FILENAME=The path and name of the file that stores a vocabulary with IDFs.
 * 
 * Output:
 * OUTPUT_FIELD=The features vector as a list of FeatureID,FeatureValue pairs. 
 * OUTPUT_TAG=Add this tag to processed docs.  
 * 
 * Author: Ilias Flaounas
 * Last Update: 01-10-2012
 * 
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BBDocSet;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.module.BaseModule;


public class FeaturesExtractorTFIDF  extends BaseModule {
	
	static final String PROPERTY_INPUT_STOPWORDS_FILENAME = "INPUT_STOPWORDS_FILENAME";
	static final String PROPERTY_INPUT_VOCABULARY_FILENAME = "INPUT_VOCABULARY_FILENAME";

	public FeaturesExtractorTFIDF(String propertiesFilename ) throws Exception {
		super(propertiesFilename);
//				DB_SETTINGS,
//				"Indexer(NYT) "+textToUse,
//				"Index based on NY Times corpus.",
//				NewsAgentModule.BLACKBOARD_ARTICLES,
//				TAG_FOR_INDEXER_PREFIX + textToUse,
//				"",
//				NewsAgentModule.BLACKBOARD_ARTICLES,
//				TAG_POST_INDEXER_PREFIX + textToUse,
//				FIELD_INDEXER_PREFIX + textToUse
//		);

	//	TEXT_TO_USE = textToUse;
	//	MODULE_INPUT_LIMIT = maxRunSize;
	}

	@Override
	public void runModuleCore() throws Exception {
		
		//Load Black Board of interest
		BlackBoard bb = _bbAPI.blackBoardLoadDateBased(  MODULE_INPUT_BLACKBOARD );
		FeaturesExtractorTFIDF_DAO _storage= new FeaturesExtractorTFIDF_DAO( bb ); 

		//Input Tag
		int inputTagID = _storage.getTagID( this.MODULE_INPUT_TAGS );
		
		//Input Fields
		List<String> inputFields = new ArrayList<String>();
		String inFields_toks[] = MODULE_INPUT_FIELDS.split(",");
		for(String f : inFields_toks)
			inputFields.add(f);
		
		//Input Voc / Stopwords
		String stopwordsFilename  = this.getProperty(PROPERTY_INPUT_STOPWORDS_FILENAME );
		String vocabularyFilename = this.getProperty(PROPERTY_INPUT_VOCABULARY_FILENAME);
		
		//Output Tag 
		int outputTagID = 0;
		if( !this.MODULE_OUTPUT_TAGS.equals("") )
			outputTagID = _storage.getTagID( this.MODULE_OUTPUT_TAGS );
		
		//String outputField = this.MODULE_OUTPUT_FIELDS ;

		System.out.println("Finding articles to be indexed...");
		BBDocSet articles = _storage.getInputDocs(inputTagID,this.MODULE_DATA_PROCESS_LIMIT);

		//Initilize indexer
		NGIndexer _ng = new NGIndexer(	vocabularyFilename, stopwordsFilename );
		
		
		BBDoc article;
		int i=0;
		while((article=articles.getNext())!=null)
		{
			if(++i%1000==0) 
				System.out.printf("%.2f%%\n",i*100.0/this.MODULE_DATA_PROCESS_LIMIT);

			String artText = _storage.getDocText(article.getID(), inputFields);

			Map<Integer,Double> key_val = _ng.CreateBOW_Map( artText  );

			_storage.storeIndexed(article.getID(), this.MODULE_OUTPUT_FIELDS, key_val);

			_storage.removeTagFromDoc( article.getID(), inputTagID );
			
			if(outputTagID!=0)
				_storage.addTagToDoc(article.getID(), outputTagID);
		}

		saveModuleResults(i,i );
	}



	/**
	 * @param args
	 */
	public static void main(String[] args)  throws Exception 
	{
		FeaturesExtractorTFIDF app = new FeaturesExtractorTFIDF(args[0]);
		app.run();
	}



}
