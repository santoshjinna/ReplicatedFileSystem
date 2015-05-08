import java.io.IOException;
import java.util.Random;


public class Testing {

	
	public static void main(String args[]) throws IOException{
		//write to file
		int node = Integer.parseInt(args[0]);
		FileManager fm = new FileManager(node);
		int read = (fm.readoverwrite * fm.operations) / 100;
		int write = fm.operations - read;
		Random rand = new Random();
		while(read >0 || write >0){
			int rwselect = rand.nextInt(2);
			int fileno = rand.nextInt(FileManager.numberFiles);
			int wait = getNext(fm.delay);
			if(read>0 && write>0){
				if(rwselect == 1){
					//System.out.println("writing to file "+fileno);
					String addend=String.valueOf(System.currentTimeMillis()) + "  "+String.valueOf(node)+"\n";
			        fm.write_enter(fileno, addend);
			        write--;
				}else{
					//System.out.println("reading from file "+fileno);
					//System.out.println("The data inside " + fileno + " is :" +fm.read_enter(fileno));
					fm.read_enter(fileno);
					read--;
				}
			}
			else if(write == 0){
				//System.out.println("reading from file "+fileno);
				//System.out.println("The data inside " + fileno + " is :" +fm.read_enter(fileno));
				fm.read_enter(fileno);
				read--;
			}
			else if(read == 0){
				//System.out.println("writing to file "+fileno);
				String addend=String.valueOf(System.currentTimeMillis()) + "  "+String.valueOf(node)+"\n";
		        fm.write_enter(fileno, addend);
		        write--;
			}
			
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}

		
		
		
	}

	public static int getNext(int lambda) {
		Random r = new Random();
		return  (int) ((-lambda)*Math.log(1-r.nextDouble()));
	}
	
}
