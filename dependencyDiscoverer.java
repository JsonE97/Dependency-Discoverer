/*Jason Egan
  2191867e
  APH Exercise 2
  This is my own work as defined in the Academic Ethics agreement I have signed
 */


import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class dependencyDiscoverer {

	
	//create a concurrent hash map to store files mapped to their dependent list of files
	static ConcurrentHashMap<String, ArrayList<String>> nextStructure = new ConcurrentHashMap<String, ArrayList<String>>();
			
	//now create a concurrent linked list to store a work queue of files
			
	static ConcurrentLinkedQueue<String> workQ = new ConcurrentLinkedQueue<String>();
	
	static ArrayList<String> newDirs = new ArrayList<String>();
	


	/*function to determine whether the curFile actually exists in the directory*/
	
	public static File checkFile(String dir, String curFile){
		File newFile = new File(dir + "/" + curFile);
		if(newFile.exists()){
			return newFile;
		}
		return null;
	}
	
	
	/*function to process the linked list of work files for dependencies*/
	
	public static void processFile(String curFile, ArrayList<String> DPs, LinkedList<String> workFiles) throws IOException{

		//try/catch block to catch any IO exceptions
		try{

			//checks the current dir f in the newDirs list for any givens files 
			for(String f : newDirs){

				File checkFile = checkFile(f,curFile);
				

				//if the file exists then read from it
				if(checkFile != null){
					FileReader reader = new FileReader(checkFile);
					BufferedReader bfReader = new BufferedReader(reader);
					String line = bfReader.readLine();
					while(line != null){
						
						//if the current line contains an include statement and following quotation marks then 
						if(line.contains("#include") && line.contains("\"")){

							//split the line by quotation mark and get the item at index 1 where the file will be contained in the quotation marks
							String lines[] = line.split("\"");
							line = lines[1];
	
							//if the list of dependencies does not already contain that file then add it to the DP list and also the workFile list
							if(!DPs.contains(line)){
								DPs.add(line);
								workFiles.add(line);
							}
							
						}
						
						line = bfReader.readLine();
					}
					bfReader.close();
				}
			}
		}catch (Exception e){
			throw new IOException(e);	
		}
		
	}
	
	//create class for the worker thread(s) - dependent on multithread or not 
	public static class Worker implements Runnable{				
		
		//create a new array list of dependencies and newWorkFiles that i still need to check for dependencies
		private ArrayList<String> dependencies;
		private LinkedList<String> newWorkFiles;
		
		//public constructor for a worker to have its own list of dependencies and new work files to search for dependencies
		public Worker(){
			this.dependencies = new ArrayList<String>();
			this.newWorkFiles = new LinkedList<String>();
		}
		@Override
		public void run()  {
			//this continues until there are no further files to process
			//also check that there is a next element on the work Q to prevent null pointer exceptions
			while(!workQ.isEmpty() && (workQ.peek() != null)){
				String file = workQ.poll();
				try {
					processFile(file,dependencies, newWorkFiles);
				} catch (IOException e) {
					e.printStackTrace();
				}
				//once the most recent file from the concurrent workQ has been processed, process the files contained in it to further look for dependencies
				while(!newWorkFiles.isEmpty()){
					String dir = newWorkFiles.poll();
					//now process this new dir to look for dependency files
					try {
						processFile(dir, dependencies, newWorkFiles);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				//put the processed file into the concurrent structure alongside its list of dependencies
				nextStructure.put(file, dependencies);

				//resets the list of dependencies for the next file to be searched
				dependencies = new ArrayList<String>();
					
			}
				
		}	
	}
	
	
	public static void main(String[] args) throws InterruptedException {
				
		
		//determine number of fields in CPATH
		ArrayList<String> paths = new ArrayList<String>();
		String sysPaths = System.getenv("CPATH");
		int crawlerThreads;		


		//attempts to get the crawler threads, if its null then return as 2
		if(System.getenv("CRAWLER_THREADS") != null){
			crawlerThreads = Integer.parseInt(System.getenv("CRAWLER_THREADS"));
		}else{
			crawlerThreads = 2;
		}
		
		//similarly do the same for the paths
		if(sysPaths!= null){
			System.out.println(sysPaths);
			paths.addAll(Arrays.asList(sysPaths.split(":")));
		}
		
		
		//determine number of -Idir args
		ArrayList<String> dirs = new ArrayList<String>();
		for(int i = 1; i < args.length; i ++){
			if(args[i].contains("-I")){
				dirs.add(args[i].substring(2));
			}
		}
		
		//add the current working directory to be searched first		
		newDirs.add("./");
		//then add all the paths from the environment variable and then the given command dirs
		newDirs.addAll(paths);
		newDirs.addAll(dirs);
		
		
		//now go through the rest of the args
		for(String s : args){
			if(!s.contains("-I")){
				String ext = s.substring(s.lastIndexOf(".") + 1);
				if((ext != "c") || (ext != "y") || (ext != "l")){
					//if the file extension is valid then add it to the work Q
					workQ.add(s);
				}else{
					System.err.println("Illegal file extension");
				}
				
			}
		}

		
		//now initialise multi threads to run through directories
		ArrayList<Thread> threadList = new ArrayList<Thread>();
		for(int i = 0; i < crawlerThreads; i++){
			Thread t = new Thread(new Worker());
			threadList.add(t);
			t.start();
		}
	
		//loops through all the threads and joins them to ensure they have finished	
		for(int i = 0; i < threadList.size(); i++){
			threadList.get(i).join();
		}
	

		//finally iterate over the concurrent hash map and print out the contents
		printDependencies();
	}
	
	public static void printDependencies(){
		for(String key : nextStructure.keySet()){
			if(nextStructure.get(key) != null){
				//create an object file String so that we can print it out before the source file
				String objFile = "";
				objFile = key.substring(0, key.length()-1) + 'o';
				System.out.print(objFile + ": ");
				System.out.print(key);
				for(String deps : nextStructure.get(key)){
					System.out.print(" " + deps);
				}
			}
			System.out.print('\n');
		}
			
	
	}
	
	
	

	

}
