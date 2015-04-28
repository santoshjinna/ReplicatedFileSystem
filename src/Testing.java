import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class Testing {

	
	public static void main(String args[]) throws IOException{
		//write to file
		int node = Integer.parseInt(args[0]);
		FileManager fm = new FileManager(node);
		
		String addend=String.valueOf(System.currentTimeMillis()) + "  "+String.valueOf(node)+"\n";
        fm.write_enter(1, addend);
	}
}
