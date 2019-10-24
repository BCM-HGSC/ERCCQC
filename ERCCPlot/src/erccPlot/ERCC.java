package erccPlot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

public class ERCC {

	public String input;
	
	public ERCC(String file) {
		this.input = file;	
	}

	public HashMap<String,Double> parseERCC() {
		HashMap<String,Double> hash = new HashMap<String,Double>();
		// open the file
		try {
			File inFile = new File(this.input);
			BufferedReader in = new BufferedReader(new FileReader(inFile));
			String line;
			while ((line = in.readLine()) != null) {
				// grab the concentrations values for each ERCC 
				// 
				 if (line.startsWith("ERCC-") ){
					String[] values = line.split("\t");
					// we want the concentration of the Spikein keyed on the ERCC id
					hash.put(values[0], Double.parseDouble(values[1]));
				}
			}
		} catch(java.io.FileNotFoundException e){
			System.err.println("Cannot open file " + this.input);
		} catch(java.io.IOException e ) {
			System.err.println("Cannot parse file "+ this.input);
		}
		return hash;
	}
	
}
