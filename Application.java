/**
 * @author Tamer
 * Apriori Frequent Itemset mining algorithm enhanced by Tamer Avci
 * Tested on: chess.dat & T10I4D100K.dat
 * T10I4D100K.dat with minimum support 500: <4 seconds
 * chess.dat with minimum support 2500 <6 seconds
 */
package freq;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; 


public class Application {

	/**
	 * dataPoint represents an itemset with its count
	 */
	class dataPoint{
		int count;
//		Set<Integer> tlist = new HashSet<Integer>();
		int[] itemset;
		dataPoint(int s, int[] A) {
			this.count = s;
			this.itemset = A;
		}
	}
	/**
	 * freqItemSets holds all the itemsets
	 * We generate one & two itemsets manually in the first scan of DB
	 */
	public static List<ConcurrentHashMap<String, dataPoint>> freqItemSets = new ArrayList<ConcurrentHashMap<String, dataPoint>>();
	public static ConcurrentHashMap<String, dataPoint> oneItemSet = new ConcurrentHashMap<String,dataPoint>();
	public static ConcurrentHashMap<String, dataPoint> twoItemSet = new ConcurrentHashMap<String,dataPoint>();
	private static String input;
	private static String output;
	private static int minsup;
	public static int itemset_size = 0;
	static BufferedReader data_input;
	public static int numTrans = 0;
	public static int rangeItems = 0;
	
