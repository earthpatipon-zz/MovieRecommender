import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

public class SimpleMovieRecommender implements BaseMovieRecommender {

	Map<Integer, Movie> movies;
	Map<Integer, User> users;
	int num_users;
	int num_movies;
	double[][] rating_matrix;
	double[][] usersim_matrix;
	
	@Override
	public Map<Integer, Movie> loadMovies(String movieFilename) {
		// TODO Auto-generated method stub
		if (movieFilename == null)
			return null;
		TreeMap<Integer, Movie> map = new TreeMap<Integer, Movie>();
		File file = new File(movieFilename);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			String pattern = "(\\d+),\"?(.+) \\((\\d+)\\)\"?,(.+)";
			Pattern p = Pattern.compile(pattern);
			while ((line = br.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.matches()) {
					Movie movie = new Movie(Integer.parseInt(m.group(1)), m.group(2), Integer.parseInt(m.group(3)));
					for (String tag : m.group(4).split("\\|")) {
						movie.addTag(tag);
					}
					map.put(movie.mid, movie);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	@Override
	public Map<Integer, User> loadUsers(String ratingFilename) {
		// TODO Auto-generated method stub
		if (ratingFilename == null)
			return null;
		TreeMap<Integer, User> map = new TreeMap<Integer, User>();
		File file = new File(ratingFilename);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			String pattern = "(\\d+),(\\d+),(\\d\\.\\d),(\\d+)";
			Pattern p = Pattern.compile(pattern);
			while ((line = br.readLine()) != null) {
				Matcher m = p.matcher(line);
				if (m.matches()) {
					int key = Integer.parseInt(m.group(1));
					if (!map.containsKey(key)) {
						User user = new User(key);
						map.put(user.uid, user);
					}
					map.get(key).addRating(movies.get(Integer.parseInt(m.group(2))), Double.parseDouble(m.group(3)),
							Long.parseLong(m.group(4)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	@Override
	public void loadData(String movieFilename, String userFilename) {
		// TODO Auto-generated method stub
		movies = this.loadMovies(movieFilename);
		users = this.loadUsers(userFilename);
	}

	@Override
	public Map<Integer, Movie> getAllMovies() {
		// TODO Auto-generated method stub
		if (this.movies != null)
			return this.movies;
		else {
			TreeMap<Integer, Movie> empty = new TreeMap<Integer, Movie>();
			return empty;
		}
	}

	@Override
	public Map<Integer, User> getAllUsers() {
		// TODO Auto-generated method stub
		if (this.users != null)
			return this.users;
		else {
			TreeMap<Integer, User> empty = new TreeMap<Integer, User>();
			return empty;
		}
	}

	@Override
	public void trainModel(String modelFilename) {
		// TODO Auto-generated method stub
		String address = modelFilename;
		int counter = 0;
		int uSize = this.users.size();
		int mSize = this.movies.size();
		int row = 0;
		int col = 0;
		// Create information before write to file
		StringBuilder str = new StringBuilder();
		str.append("@NUM_USERS " + uSize + "\n");
		str.append("@USER_MAP {");
		// Print loop for users
		for (User i : this.users.values()) {
			str.append(counter + "=" + i.uid);
			if (counter == uSize - 1) {
				str.append("}\n");
				counter = 0;
				break;
			}
			str.append(", ");
			counter++;
		}
		str.append("@NUM_MOVIES " + mSize + "\n");
		str.append("@MOVIE_MAP {");
		// Print loop for movies
		for (Movie i : this.movies.values()) {
			str.append(counter + "=" + i.mid);
			if (counter == mSize - 1) {
				str.append("}\n");
				counter = 0;
				break;
			}
			str.append(", ");
			counter++;
		}
		// Rating Matrix
		str.append("@RATING_MATRIX\n");
		double[][] R = new double[uSize][mSize];
		double[] avrRating = new double[uSize];
		for (User i : this.users.values()) {
			for (Movie j : this.movies.values()) {
				if (i.ratings.containsKey(j.mid)) {
					R[row][col] = i.ratings.get(j.mid).rating;
					str.append(R[row][col] + " ");
				} else if (counter != mSize) {
					str.append("0.0 ");
				}
				col++;
				counter++;
			}
			avrRating[row] = i.getMeanRating();
			str.append(avrRating[row] + "\n");
			col = 0;
			counter = 0;
			row++;
		}
		// Usersim Matrix
		str.append("@USERSIM_MATRIX\n");
		double[][] S = new double[uSize][uSize];
		Set<Integer> intersection = new HashSet<Integer>();
		row = 0;
		col = 0;

		for (User u : this.users.values()) {
			double ratingAvr_u = u.getMeanRating();
			for (User v : this.users.values()) {
				if (row == col) {
					S[row][col] = 1;
				} else if (row > col) {
					S[row][col] = S[col][row];
				} else {
					intersection = Sets.intersection(u.ratings.keySet(), v.ratings.keySet());
					// use huge time ^^
					if (!intersection.isEmpty()) {
						double ratingAvr_v = v.getMeanRating();
						double numerator_u = 0;
						double numerator_v = 0;
						double numerator_total = 0;
						double denominator_u = 0;
						double denominator_v = 0;
						double Similarity = 0;
						for (int i : intersection) {
							numerator_u = u.ratings.get(i).rating - ratingAvr_u;
							numerator_v = v.ratings.get(i).rating - ratingAvr_v;
							numerator_total += numerator_u * numerator_v;
							denominator_u += Math.pow(u.ratings.get(i).rating - ratingAvr_u, 2);
							denominator_v += Math.pow(v.ratings.get(i).rating - ratingAvr_v, 2);
						}
						if (denominator_u == 0 || denominator_v == 0) {
							S[row][col] = 0;
						} else {
							double denominator_total = Math.sqrt(denominator_u) * Math.sqrt(denominator_v);
							Similarity = numerator_total / denominator_total;
							if (Similarity >= -1 && Similarity <= 1)
								S[row][col] = Similarity;
							else {
								if (Similarity < -1)
									S[row][col] = -1;
								else
									S[row][col] = 1;
							}
						}
					} else {// intersection = empty
						S[row][col] = 0;
					}
				}
				str.append(S[row][col] + " ");
				col++;
			}
			col = 0;
			str.append("\n");
			row++;
		}

		// Write to file
		Writer writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(address + ".model"), "utf-8"));
			writer.write(str.toString());
		} catch (

		IOException ex)

		{
			ex.printStackTrace();
		} finally

		{
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void loadModel(String modelFilename) {
		// TODO Auto-generated method stub
		File file = new File(modelFilename);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String line;
			String pattern_num_users = "@NUM_USERS (\\d+)";
			String pattern_user_map = "@USER_MAP {(.+)}";
			String pattern_num_movies = "@NUM_MOVIES (\\d+)";
			String pattern_movie_map = "@MOVIE_MAP {(.+)}";
			String pattern_rating_matrix = "@RATING_MATRIX";
			String pattern_usersim_matrix = "@USERSIM_MATRIX";
			Pattern p_num_users = Pattern.compile(pattern_num_users);
			Pattern p_user_map = Pattern.compile(pattern_user_map);
			Pattern p_num_movies = Pattern.compile(pattern_num_movies);
			Pattern p_movie_map = Pattern.compile(pattern_movie_map);
			Pattern p_rating_matrix = Pattern.compile(pattern_rating_matrix);
			Pattern p_usersim_matrix = Pattern.compile(pattern_rating_matrix);
			Matcher m;
			while ((line = br.readLine()) != null) {
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public double predict(Movie m, User u) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<MovieItem> recommend(User u, int fromYear, int toYear, int K) {
		// TODO Auto-generated method stub
		return null;
	}

}
