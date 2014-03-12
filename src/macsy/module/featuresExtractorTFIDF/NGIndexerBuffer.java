package macsy.module.featuresExtractorTFIDF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;




public class NGIndexerBuffer extends NGIndexer {

	public String ALL_BUFFERS_PATHNAME = "Indexer_Buffer/";
	public String BUFFER_PATHNAME = null;
	public String INDEXED_EXTENSION = ".idx";


	public NGIndexerBuffer(String voc_filename, String stopwords_filename, String BufferName) throws Exception
	{
		super(voc_filename,stopwords_filename);

		File pathdir = new File( ALL_BUFFERS_PATHNAME );
		if(!pathdir.exists())
			pathdir.mkdir();

		BUFFER_PATHNAME = BufferName + "/";
		File bufferdir = new File( ALL_BUFFERS_PATHNAME + BUFFER_PATHNAME);
		if(!bufferdir.exists())
			bufferdir.mkdir();

	}

	/**
	 * Return the BOW string or NULL if not found
	 * @param ID
	 * @return
	 */
	public String LoadBOW( int ID )
	{
		try
		{
			BufferedReader input = new BufferedReader( 
					new FileReader(ALL_BUFFERS_PATHNAME + BUFFER_PATHNAME + ID+ INDEXED_EXTENSION) );

			String line = input.readLine();  
			input.close();
			return line;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void SaveBOW( int ID, String bow ) throws Exception
	{
		BufferedWriter fp = null;
		
		File filename = new File(ALL_BUFFERS_PATHNAME + BUFFER_PATHNAME + ID+ INDEXED_EXTENSION);			
		fp = new BufferedWriter(new FileWriter(filename));
		fp.write( bow );
		fp.close();
	}

}
