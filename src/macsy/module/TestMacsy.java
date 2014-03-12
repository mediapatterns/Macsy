package macsy.module;

import macsy.blackBoardsSystem.BBDoc;
import macsy.blackBoardsSystem.BlackBoard;
import macsy.blackBoardsSystem.BlackBoardsAPI;

/**
* @author      Ilias Flaounas <iliasfl@gmail.com>
* @version     1.0                   
* @since       2012-11-01
* 
*/
public class TestMacsy {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Testing Macsy...");

		try
		{
			BlackBoardsAPI api = new BlackBoardsAPI(null,true);
			BlackBoard macsy = api.blackBoardLoad("Macsy");
			
			String testString = "This is a test doc. ";
			
			BBDoc aDoc = new BBDoc();
			aDoc.setField("Test",testString);
			macsy.insertNewDoc(aDoc);
			System.out.println("Inserted with ID = "+ aDoc.getID());
			
			BBDoc retreived = macsy.findDocByID(aDoc.getID());
			if((retreived!=null) && testString.equals(retreived.getField("Test") ))
				System.out.println("Retrieval test OK");
			else
				System.out.println("Hm something is wrong can not find the inserted doc");
			
			macsy.removeDoc(aDoc.getID());
		}
		catch(Exception e)
		{
			System.out.println("Some error occured: " + e.toString());
			System.exit(-1);
		}
		System.out.println("DONE");
		

	}

}
