import java.io.Serializable;


public class FileDetails implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6314909665058763902L;
	public int fileNumber;
	public int VN;
	public int RU;
	public int DS;
	public int nodeNo;
	public byte[] byteFile;
	
	
	public FileDetails(int fileNo,int version, int recentlyUpdated, int distingSite, byte[] file, int nodeNo){
		this.VN = version;
		this.RU = recentlyUpdated;
		this.DS = distingSite;
		this.byteFile = file;
		this.nodeNo = nodeNo;
		this.fileNumber = fileNo;
	}
	
	public String toString(){
		String str = "This is file " +fileNumber+" version: "+VN+ " RU "+RU+" from node :"+nodeNo;
		return str;
	}
	
	public boolean equals(Object f){
		FileDetails fd = (FileDetails)f;
		if(fd.nodeNo==this.nodeNo){
			return true;
		}else
			return false;
	}
	
	
	
}
