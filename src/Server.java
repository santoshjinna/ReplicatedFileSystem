import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Server implements Runnable{
	public FileManager fileManage;
 
	public boolean end = true;

	private ServerSocket serverSock;
	private int port;
	
	public Server(FileManager mang, int port){
		this.fileManage = mang;
		this.port = port;
	}
	
	@Override
	public void run() {
		go();
		
	}
	
	public void go()
	{
		try
		{
			serverSock = new ServerSocket(port);
			//Server goes into a permanent loop accepting connections from clients			
			while(end)
			{
				//Listens for a connection to be made to this socket and accepts it
				//The method blocks until a connection is made
				Socket sock = serverSock.accept();
				//PrintWriter is a bridge between character data and the socket's low-level output stream
//				PrintWriter writer = new PrintWriter(sock.getOutputStream());
//				writer.println(message);
//				writer.close();
				
				//BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
				//String cmd = br.readLine();
				Message msg = new Message();
				try {
					msg = (Message) ois.readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
//				if(msg.messageType.equalsIgnoreCase("REQ")){
//					
//					//String requestedNode = br.readLine();
//					int requestedNode = msg.sourceNode;
//					//System.out.println("received request from "+requestedNode);
//					fileManage.processRequest(msg);
//					
//				}
				if(msg.messageType.equalsIgnoreCase("WREQ")){
					fileManage.processWriteRequest(msg);
					
				}else if(msg.messageType.equalsIgnoreCase("RREQ")){
					fileManage.processReadRequest(msg);
					
				}			
//				else if(msg.messageType.equalsIgnoreCase("REP")){
//					
//					//String repliedNode = br.readLine();
//					int repliedNode = msg.sourceNode;
//                    
//					//System.out.println("received reply from "+repliedNode);
//					fileManage.processReply(msg);
//					
//				} 
				else if(msg.messageType.equalsIgnoreCase("WGRANT")){
					fileManage.processWriteReply(msg);
					
				}else if(msg.messageType.equalsIgnoreCase("RGRANT")){
					fileManage.processWriteReply(msg);
					
				}
				else if(msg.messageType.equalsIgnoreCase("NOGRANT")){
					fileManage.processWriteReply(msg);
					
				} else if(msg.messageType.equalsIgnoreCase("WRELEASE")){
					fileManage.processWriteRelease(msg);
					
				} else if(msg.messageType.equalsIgnoreCase("READRELEASE")){
					fileManage.processReadRelease(msg);
					
				}else if(msg.messageType.equalsIgnoreCase("RELEASE")){
					fileManage.processReleaseLock(msg);
					
				}
			}
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	

}