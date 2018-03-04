import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.Sets;

public class Evaluator {
	
	public double[] rmse = null;
	public double avgRmse = 0;
	public double[] corr = null;
	public double avgCorr = 0;
	private final int CORR_THRESH = 5;
	public double[] pre = null;
	public double avgPre = 0;
	public double[] rec = null;
	public double avgRec = 0;
	public double[] f1 = null;
	public double avgF1 = 0;
	
	List<User> testUsers = null;
	BaseMovieRecommender r = null;
	
	/**
	 * Computes the RMSE of expected[] and actual[]
	 * @param expected
	 * @param actual
	 * @return
	 */
	public static double getRMSE(double[] expected, double[] actual)
	{	long n = 0; 
		double sum = 0;  
		double max = -Double.MAX_VALUE; 
		double min = Double.MAX_VALUE; 
		
		for(int i = 0; i < expected.length; i++)
		{
			n++; 
			sum += Math.pow(expected[i] - actual[i], 2); 
			max = Math.max(max, expected[i]); 
			min = Math.min(min, expected[i]);
		}
		
		double rmse = Math.sqrt(sum / n);
		
		return rmse;
	}
	
	
	
	/**
	 * Computes precision, recall, and F1 (in percentages) of the recommended results
	 * @param recommended
	 * @param u
	 * @return
	 */
	public static double[] getPreRecF1(List<MovieItem> recommended, User u)
	{
		double[] result = new double[3];
		Set<Integer> retrieved = new HashSet<Integer>();
		for(MovieItem m: recommended)
		{
			retrieved.add(m.getMovie().mid);
		}
		Set<Integer> actual = new HashSet<Integer>();
		for(Rating rat: u.ratings.values())
		{
			if(rat.rating >= 4) actual.add(rat.m.mid);
		}
		
		
		Set<Integer> common = Sets.intersection(retrieved, actual);
		if(retrieved.size() != 0)	result[0] = 100.0*((double)common.size())/((double)retrieved.size());
		if(actual.size() != 0)	result[1] = 100.0*((double)common.size())/((double)actual.size());
		if(result[0] + result[1] != 0) result[2] = (2.0*result[0]*result[1])/(result[0] + result[1]);
		
		return result;
	}
	
	/**
	 * Constructor. Taking the trained recommender "_r", and the test user filename "testUserFilename"
	 * @param _r
	 * @param testUserFilename
	 */
	public Evaluator(BaseMovieRecommender _r, String testUserFilename)
	{	r = _r;
		testUsers = new Vector<User>();
		testUsers.addAll(r.loadUsers(testUserFilename).values());
		rmse = new double[testUsers.size()];
		corr = new double[testUsers.size()];
		pre = new double[testUsers.size()];
		rec = new double[testUsers.size()];
		f1 = new double[testUsers.size()];
	}
	
	/**
	 * Averaging correlation coefficients by Fisher-transforming each corr into a z value,
	 * Take the avg of the z values, then transform it back to avg corr
	 * @param c
	 */
	public static double getAvgCorr(double[] cs)
	{
		int count = 0;
		double result = 0;
		for(double c: cs)
		{
			if(!Double.isFinite(c)) continue;
			if(c < -1 || c > 1) continue;
			double z = 0;
			if(c == 1.0)z = 3;
			else if(c == -1.0) z = -3;
			else
			{
				z = 0.5*Math.log((1.0+c)/(1.0-c));
			}
			result += z;
			count++;
		}
		if(count > 0) result/=(double)count;
		result = (Math.exp(2.0*result)-1.0)/(Math.exp(2.0*result)+1.0);
		if(!Double.isFinite(result)) result = 0;
		return result;
	}
	
	/**
	 * Evaluates both the rating prediction and movie recommendation. Produce a report file that details
	 * both the meta and individual evaluations.
	 * @param fromYear
	 * @param toYear
	 * @param K
	 * @param reportFilename
	 */
	public void evaluate(int  fromYear, int toYear, int K, String reportFilename)
	{
		StringBuilder reportStr = new StringBuilder();
		PearsonsCorrelation corrEngine = new PearsonsCorrelation();
		for(int i = 0; i < testUsers.size(); i++)
		{
			User u = testUsers.get(i);
			//eval prediction accuracy
			double[] actualRatings = new double[u.ratings.size()];
			double[] predictedRatings = new double[u.ratings.size()];
			int count = 0;
			reportStr.append("UID "+u.uid+"\n");
			reportStr.append("MID ");
			for(Rating rat: u.ratings.values())
			{
				actualRatings[count] = rat.rating;
				predictedRatings[count] = r.predict(rat.m, u);
				count++;
				reportStr.append(rat.m.mid+" ");
			}
			reportStr.append("\n");
			reportStr.append("ACTUAL "+StringUtils.join(actualRatings, ' ')+"\n");
			reportStr.append("PREDICTED "+StringUtils.join(predictedRatings, ' ')+"\n");
			
			rmse[i] = getRMSE(predictedRatings, actualRatings);
			avgRmse+=rmse[i];
			try{
				if(actualRatings.length < CORR_THRESH) throw new Exception();
				corr[i] = corrEngine.correlation(predictedRatings, actualRatings);
				if(Double.isNaN(corr[i])) throw new Exception();
				avgCorr += corr[i];
			}catch(Exception e)
			{
				corr[i] = -99;	
			}

			//eval recommendation
			List<MovieItem> recommendedMovies = r.recommend(u, fromYear, toYear, K);
			reportStr.append("RECOMMENDED "+StringUtils.join(recommendedMovies, " ")+"\n");
			
			double[] prf = getPreRecF1(recommendedMovies, u);
			pre[i] = prf[0];
			avgPre += pre[i];
			rec[i] = prf[1];
			avgRec += rec[i];
			f1[i] = prf[2];
			avgF1 += f1[i];
			reportStr.append("RMSE "+rmse[i]+"\n");
			reportStr.append("CORRELATION "+corr[i]+"\n");
			reportStr.append("Precision "+pre[i]+"\n");
			reportStr.append("Recall "+rec[i]+"\n");
			reportStr.append("F1 "+f1[i]+"\n\n");
		}
		
		//finalizing
		avgRmse /= (double)testUsers.size();
		avgPre /= (double)testUsers.size();
		avgRec /= (double)testUsers.size();
		avgF1 /= (double)testUsers.size();
		avgCorr = getAvgCorr(corr);
		String summaryStr = "AVG_RMSE "+avgRmse+
				"\nAVG_CORRELATION "+avgCorr+
				"\nAVG_PRECISION "+avgPre+
				"\nAVG_RECALL "+avgRec+
				"\nAVG_F1 "+avgF1+"\n\n";
		try {
			FileUtils.write(new File(reportFilename), summaryStr+reportStr.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public String toString()
	{
		String str = "Evaluation Result:\n";
		str += "RMSE = "+avgRmse+"\n";
		str += "Correlation = "+avgCorr+"\n";
		str += "Precision = "+avgPre+"\n";
		str += "Recall = "+avgRec+"\n";
		str += "F1 = "+avgF1+"\n";
		
		return str;
	}
}
