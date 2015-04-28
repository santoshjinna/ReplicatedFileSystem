import java.io.Serializable;


public class Message implements Serializable{
 
	public String messageType;
	public int sourceNode;
	public FileDetails fileDetails;
	public boolean lockAcquired;

	
	public Message(){
		messageType = "dummy";
	}
	public Message(String msgType,  int srcNode, FileDetails fd, boolean locked){
		this.messageType = msgType;
		this.sourceNode = srcNode;
		this.fileDetails = fd;
		this.lockAcquired = locked;
	}
	
	
}