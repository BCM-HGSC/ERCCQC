/**
 * @author simonw
 *
 */

import com.martiansoftware.jsap.*;
import java.util.HashMap;
import java.io.*;
import java.util.Iterator;
import erccPlot.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import org.jfree.ui.RefineryUtilities;

public  class Main {

	public static String version = "13.6.7";
	public static String ErccFile;
	public static String cufflinksFile;
	public static String outputDir;
	private static HashMap<String,Double[]> ERCCData;
	private static HashMap<String,Double> ERCCConc;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// parse the arguments
		parseArgs(args);
		runTask();
	}

	public static void runTask() {
		Cufflinks cuff = new Cufflinks(cufflinksFile);
		ERCCData = cuff.parseCufflinks();
		ERCC conc = new ERCC(ErccFile);
		ERCCConc = conc.parseERCC();
		Regression r = new Regression(ERCCData,ERCCConc,outputDir);
	}

	public static void correlation() {
		for (String id : ERCCData.keySet()){
			System.out.print("ID " + id);
			Double[] FPKMs= ERCCData.get(id);
			for(Double FPKM : FPKMs){
				System.out.print(" " + FPKM);
			}
			System.out.println();
		}
		for (String id : ERCCConc.keySet()){
			System.out.print("ID " + id + " ");
			System.out.println(ERCCConc.get(id));
		}
	}

	public static void parseArgs(String[] args) {
		try {
			JSAP jsap = new JSAP();

			FlaggedOption cuffOpt = new FlaggedOption("cufflinks")
			.setStringParser(JSAP.STRING_PARSER)
			.setRequired(true)
			.setShortFlag('c')
			.setLongFlag("cufflinks");
			cuffOpt.setHelp("Cufflinks genes.fpkm_tracking file");
			jsap.registerParameter(cuffOpt);


			FlaggedOption ERCCOpt = new FlaggedOption("ERCC")
			.setStringParser(JSAP.STRING_PARSER)
			.setRequired(true)
			.setShortFlag('e')
			.setLongFlag("ercc");
			ERCCOpt.setHelp("File containing ERCC concentrations");
			jsap.registerParameter(ERCCOpt);

			FlaggedOption outOpt = new FlaggedOption("output")
			.setStringParser(JSAP.STRING_PARSER)
			.setRequired(true)
			.setShortFlag('o')
			.setLongFlag("output");
			outOpt.setHelp("Directory to write results");
			jsap.registerParameter(outOpt);
			
			JSAPResult params = jsap.parse(args);

			if (!params.success()) {
				for (Iterator errs = params.getErrorMessageIterator(); errs.hasNext();) {
					System.err.println("Error: " + errs.next());
				}
				printUsage(jsap);
				System.exit(5);
			}

			// get the input files
			String ercc = params.getString("ERCC");
			File erccFile = new File(ercc);
			if ( ercc == null ) {
				System.err.println("Error! Input File is Required!");
				System.exit(5);
			} else if (!erccFile.exists()) {
				System.err.println("Error! Input File " + erccFile.getAbsolutePath() + " doesn't exist!");
				System.exit(5);
			} else {
				ErccFile = ercc;
			}
			String cuffIn = params.getString("cufflinks");
			File cuffInFile = new File(cuffIn);
			if ( cuffIn == null ) {
				System.err.println("Error! Input File is Required!");
				System.exit(5);
			} else if (!cuffInFile.exists()) {
				System.err.println("Error! Input File " + cuffInFile.getAbsolutePath() + " doesn't exist!");
				System.exit(5);
			} else {
				cufflinksFile = cuffIn;
			}
			String o = params.getString("output");
			File dir = new File(o);
			if (!dir.exists()){
				System.err.println("Output Directory " + dir.getAbsolutePath() + " Does not exist!");
				printUsage(jsap);
				System.exit(5);
			} else {
				outputDir = dir.getAbsolutePath()+"/";
			}	

		} catch (JSAPException e) {
			System.err.println("Exception during option parsing");
			e.printStackTrace();
			System.exit(5);
		}
	}

	/**
	 * Prints JSAP Usage in a user-friendly format
	 */
	public static void printUsage(JSAP jsap) { 
		System.err.println();
		System.err.print("ERCCPlot calculates correlation between known spike-in concentrations of ERCC \n"
				+ "against FPKM values from cufflinks\n"
				+ "version = " + version  + "\n"
				+ "Usage: java -jar ERCCPLot.jar "
				+ Main.class.getName());
		System.err.println("        "
				+ jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
	}

	public static void wait (int n){
		long t0,t1;
		t0=System.currentTimeMillis();
		do{
			t1=System.currentTimeMillis();
		}
		while (t1-t0<(n * 1000));
	}

}
