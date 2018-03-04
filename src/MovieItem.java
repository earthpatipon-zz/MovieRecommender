
public class MovieItem implements Comparable<MovieItem>{
	private Movie movie;
	private double score = 0;
	
	public MovieItem(Movie _movie, double _score)
	{
		movie = _movie;
		score = _score;
	}
	
	
	
	public Movie getMovie() {
		return movie;
	}



	public double getScore() {
		return score;
	}



	@Override
	public int compareTo(MovieItem arg0) {
		if(arg0 instanceof MovieItem)
		{
			if(((MovieItem)arg0).score < score) return -1;
			else if (((MovieItem)arg0).score > score) return 1;
		}
		return 0;
	}
	
	@Override
	public String toString()
	{
		return movie.getID()+":"+score;
	}
	
}
