import java.util.HashMap;
import java.util.Map;

public class User implements Comparable<User>{
	public int uid = 0;
	Map<Integer, Rating> ratings = null;		//mapping movieID -> rating
	
	public User(int _id)
	{
		uid = _id;
		ratings = new HashMap<Integer, Rating>();
	}
	
	public double getMeanRating()
	{
		double result = 0;
		for(Rating r: ratings.values())
		{
			result += r.rating;
		}
		if(ratings.size() > 0) result /= (double)ratings.size();
		
		return result;
	}
	
	public void addRating(Movie movie, double rating, long timestamp)
	{
		Rating r = ratings.get(movie.getID());
		if(r == null) 
		{	r = new Rating(movie, rating, timestamp);
			ratings.put(movie.getID(), r);
		}
		else
		{
			if(r.timestamp < timestamp)
			{
				r.rating = rating;
				r.timestamp = timestamp;
			}
		}
	}
	
	public String toString()
	{
		StringBuilder  s = new StringBuilder();
		s.append("[user: "+uid+" rates "+ratings.size()+" movies]\n");
		for(Rating rat: ratings.values())
		{
			s.append("\t"+rat+"\n");
		}
		 
		 return s.toString();
	}

	@Override
	public int compareTo(User o) {
		return (new Integer(uid)).compareTo(o.uid);
	}
}
