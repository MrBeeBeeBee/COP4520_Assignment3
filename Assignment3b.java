import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.io.File;
import java.io.PrintWriter;

public class Assignment3b
{
	// Records the record highs and lows of each minute (extremeSets),
	// and ultimately each hour (recordHigh & recordLow) gathered by the probes
	private static final List<Integer> recordHighs = Collections.synchronizedList(new ArrayList<>());
	private static final List<Integer> recordLows = Collections.synchronizedList(new ArrayList<>());
	private static final List<Integer[]> extremeSets = Collections.synchronizedList(new ArrayList<>());
	
	private static final int NUM_THREADS = 8;
	private static int currentHour = 0;

	// Each probe generates a random number from -100 to 70
	private static class Probe implements Runnable
	{
		@Override
		public void run() {
			int temp = (int) (Math.random() * 171) - 100;
			recordExtremes(temp);
		}
	}

	// Store the top 5 highest and lowest temperatures
	public static synchronized void recordExtremes(int temp)
	{
		// Record the highest temperatures. Fill first.
		if (recordHighs.size() <= 4)
		{
			recordHighs.add(temp);
		}
		else
		{
			// When full, replace the lowest
			for (int i = 0; i < recordHighs.size(); i++)
			{
				if (temp > recordHighs.get(i))
				{
					recordHighs.set(i, temp);
					break;
				}
			}
		}

		// Record the lowest temperatures. Fill first.
		if (recordLows.size() <= 4)
		{
			recordLows.add(temp);
		}
		else
		{
			// When full, replace the highest
			for (int i = 0; i < recordLows.size(); i++)
			{
				if (temp < recordLows.get(i))
				{
					recordLows.set(i, temp);
					break;
				}
			}
		}

		// Reverse the list because we want the display as high -> low
		Collections.sort(recordHighs);
		Collections.reverse(recordHighs);

		Collections.sort(recordLows);
	}

	// Pinpoints the record highs and lows for each hour and
	// puts them together in a set so they can be easily found
	private static void calculateExtremes()
	{
		Integer[] extremeSet = new Integer[2];

		extremeSet[0] = recordHighs.get(4);
		extremeSet[1] = recordLows.get(0);

		extremeSets.add(extremeSet);
	}
	
	// Creates a hourlyReports folder and writes each report to its own .txt file
	private static void generateHourlyReport()
	{
		try
		{
			// Create a new folder to store the reports in
			new File("hourlyReports").mkdirs();
			PrintWriter printWriter = new PrintWriter("hourlyReports/Hour" + (currentHour < 10 ? "0" : "") + currentHour + ".txt");
			
			printWriter.println("Report For Hour " + (currentHour < 10 ? "0" : "") + currentHour + "\n");

			// Print out the 5 highest temperatures
			printWriter.println("Highest Temperatures:");
			for (Integer highTemp : recordHighs)
			{
				printWriter.print((highTemp < 0 ? "" : " ") + highTemp + "F ");
			}

			printWriter.print("\n\n");

			// Print out the 5 lowest temperatures
			printWriter.println("Lowest Temperatures:");
			for (Integer lowTemp : recordLows)
			{
				printWriter.print((lowTemp < 0 ? "" : " ") + lowTemp + "F ");
			}

			printWriter.print("\n\n");

			int largestChange = 0;
			int startMinute = 0;

			// Calculate and print out the greatest difference
			// in temperature over a 10 minute interval
			for (int i = 0; i < extremeSets.size() - 10; i++)
			{
				int high = extremeSets.get(i)[0];
				int low = extremeSets.get(i + 10)[1];
				int change = high - low;

				if (change > largestChange)
				{
					largestChange = change;
					startMinute = i;
				}
			}

			// Print out the greatest temperature change and the interval it occurred over
			printWriter.println("Largest Temperature Change Over A 10 Minute Interval:");
			printWriter.println(largestChange + "F\n");

			printWriter.println("Interval Occurred Over:");
			printWriter.print((currentHour < 10 ? "0" : "") + currentHour + ":" + startMinute + " - ");
			printWriter.print((currentHour < 10 ? "0" : "") + currentHour + ":" + (startMinute + 10));
			
			printWriter.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// Cleans up main & wipes the data for the next loop
	private static void clearData()
	{
		recordHighs.clear();
		recordLows.clear();
		extremeSets.clear();
	}

	public static void main(String[] args)
	{
		// Start tracking the time for the end report
		long startTime = System.currentTimeMillis();

		System.out.println("Creating the Probes");
		// Create Probes
		Probe[] probes = new Probe[NUM_THREADS];
		for (int i = 0; i < NUM_THREADS; i++)
		{
			probes[i] = new Probe();
		}

		System.out.println("Beginning Data Collection");
		// Run Probe Manager for 48 hours
		for (int h = 0; h < 48; h++)
		{
			// Track the extremes of each minute
			currentHour = h;
			for (int m = 0; m < 60; m++)
			{
				// Create threads and start them
				Thread[] threads = new Thread[NUM_THREADS];

				for (int i = 0; i < NUM_THREADS; i++)
				{
					threads[i] = new Thread(probes[i]);
					threads[i].start();
				}

				// Wait for the threads to finish
				for (Thread thread : threads)
				{
					try
					{
						thread.join();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				calculateExtremes();
			}

			System.out.println("Filling Out Daily Report #" + h);

			// Generate the hourly report for each hour & clean the slate
			generateHourlyReport();
			clearData();
		}

		// Calculate the duration of the program
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		long simplifiedTime = totalTime/1000;

		System.out.println("Total runtime is " + totalTime + 
		" milliseconds, or " + simplifiedTime + " seconds.");
	}
}
