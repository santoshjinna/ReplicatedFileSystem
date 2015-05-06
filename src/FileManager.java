import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;


public class FileManager {
	
	public int nodeNumber;
	public HashMap<Integer,String> nodeMap = new HashMap<Integer,String>();
	
	public int N;
	public int VN;
	public int RU;
	public int DS;
	
	public static int numberFiles;
	public int operations;
	public int delay;
	public Object syncLock = new Object();
	public volatile boolean[] fileWriteLock;
	public volatile boolean[] fileReadLock;
	public volatile boolean gotReplies = false;
	public int serverPort;
	public int readoverwrite;
	public int backoffmin;
	public int backoffmax;
	public static int totalNumber;
	
	public volatile Vector<FileDetails> listOfFDs= new Vector<FileDetails>();
	public volatile ArrayList<Integer>[] listReadRequests;
	public FileDetails[] files;
	int count = 0;
	
    public int[][] nativeCSVectors;
	
	public int[][] LVWs;
	public int[][] LVRs;
	public int[][] csCounts;
	
	@SuppressWarnings("unchecked")
	public FileManager(int nodeno){
		parseConfigFile("config.txt");
		this.nodeNumber = nodeno;
		
		files = new FileDetails[numberFiles];
		fileWriteLock = new boolean[numberFiles];
		fileReadLock = new boolean[numberFiles];
		listReadRequests = new ArrayList[numberFiles];
		
        nativeCSVectors=new int[numberFiles][4];
		
		LVWs = new int[numberFiles][N];
		LVRs = new int[numberFiles][N];
		csCounts = new int[numberFiles][N];
		
		initializeDS();
		
		String add = nodeMap.get(nodeNumber);
		String[] ips = add.split(":");
		Server serv = new Server(this,Integer.parseInt(ips[1]));
		Thread serverThread = new Thread(serv);
		serverThread.start();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void parseConfigFile(String fileName){
		//parse config file and set values
		BufferedReader br;
		totalNumber = -1;
		numberFiles = operations = delay = readoverwrite = backoffmin = backoffmax =-1;
		int linecount = 0;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			while((line = br.readLine()) != null){
				if(line.length() == 0);
				else if(line.charAt(0) == '#');
				else if(totalNumber == -1){
					totalNumber = Integer.parseInt(line.trim());
					linecount = totalNumber;
					N=totalNumber;
				}
				else if(linecount>0){
					line = line.trim().replaceAll("(\t)+", ",");
					//Initializing the Hashmap with the node configuration
					nodeMap.put(Integer.parseInt(line.split(",")[0]), line.split(",")[1] + ":" + line.split(",")[2]);
					linecount--;
				}else if(numberFiles == -1){
					numberFiles = Integer.parseInt(line.trim());
					//createFile();
				}
				else if(operations == -1)
					operations = Integer.parseInt(line.trim());
				else if(delay == -1)
					delay = Integer.parseInt(line.trim());
				else if(readoverwrite == -1)
					readoverwrite = Integer.parseInt(line.trim());
				else if(backoffmin == -1){
					backoffmin = Integer.parseInt(line.trim().split(" ")[0]);
					backoffmax = Integer.parseInt(line.trim().split(" ")[1]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private static void createFile(){
		int dirs = totalNumber;
		while(dirs>0){
			File i = new File("Machine"+dirs);
			i.getParentFile().mkdirs();
			int a = numberFiles;
			while(a>0){
				try {
					i = new File("Machine"+dirs+"/file"+a + ".txt");
					i.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				a--;
			}
			dirs--;
		}
	}
	
	public void initializeDS(){
		for(int i=0;i<numberFiles;i++){
			try {
				files[i]= new FileDetails(i,0,N,0,formFile(i),nodeNumber);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			listReadRequests[i] = new ArrayList<Integer>();
			for(int j=0;j<N;j++){
				LVWs[i][j]=-1;
				LVRs[i][j]=-1;
				csCounts[i][j]=0;
				
			}
			
			nativeCSVectors[i][0]=-1;
			nativeCSVectors[i][1]=-1;
			nativeCSVectors[i][2]=-1;
			nativeCSVectors[i][3]=0;
		}
	}
	
	public boolean write_enter(int fileNo, String addend) throws IOException{
		if(askWritePermissions(fileNo)){
			write(addend,fileNo);
			return true;
		}
		
		return false;
		
	}
	
	public String read_enter(int fileNo) throws IOException{
		if(askReadPermissions(fileNo)){
			return read(fileNo);
		}
		return null;
	}
	
	public boolean askWritePermissions(int fileNumber) throws IOException{
		do{
			
			if(acquireWriteLock(fileNumber)){
				synchronized(this){
					if(!listOfFDs.contains(files[fileNumber]))
						listOfFDs.add(files[fileNumber]);
				}
				for(int i=0; i<N; i++){
					if(i!=nodeNumber)
					    sendWriteRequest(i,fileNumber);
				}
				//wait untill it got all replies
				while(!gotReplies){
					
				}	
				//check for necessary conditions
				checkForMEvalidity();
				
				if(checkForPermissions()){
					return true;
				}else{
					synchronized(this){
					    fileWriteLock[fileNumber]=false;
					}
					releaseLocks();
					int wait = backoffmin;
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(wait < backoffmax)
						wait *= 2;
					else
						return false;
				}
			}
		}while(true);
	}
	
	public boolean askReadPermissions(int fileNumb) throws IOException{
		do{
			
			if(acquireReadLock(fileNumb,nodeNumber)){
				// not printed properly
				synchronized(this){
					if(!listOfFDs.contains(files[fileNumb]))
							listOfFDs.add(files[fileNumb]);
				}
				for(int i=0; i<N; i++){
					if(i!=nodeNumber)
					    sendReadRequest(i,fileNumb);
				}
				//wait untill it got all replies
				while(!gotReplies){
					
				}	
				//check for necessary conditions
				checkForMEvalidity();
				
				if(checkForPermissions()){
					return true;
				}else{
					synchronized(this){
					    listReadRequests[fileNumb].remove(Integer.valueOf(nodeNumber));
					    if(listReadRequests[fileNumb].isEmpty())
					    	fileReadLock[fileNumb] = false;
					}
					releaseLocks();
					int wait = backoffmin;
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(wait < backoffmax)
						wait *= 2;
					else
						return false;
				}
			}
		}while(true);
	}	
	
	public boolean checkForPermissions(){
		int maxVN=0,maxRU=0, maxVNCount=0, maxDS=-1;
		for(FileDetails fd:listOfFDs){
				if(fd.VN>=maxVN){
					maxVN=fd.VN;
					maxRU=fd.RU;
				}
		}	
		for(FileDetails f:listOfFDs){
			    if(f.VN==maxVN){
			    	maxVNCount++;
			    	maxDS = f.DS;
			    }
		}
		if(maxVNCount > maxRU/2){
				return true;
		}
		else if(maxVNCount == maxRU/2){
			for(FileDetails fd1:listOfFDs){
				if(fd1.nodeNo == maxDS){
					return true;
				}
			}
		}
		return false;
	}
	
	public void write(String addend,int fileNo) throws IOException{
		int maxVN=0;
		FileDetails maxFD=null;
		for(FileDetails fd:listOfFDs){
			if(fd.VN>=maxVN){
				maxFD = fd;
				maxVN = fd.VN;
			}
		}
		addend = addend +" "+maxVN;
		byte[] fileArray = maxFD.byteFile;
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("Machine"+(nodeNumber+1)+"/file"+fileNo+".txt"));
		//bos.write(fileArray);
		byte[] strBytes = addend.getBytes();
		byte[] bytesToCopy;
		if(fileArray!=null)
		    bytesToCopy = new byte[fileArray.length+strBytes.length];
		else
			bytesToCopy = new byte[strBytes.length];
		System.arraycopy(fileArray, 0, bytesToCopy, 0, fileArray.length);
		System.arraycopy(strBytes, 0, bytesToCopy, fileArray.length, strBytes.length);
		
		bos.write(bytesToCopy);
		bos.close();
		
		nativeCSVectors[fileNo][0]=maxVN;
		System.out.println("testing version number: "+maxVN+" with size: "+listOfFDs.size());
		for(FileDetails test:listOfFDs){
			System.out.println(test.toString());
		}
		
		byte[] fileBytes = formFile(fileNo);
		FileDetails updated = new FileDetails(fileNo,maxVN+1,listOfFDs.size(),nodeNumber,fileBytes,nodeNumber);
		files[fileNo]=updated;
		for(FileDetails fd:listOfFDs){
			if(fd.nodeNo!=nodeNumber)
			    sendWriteLockRelease(updated, fd.nodeNo);
		}
		
		nativeCSVectors[fileNo][2]=maxVN+1;
		
		nativeCSVectors[fileNo][3]=nativeCSVectors[fileNo][3]+1;		
		
		synchronized(this){
		    fileWriteLock[fileNo]=false;
		}
		listOfFDs.clear();
		gotReplies=false;
		count=0;
	}
	
	public String read(int fileNumb) throws IOException{
		int maxVN=0;
		FileDetails maxFD=null;
		for(FileDetails fd:listOfFDs){
			if(fd.VN >= maxVN){
				maxFD = fd;
				maxVN = fd.VN;
			}
		}
		
		byte[] fileArray = maxFD.byteFile;
		String readString;
		if(fileArray!=null)
			readString = new String(fileArray);
		else
			readString="";
		
        nativeCSVectors[fileNumb][1]=maxVN;
        nativeCSVectors[fileNumb][3]=nativeCSVectors[fileNumb][3]+1;		
        
		for(FileDetails fd:listOfFDs){
			if(fd.nodeNo!=nodeNumber)
			    sendReadRelease(fd.nodeNo,fd.fileNumber);
		}
		
		synchronized(this){
		    listReadRequests[fileNumb].remove(Integer.valueOf(nodeNumber));
		    if(listReadRequests[fileNumb].isEmpty()){			
			    fileReadLock[fileNumb] = false;
			}
		}	
		listOfFDs.clear();
		gotReplies=false;
		count=0;       
        
        return readString;
	}
	
	public byte[] formFile(int fileNumb) throws IOException{
		File myFile = new File ("Machine"+(nodeNumber+1)+"/file"+fileNumb+".txt");
        byte [] mybytearray  = new byte [(int)myFile.length()];
        FileInputStream fis = new FileInputStream(myFile);
        BufferedInputStream bis = new BufferedInputStream(fis);
        bis.read(mybytearray,0,mybytearray.length);
        bis.close();
        
        return mybytearray;
	}
	
	public void updateFile(byte[] fileBytes, int fileNumb) throws IOException{
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("Machine"+(nodeNumber+1)+"/file"+fileNumb+".txt"));
		bos.write(fileBytes);
		bos.close();
	}

	public void releaseLocks(){
		for(FileDetails fd : listOfFDs){
			sendReleaseLocks(fd.nodeNo,fd.fileNumber);
		}
		listOfFDs.clear();
		gotReplies=false;
		count=0;
	}
	
	public void sendReply(FileDetails fd, boolean renderLock, int destination){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("REP",nodeNumber,fd,renderLock,-1,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}		
	}

	public void sendWriteReply(FileDetails fd, boolean renderLock, int destination,int[][] csVecs){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message repMsg = new Message("WGRANT",nodeNumber,fd,renderLock,-1,csVecs);
			oos.writeObject(repMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}		
	}	
	
	public void sendReadReply(FileDetails fd, boolean renderLock, int destination, int[][] csVecs){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message repMsg = new Message("RGRANT",nodeNumber,fd,renderLock,-1,csVecs);
			oos.writeObject(repMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	public void sendNoLockReply(int destination, int[][] csVecs){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message repMsg = new Message("NOGRANT",nodeNumber,null,false,-1,csVecs);
			oos.writeObject(repMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}		
	}
	
	public void sendWriteRequest(int destination,int fileNumb){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("WREQ",nodeNumber,null,false,fileNumb,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	public void sendReadRequest(int destination,int fileNumb){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("RREQ",nodeNumber,null,false,fileNumb,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	//sent when write is successful
	public void sendWriteLockRelease(FileDetails fd,int destination){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("WRELEASE",nodeNumber,fd,false,fd.fileNumber,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	
	public void sendReadRelease(int destination,int fileNo){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("READRELEASE",nodeNumber,null,false,fileNo,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	//sent when write is NOT successful and when read is successful
	public void sendReleaseLocks(int destination, int fileNumb){
		try
		{
			String address = nodeMap.get(destination);
			String[] ips = address.split(":");
			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = new Socket(ips[0],Integer.parseInt(ips[1]));
			
           // System.out.println("sent request to "+ destination+" from "+ nodeNo);
			ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
			Message reqMsg = new Message("RELEASE",nodeNumber,null,false,fileNumb,null);
			oos.writeObject(reqMsg);
			//writer.close();
			oos.close();
			clientSocket.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}	
	
	public void processWriteReply(Message reply){
		fillCSVectors(reply.csVectors,reply.sourceNode);
		if(reply.lockAcquired){
			synchronized(this){
				if(!listOfFDs.contains(reply.fileDetails))
					listOfFDs.add(reply.fileDetails);
			}
		}
		
		count++;
		if(count == N-1)
			gotReplies = true;
	}
	
	public void processReadReply(Message reply){
		if(reply.lockAcquired){
			synchronized(this){
				if(!listOfFDs.contains(reply.fileDetails))
					listOfFDs.add(reply.fileDetails);
			}
		}else
			//permissions[reply.sourceNode]=false;
		
		count++;
		if(count == N-1)
			gotReplies = true;
	}	
	
	public void processWriteRequest(Message msg) throws IOException{
		if(acquireWriteLock(msg.fileNo)){
			sendWriteReply(new FileDetails(msg.fileNo,files[msg.fileNo].VN,files[msg.fileNo].RU,files[msg.fileNo].DS,formFile(msg.fileNo),nodeNumber),true,msg.sourceNode,nativeCSVectors);
		}else{
			sendNoLockReply(msg.sourceNode,nativeCSVectors);
		}
	}
	
	public void processReadRequest(Message msg) throws IOException{
		if(acquireReadLock(msg.fileNo,msg.sourceNode)){
			sendReadReply(new FileDetails(msg.fileNo,files[msg.fileNo].VN,files[msg.fileNo].RU,files[msg.fileNo].DS,formFile(msg.fileNo),nodeNumber),true,msg.sourceNode,nativeCSVectors);   
		}else{
			sendNoLockReply(msg.sourceNode,nativeCSVectors);
		}
	}
	
	public void processWriteRelease(Message msg) throws IOException{
		byte[] file = msg.fileDetails.byteFile;
		updateFile(file,msg.fileNo);
		this.VN = msg.fileDetails.VN;
		this.RU = msg.fileDetails.RU;
		this.DS = msg.fileDetails.DS;
		files[msg.fileNo] = msg.fileDetails;
		synchronized(this){
		    fileWriteLock[msg.fileNo]=false;
		}
	}
	
	public void processReadRelease(Message msg){
		synchronized(this){
		    listReadRequests[msg.fileNo].remove(Integer.valueOf(msg.sourceNode));
		    if(listReadRequests[msg.fileNo].isEmpty()){		
			    fileReadLock[msg.fileNo] = false;
			}
		}    
	}
	
	public void processReleaseLock(Message msg){
		synchronized(this){
			fileWriteLock[msg.fileNo] = false;
	
		    listReadRequests[msg.fileNo].remove(Integer.valueOf(msg.sourceNode));
		    if(listReadRequests[msg.fileNo].isEmpty()){			
			    fileReadLock[msg.fileNo] = false;
			}
		}	
	}
	

	public synchronized boolean acquireReadLock(int fileToLock, int node){
		if(!fileWriteLock[fileToLock]){
			fileReadLock[fileToLock] = true;
			listReadRequests[fileToLock].add(node);
			return true;
		}
		return false;
	}
	
	public synchronized boolean acquireWriteLock(int fileToLock){
		if(!fileWriteLock[fileToLock] && !fileReadLock[fileToLock]){
			fileWriteLock[fileToLock] = true;
			return true;
		}
		return false;
		
	}
	
	//first column: LVW
	//second column: LVR
	//3rd column: VNMax
	//4th column: csCount
	public void fillCSVectors(int[][] csVecs,int srcNode){
		for(int i=0;i<numberFiles;i++){
			LVWs[i][srcNode]=csVecs[i][0];
			LVRs[i][srcNode]=csVecs[i][1];
		    csCounts[i][srcNode]=csVecs[i][3];
		    //csMa[i]=Math.max(csMa[i], csVecs[i][3]);
		    //VNMa[i]=Math.max(VNMa[i], csVecs[i][2]);
		    nativeCSVectors[i][2]= Math.max(nativeCSVectors[i][2], csVecs[i][2]);
		    nativeCSVectors[i][3]= Math.max(nativeCSVectors[i][3], csVecs[i][2]);
		}
	}
	
	public void checkForMEvalidity(){
		//for write
		for(int i=0;i<numberFiles;i++){
			for(int j=0;j<N;j++){
				int isDer = findInRow(LVWs,i,LVWs[i][j],j);
				if(isDer > -1){
					if(Math.abs(LVWs[i][j] - nativeCSVectors[i][2])==1){
						System.out.println("MUTUAL EXCLUSION VIOLATED b/w nodes "+isDer+" and "+j+" for file "+i);
						print(LVWs,numberFiles,N);
						print(nativeCSVectors,numberFiles,4);
						nativeCSVectors[i][2]=nativeCSVectors[i][2]+1;
					}
				}
			}
		}
		
		//for read
		for(int k=0;k<numberFiles;k++){
			for(int l=0;l<N;l++){
				int isPresent = findInRow(LVRs,k,LVWs[k][l],l);
				if(isPresent > -1){
					if(csCounts[k][l]==csCounts[k][isPresent]){
						if(csCounts[k][l]==nativeCSVectors[k][3]){
						//	System.out.println("MUTUAL EXCLUSION VIOLATED b/w nodes "+isPresent+" and "+l+" for file "+k);
							nativeCSVectors[k][3]=nativeCSVectors[k][3]+1;
						}
					}
				}
			}
		}
		
	}
	
	public int findInRow(int[][] searchArr, int rowNo, int element, int col){
		if(element!=-1){
			for(int i=0;i<N;i++){
				if(i!=col){
					if(searchArr[rowNo][i]==element){
						return i;
					}
				}
			}
		}
		return -1;
	}
	
	public void print(int[][] array, int row, int col){
		System.out.println();
		
		for(int i=0;i<row;i++){
			for(int j=0;j<col;j++){
				System.out.print(array[i][j]+" ");
			}
			System.out.println();
		}
	}
}

