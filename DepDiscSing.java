import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DepDiscSing {

	//now create a concurrent hash map to store files mapped to their dependent list of files
	static ConcurrentHashMap<String, ArrayList<String>> nextStructure = new ConcurrentHashMap<String, ArrayList<String>>();
			
	//now create a concurrent linked list to store a work queue of files
			
	static ConcurrentLinkedQueue<String> workQ = new ConcurrentLinkedQueue<String>();
	
	static ArrayList<String> newDirs = new ArrayList<String>();
	
	
	public static File checkFile(String dir, String curFile){
		File newFile = new File(dir + "/" + curFile);
		if(newFile.exists()){
			return newFile;
		}
		return null;
	}
	
	
	
	public static void processFile(String curFile, ArrayList<String> DPs, LinkedList<String> workFiles) throws IOException{
		try{
			for(String f : newDirs){
				File checkFile = checkFile(f,curFile);
				if(checkFile != null){
					FileReader reader = new FileReader(checkFile);
					BufferedReader bfReader = new BufferedReader(reader);
					String line = bfReader.readLine();
					while(line != null){
						if(line.contains("#include") && line.contains("\"")){
							String lines[] = line.split("\"");
							line = lines[1];
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
			
		}
		
	}
	
	//create class for the worker thread(s) - dependent on multithread or not 
	public static class Worker implements Runnable{
		
		//create a new array list of dependencies and newWorkFiles that i still need to check for dependencies
		ArrayList<String> dependencies = new ArrayList<String>();
		LinkedList<String> newWorkFiles = new LinkedList<String>();
		@Override
		public void run() {
			while(!workQ.isEmpty()){
				String file = workQ.poll();
				try {
					processFile(file,dependencies, newWorkFiles);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				while(!newWorkFiles.isEmpty()){
					String dir = newWorkFiles.poll();
					//now process this new dir to look for dependency files
					try {
						processFile(dir, dependencies, newWorkFiles);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				nextStructure.put(file, dependencies);
				dependencies = new ArrayList<String>();
					
			}
				
		}	
	}
	
	
	public static void main(String[] args) throws InterruptedException {
		
		
		//determine number of fields in CPATH
		ArrayList<String> paths = new ArrayList<String>();
		String sysPaths = System.getenv("CPATH");
		
		if(System.getenv("CRAWLER_THREADS") != null){
			int crawlerThreads = Integer.parseInt(System.getenv("CRAWLER_THREADS"));
		}

		if(sysPaths!= null){
			paths.addAll(Arrays.asList(sysPaths.split(":")));
		}
		

		
		//determine number of -Idir args
		ArrayList<String> dirs = new ArrayList<String>();
		for(int i = 1; i < args.length; i ++){
			if(args[i].contains("-I")){
				dirs.add(args[i].substring(2));
			}
		}
		
		
		newDirs.add("./");
		newDirs.addAll(paths);
		newDirs.addAll(dirs);
		int crawlerThreads = 1;		
		
		//now go through the rest of the args
		for(String s : args){
			if(!s.contains("-I")){
				String ext = s.substring(s.lastIndexOf(".") + 1);
				if((ext != "c") || (ext != "y") || (ext != "l")){
					workQ.add(s);
				}else{
					System.err.println("Illegal file extension");
				}
				
			}
		}
		
		long start = System.currentTimeMillis();
		//now initialise single thread to run through directories
		Thread t = new Thread(new Worker());
		t.start();
		
	t.join();
	printDependencies();
	long end  = System.currentTimeMillis();
	System.out.println(end-start);
	}
	
	public static void printDependencies(){
		for(String key : nextStructure.keySet()){
			if(nextStructure.get(key) != null){
				String objFile = "";
				objFile = key.substring(0, key.length()-1) + 'o';
				System.out.print(objFile + ": ");
				System.out.print(key + " ");
				for(String deps : nextStructure.get(key)){
					System.out.print(deps + " ");
				}
			}
			System.out.print('\n');
		}
			
	
	}
	
	
	

	

}
