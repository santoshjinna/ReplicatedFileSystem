import java.io.Serializable;


public class Message implements Serializable{
 
	/**
	 * 
	 */
	private static final long serialVersionUID = -5017977211012691002L;
	public String messageType;
	public int sourceNode;
	public FileDetails fileDetails;
	public boolean lockAcquired;
	public int fileNo;

	
	public Message(){
		messageType = "dummy";
	}
	public Message(String msgType,  int srcNode, FileDetails fd, boolean locked, int fileNum){
		this.messageType = msgType;
		this.sourceNode = srcNode;
		this.fileDetails = fd;
		this.lockAcquired = locked;
		this.fileNo = fileNum;
	}
	
	
}
