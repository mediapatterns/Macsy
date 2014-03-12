package macsy.blackBoardsSystem;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * This class is used to store a set of Articles.
 * 
 * Functions in Articles Black Board that return set of articles, use this class as output. 
 *   
 * @author Ilias Flaounas
 * 
 * @version     1.1                   
 * @since       2014-03-12
 * 
 */
public final class BBDocTagSet {

	/**
	 * A list of cursor rsults. 
	 */
	private List<DBCursor> cursors = null;
	
	/**
	 * A pointer to the cursor from witch the next article will be returned.
	 */
	private int currentCursor = 0; 

	/**
	 * Creates a new BBDocArticleSet populated with cursor result.
	 * @param cursor 
	 */
	BBDocTagSet(DBCursor cursor)
	{
		this.cursors =  new ArrayList<DBCursor>();
		
		if(cursor!=null)
			cursors.add(cursor);
		
		currentCursor = 0;
	}

	/**
	 * Creates a new BBDocArticleSet populated with all cursor results.
	 * @param cursors A list of cursors
	 */
	BBDocTagSet(List<DBCursor> cursors)
	{
		this.cursors = cursors;
		currentCursor = 0;
	}

	/**
	 * Each time it is called it returns the next tag, or null if no more tags are found. 
	 *  
	 * @return The next tag ID as Integer or null if there aren't any more tags. 
	 */
	public Integer getNextTagID()
	{
		while(currentCursor < cursors.size())
		{
			if(cursors.get(currentCursor).hasNext())
			{
				DBObject art = cursors.get(currentCursor).next();
				
				int tagID = (Integer) art.get(BlackBoard.TAG_ID);
	
				return tagID;
			}
			currentCursor ++;
		}
		return null;
	}
	
	/**
	 * Each time it is called it returns the next tag, or null if no more tags are found. 
	 *  
	 * @return The next tag Name as String or null if there aren't any more tags. 
	 */
	public String getNextTagName()
	{
		while(currentCursor < cursors.size())
		{
			if(cursors.get(currentCursor).hasNext())
			{
				DBObject art = cursors.get(currentCursor).next();
				
				String tagName = (String) art.get(BlackBoard.TAG_NAME);
	
				return tagName;
			}
			currentCursor ++;
		}
		return null;
	}

	/**
	 * Creates a clone of current set.
	 */
	public BBDocTagSet clone()
	{
		List<DBCursor> cursors_copy = new ArrayList<DBCursor>();
		for(DBCursor c: cursors)
			cursors_copy.add( c.copy() );
		
		return new BBDocTagSet(cursors_copy);
	}
}