	/**
	 * Main runs the algorithm and formats the output. Also keeps a timer
	 * @param args, 0=input data set, 1=minimum support, 2=output file
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 */
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		input = args[0]; minsup = Integer.parseInt(args[1]); output = args[2]; //configuration
		final PrintStream oldStdout = System.out;
		System.setOut(new PrintStream(new FileOutputStream(output)));
		data_input = new BufferedReader(new FileReader(input));
		long start = System.currentTimeMillis();
		run();
		long end = System.currentTimeMillis();
		data_input.close();
		System.setOut(oldStdout);
        System.out.println("Execution time is: "+((double)(end-start)/1000) + " seconds. Formatting output..");
		formatOutput();


	}
	

	/**
	 * Formats the output, sorts it according to the output desired
	 * Linux environment needed
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void formatOutput() throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("sed -i s/\\[//g "+output);
		p.waitFor();
		p = Runtime.getRuntime().exec("sed -i s/\\]//g "+output);
		p.waitFor();
		p = Runtime.getRuntime().exec("sed -i s/\\,//g "+output);
		p.waitFor();

		String[] cmd = new String[freqItemSets.size()+4];
		cmd[0] = "sort";

		int i=1;
		for(i=1; i<freqItemSets.size()+1; i++) {
			cmd[i]="-nk"+i;
		}
		cmd[i] = "-o";
		cmd[i+1] = output;
		cmd[i+2] = output;
		p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
	}
	/**
	 * Apriori algorithm
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void run() throws IOException, InterruptedException {
		
			List<List<Integer>> transDB = new ArrayList<List<Integer>>(); //storing the DB to avoid reading costs

			String line = data_input.readLine();
			
			while(line!=null) {
				
				numTrans++;
				StringTokenizer t = new StringTokenizer(line," ");
				List<Integer> row = new ArrayList<Integer>();
 

				
				//parse the line-create the one itemset
				while(t.hasMoreTokens()) {
					int x = Integer.parseInt(t.nextToken());
					row.add(x);
					if(x>rangeItems)
						rangeItems = x;
					int[] itemset = {x};
					String key = Arrays.toString(itemset);
					
					if(!oneItemSet.containsKey(key)) {
							oneItemSet.put(key, new Application().new dataPoint(1, itemset));
//							oneItemSet.get(key).tlist.add(numTrans);
					}
					else {
							oneItemSet.get(key).count++;
//							oneItemSet.get(key).tlist.add(numTrans);
					}
				}
				
				
				//add the row
				transDB.add(row);
				
				//find two itemset in each row
				for(int i=0; i< row.size()-1; i++) {
					for(int j=i+1; j<row.size(); j++) {
						int[] itemset = {(int) row.get(i), (int) row.get(j)};
						String key = Arrays.toString(itemset);
						if(!twoItemSet.containsKey(key)) {
							twoItemSet.put(key, new Application().new dataPoint(1, itemset));
							
						}
						else
							twoItemSet.get(key).count++;
					}
					
				}
				line=data_input.readLine();
			}
			
			
			
			//remove infrequent items
			List<Integer> trash = new ArrayList<Integer>();
			for(String key : oneItemSet.keySet()) {
				if(oneItemSet.get(key).count<minsup) {
					trash.add(oneItemSet.get(key).itemset[0]);
					oneItemSet.remove(key);
				}
					
			}
			
			//remove one-infrequent items from DB- Shrinking
			for(List<Integer> element : transDB) {
				for(int z=0; z<element.size(); z++) {
					if(trash.contains(element.get(z))) {
						element.remove(element.get(z));
					}
				}
			}
			
			//Write to file
			for(String key : oneItemSet.keySet()) {
				System.out.println(key + " " + "("+oneItemSet.get(key).count+")");
			}
			for(String key : twoItemSet.keySet()) {
				if(twoItemSet.get(key).count<minsup)
					twoItemSet.remove(key);
			}
			for(String key : twoItemSet.keySet()) {
				System.out.println(key + " " + "("+twoItemSet.get(key).count+")");
			}
			
			//add them to the freqItemSets
			freqItemSets.add(oneItemSet);
			freqItemSets.add(twoItemSet);
			
	//generate the next candidate set
	ConcurrentHashMap<String, dataPoint> nextItemSet = generateNextCanSet();

	while(nextItemSet.size()!=0) {		
		for(List<Integer> row : transDB) {
			boolean[] exist = new boolean[rangeItems+1]; //existence array
			Arrays.fill(exist, false);
			for(int t = 0; t<row.size(); t++) {
					exist[row.get(t)] = true;
			}
			boolean match;
			for(String key: nextItemSet.keySet()) { //go through each candidate
				match = true;
				int A[] = nextItemSet.get(key).itemset;

				for(int i = 0; i<A.length; i++) {
					
					if(exist[A[i]]==false) {
						match = false;
						break;
					}
				}
				if(match) { //if made this far increase the count all the items exist in the row
					nextItemSet.get(key).count++;
				}
		}

		
		}

		//prune infrequent items
		for(String key : nextItemSet.keySet()) {
			if(nextItemSet.get(key).count<minsup)
				nextItemSet.remove(key);
		}
		
		//write them to file
		for(String key : nextItemSet.keySet()) {
			System.out.println(key + " " + "("+nextItemSet.get(key).count+")");
		}
		
		nextItemSet = generateNextCanSet(); //generate next itemset until none can be generated
	}	
}

	/**
	 * generates a new candidateset
	 * @return new candidateset of size+1
	 */
	public static ConcurrentHashMap<String, dataPoint> generateNextCanSet() {
		ConcurrentHashMap<String, dataPoint> candidateSet = new ConcurrentHashMap<String, dataPoint>();
		ConcurrentHashMap<String, dataPoint> currentSet = freqItemSets.get(freqItemSets.size()-1); //last itemset
		
		for(String key : currentSet.keySet()) { //go through each itemset
			int[] A = currentSet.get(key).itemset;
			int k = A.length;
			for(String key2 : currentSet.keySet()) {//combine them with other itemsets with k-2 common elements
				boolean match = true;
				if(key2.equals(key))	
					continue;
				else {
					int[] B = currentSet.get(key2).itemset;
					for(int i = 0; i<=k-2; i++) {
						if (A[i]!=B[i])
							match = false;
					}
					if(match) { //match k-2 elements
						int[] C = new int[k+1];
						for(int i = 0; i<k; i++) {
							C[i] = A[i]; //new candidate itemset with k+1 elements
						}
						C[k] = B[k-1]; 
						Arrays.sort(C);
						//subset pruning
						int[] X = new int[k];
						int[] Y = new int[k];
						for(int c=0; c<k; c++) {
							X[c] = C[c+1];
							if(c>=1)
								Y[c] = C[c+1];
							else
								Y[c] = C[c];
						}
						//add it to the candidate set only if the subsets also exist
						if(currentSet.containsKey(Arrays.toString(X)) && currentSet.containsKey(Arrays.toString(Y)))
							candidateSet.put(Arrays.toString(C), new Application(). new dataPoint(0, C));	
					}
					
				}
				
			}

		}
		freqItemSets.add(candidateSet);
		return candidateSet;
	}
}
