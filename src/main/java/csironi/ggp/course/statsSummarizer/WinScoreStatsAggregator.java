package csironi.ggp.course.statsSummarizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class expects as input a folder containing a sub-folder for each game,
 * where each sub-folder contains scores and wins statistics of the tournament
 * for the game (usually this structure is created by the StatsSummarizer class).
 * The class aggregates wins and scores statistics of all games into single files.
 * It also creates a .csv file for each player algorithm in the tourney containing
 * the latex representations of the win percentage (with confidence interval) of
 * the algorithm for each game.
 *
 * @author C.Sironi
 *
 */
public class WinScoreStatsAggregator {

	public WinScoreStatsAggregator() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {

		/************************************ Prepare the folders *********************************/

		if(args.length != 2){
			System.out.println("Impossible to aggregate statistics. Specify both the absolute path of the folder containing statistics and the name of the aggragate statistics file.");
			System.out.println("This code will create two aggragated statistics files: [NameYouProvide]ScoreStatistics.csv and [NameYouProvide]WinsStatistics.csv.");
			return;
		}

		String sourceFolderPath = args[0];
		String resultFile = sourceFolderPath + "/" + args[1];

		System.out.println(sourceFolderPath);
		System.out.println(resultFile);

		File sourceFolder = new File(sourceFolderPath);

		if(!sourceFolder.isDirectory()){
			System.out.println("Impossible to find the directory with the statistics to aggragate.");
			return;
		}

		File[] statsDirs = sourceFolder.listFiles();

		File[] statsFiles;

		String gameKey;

		String scoresFile = resultFile + "ScoreStatistics.csv";

		System.out.println(scoresFile);

		String winsFile = resultFile + "WinsStatistics.csv";

		System.out.println(winsFile);

		writeToFile(scoresFile, "Game;Player;#Samples;MinScore;MaxScore;StandardDeviation;StdErrMean;AvgScore;ConfidenceInterval;MinExtreme;MaxExtreme;Robustness;");

		writeToFile(winsFile, "Game;Player;#Samples;MinPoints;MaxPoints;StandardDeviation;StdErrMean;AvgWin%;ConfidenceInterval;MinExtreme;MaxExtreme;Robustness;");

		BufferedReader br;
		String theLine;

		// For the folder of each game...
		for(int i = 0; i < statsDirs.length; i++){

			if(statsDirs[i].isDirectory() && statsDirs[i].getName() != null && (statsDirs[i].getName().endsWith("-Stats") || statsDirs[i].getName().endsWith("-stats") || statsDirs[i].getName().endsWith(".Statistics") || statsDirs[i].getName().endsWith(".statistics"))){

				writeToFile(scoresFile, ";");
				writeToFile(winsFile, ";");

				if(statsDirs[i].getName().endsWith("-Stats") || statsDirs[i].getName().endsWith("-stats")){
					gameKey = statsDirs[i].getName().substring(0, statsDirs[i].getName().length()-6);
				}else{
					String[] splitStatsDir = statsDirs[i].getName().split("\\.");
					gameKey = splitStatsDir[1];
				}

				System.out.println(gameKey);

				statsFiles = statsDirs[i].listFiles();

				for(int j = 0; j < statsFiles.length; j++){

					if(statsFiles[j].getName().equals("ScoreStats.csv")){

						try {
							br = new BufferedReader(new FileReader(statsFiles[j]));
							theLine = br.readLine(); // First line is headers
							theLine = br.readLine();

							// Forgot to do this when computing statistics, adding this temporary fix
							theLine = addCIExtremes(theLine);

							//System.out.println("4: " + theLine);

							writeToFile(scoresFile, gameKey + ";" + theLine);
							theLine = br.readLine();

							// Forgot to do this when computing statistics, adding this temporary fix
							theLine = addCIExtremes(theLine);

							writeToFile(scoresFile, gameKey + ";" + theLine);
							br.close();
						} catch (IOException e) {
							System.out.println("Exception when reading a file while aggregating the statistics.");
				        	e.printStackTrace();
						}

					}

					if(statsFiles[j].getName().equals("WinsStats.csv")){

						try {
							br = new BufferedReader(new FileReader(statsFiles[j]));
							theLine = br.readLine(); // First line is headers
							theLine = br.readLine();

							// Forgot to do this when computing statistics, adding this temporary fix
							theLine = addCIExtremes(theLine);

							writeToFile(winsFile, gameKey + ";" + theLine);
							theLine = br.readLine();

							// Forgot to do this when computing statistics, adding this temporary fix
							theLine = addCIExtremes(theLine);

							writeToFile(winsFile, gameKey + ";" + theLine);
							br.close();
						} catch (IOException e) {
							System.out.println("Exception when reading a file while aggregating the statistics.");
				        	e.printStackTrace();
						}

					}
				}

			}
		}

		// Once the wins and scores have been aggregated, create the LATEX code to put the results in a table

		// For the wins:

		List<String> orderedPlayerTypes = new ArrayList<String>();
		Map<String, Double> averagesSum = new HashMap<String, Double>();
		Map<String, Integer> numGames = new HashMap<String, Integer>();
		//Map<String, Integer> rubustness = new HashMap<String, Integer>();

		Map<String, String> latexData = new HashMap<String, String>();
		Map<String, Double> latexAvg = new HashMap<String, Double>();

		try {
			br = new BufferedReader(new FileReader(winsFile));
			theLine = br.readLine(); // First line is headers
			theLine = br.readLine(); // Second line is empty
			theLine = br.readLine();

			while(theLine != null){
				while(theLine != null && !theLine.equals(";")){
					String[] split = theLine.split(";");
					String playerType = split[1];
					if(!(orderedPlayerTypes.contains(playerType))){
						 orderedPlayerTypes.add(playerType);
						 averagesSum.put(playerType, new Double(0));
						 numGames.put(playerType, new Integer(0));
						 latexData.put(playerType, "");
						 latexAvg.put(playerType, new Double(0));
						 //rubustness.put(playerType, new Integer(0));
					}

					String stringAvg = split[7];
					double avg = Double.parseDouble(stringAvg);
					averagesSum.put(playerType, new Double(averagesSum.get(playerType).doubleValue() + avg));
					numGames.put(playerType, new Integer(numGames.get(playerType).intValue() + 1));

					String game = split[0];
					double ci = Double.parseDouble(split[8]);
					latexData.put(playerType, (latexData.get(playerType)+ game +";$"+ round(avg,1) + "(\\pm" + round(ci,2) + ")$;\n"));
					latexAvg.put(playerType, new Double(latexAvg.get(playerType).doubleValue() + round(avg,1)));

					theLine = br.readLine();
				}

				theLine = br.readLine();

			}

			br.close();

			writeToFile(winsFile, ";");

			for(String s : orderedPlayerTypes){
				double overallAvg = (averagesSum.get(s).doubleValue() / numGames.get(s).intValue());
				writeToFile(winsFile, "OverallAvg;" + s + ";" + ";;;;;" + overallAvg);
				String latexFile = resultFile + s + "-Latex.csv";
				double overallLatexAvg = (latexAvg.get(s).doubleValue() / numGames.get(s).intValue());
				writeToFile(latexFile, (latexData.get(s) + "\nOverallAvg;$" + round(overallLatexAvg,1) + "$;" ));
			}

		} catch (IOException e) {
			System.out.println("Exception when reading a file while aggregating the statistics.");
        	e.printStackTrace();
		}

	}

	private static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}

	/**
	 * Checks if the line already contains the extremes of the confidence interval, if not adds them to the line.
	 *
	 * The extremes of the confidence interval are [avg-CI, avg+CI].
	 *
	 * @param theLine
	 * @return
	 */
	private static String addCIExtremes(String theLine){

		//System.out.println("1: " + theLine);

		String[] splitLine = theLine.split(";");

		if(splitLine.length <= 8){

			try{
				double avg = Double.parseDouble(splitLine[6]);
				double ci = Double.parseDouble(splitLine[7]);

				theLine += ((avg-ci) + ";" + (avg+ci) + ";");

				//System.out.println("2: " + theLine);
			}catch(NumberFormatException e){
				System.out.println("Impossible to add CI extremes. Skipping.");
				e.printStackTrace();
			}
		}

		//System.out.println("3: " + theLine);

		return theLine;

	}

	private static void writeToFile(String filename, String message){
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename, true));
			out.write(message+"\n");
            out.close();
		} catch (IOException e) {
			System.out.println("Error writing file " + filename + ".");
			e.printStackTrace();
		}
	}

}