package erccPlot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class Cufflinks {

	public String input;
	
	public Cufflinks(String file) {
		// TODO Auto-generated constructor stub
		this.input = file;
	}

	public HashMap<String,Double[]> parseCufflinks() {
		HashMap<String,Double[]> hash = new HashMap<String,Double[]>();
		// open the file
		try {
			File inFile = new File(this.input);
			BufferedReader in = new BufferedReader(new FileReader(inFile));
			String line;
			while ((line = in.readLine()) != null) {
				// grab the fpkm values for each ERCC 
				// ERCC-00054      -       -       ERCC-00054      -       -       ERCC-00054:0-274        -       -       22.6937 18.0123 27.375  OK
				 if (line.startsWith("ERCC-") ){
					String[] values = line.split("\t");
					// we want the FPKM and upper and lower bounds confidence
					// keyed on the ERCC id
					Double[] FPKM = new Double[3];
					FPKM[0] = Double.parseDouble(values[9]); // FPKM
					FPKM[1] = Double.parseDouble(values[10]); //FPKM low conf
					FPKM[2] = Double.parseDouble(values[11]); // FPKM high conf
					hash.put(values[0], FPKM);
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
