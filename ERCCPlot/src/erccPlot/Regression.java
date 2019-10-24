package erccPlot;

import java.awt.Paint;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.chart.ChartUtilities;

@SuppressWarnings("serial")

public class Regression {

	public static BufferedWriter stats ;
	public static BufferedWriter html ;
	public static BufferedWriter graph ;
	public static String outDir;
	
	public Regression(HashMap<String,Double[]> fpkm,HashMap<String,Double> conc,String outputDir) {
		this.outDir = outputDir;
		// open output file
		try {
			File m = new File(outputDir + "metrics.tsv");
			File h = new File(outputDir + "index.html");
			// if file doesn't exists, then create it
			if (!m.exists()) {
				m.createNewFile();
			}
			// if file doesnt exists, then create it
			if (!h.exists()) {
				h.createNewFile();
			}
			FileWriter fw1 = new FileWriter(m.getAbsoluteFile());
			stats = new BufferedWriter(fw1);
			FileWriter fw2 = new FileWriter(h.getAbsoluteFile());
			html = new BufferedWriter(fw2);

		} catch (IOException e) {
			e.printStackTrace();
		}
		XYSeriesCollection data = getData(fpkm,conc);
		JFreeChart chart = createChart(data);
		// save it to file
		saveChart(chart);
	}

	private  JFreeChart createChart(XYSeriesCollection data) {
		JFreeChart chart = ChartFactory.createScatterPlot("", "log2 ERCC concentration", "log2 FPKM", data,
				PlotOrientation.VERTICAL, true, false, false);
		XYPlot plot = (XYPlot) chart.getPlot();
		XYItemRenderer scatterRenderer = plot.getRenderer();
		StandardXYItemRenderer regressionRenderer = new StandardXYItemRenderer();
		regressionRenderer.setBaseSeriesVisibleInLegend(false);
		plot.setDataset(1, regress(data));
		plot.setRenderer(1, regressionRenderer);
		DrawingSupplier ds = plot.getDrawingSupplier();
		for (int i = 0; i < data.getSeriesCount(); i++) {
			Paint paint = ds.getNextPaint();
			scatterRenderer.setSeriesPaint(i, paint);
			regressionRenderer.setSeriesPaint(i, paint);
		}
		return chart;
	}

	private  XYDataset regress(XYSeriesCollection data) {
		// Determine bounds
		double xMin = Double.MAX_VALUE, xMax = 0;
		for (int i = 0; i < data.getSeriesCount(); i++) {
			XYSeries ser = data.getSeries(i);
			for (int j = 0; j < ser.getItemCount(); j++) {
				double x = ser.getX(j).doubleValue();
				if (x < xMin) {
					xMin = x;
				}
				if (x > xMax) {
					xMax = x;
				}
			}
		}
		// Create 2-point series for each of the original series
		XYSeriesCollection coll = new XYSeriesCollection();
		for (int i = 0; i < data.getSeriesCount(); i++) {
			XYSeries ser = data.getSeries(i);
			int n = ser.getItemCount();
			if ( n < 5 ) {
				String message = "Identifed " + n + " ERCC transcripts, not enough to make plot";
				System.err.println(message);
				try {
					stats.write(message);
					html.write(message);
					stats.close();
					html.close();
				} catch ( IOException e ) {
					e.printStackTrace();
				}
				// return success
				System.exit(0);
			}
			double sx = 0, sy = 0, sxx = 0, sxy = 0, syy = 0;
	        double[] x = new double[1000];
	        double[] y = new double[1000];
			for (int j = 0; j < n; j++) {
				double xp = ser.getX(j).doubleValue();
				double yp = ser.getY(j).doubleValue();
	            x[j] = xp;
	            y[j] = yp;
				sx += xp;
				sy += yp;
				sxx += xp * xp;
				sxy += xp * yp;
				syy += yp * yp;
			}
			double b = (n * sxy - sx * sy) / (n * sxx - sx * sx);
			double a = sy / n - b * sx / n;
			XYSeries regr = new XYSeries(ser.getKey());
			regr.add(xMin, a + b * xMin);
			regr.add(xMax, a + b * xMax);
			coll.addSeries(regr);
			// whats the slope of the line?
			Double slope =  (( a + b * xMax) - (a + b * xMin)) / (xMax - xMin);
			
	        double xbar = sx / n;
	        double ybar = sy / n;
			   // second pass: compute summary statistics
	        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
	        for (int j = 0; j < n; j++) {
	            xxbar += (ser.getX(j).doubleValue() - xbar) * (ser.getX(j).doubleValue() - xbar);
	            yybar += (ser.getY(j).doubleValue() - ybar) * (ser.getY(j).doubleValue() - ybar);
	            xybar += (ser.getX(j).doubleValue() - xbar) * (ser.getY(j).doubleValue() - ybar);
	        }
	        double beta1 = xybar / xxbar;
	        double beta0 = ybar - beta1 * xbar;


	        // analyze results
	        int df = n - 2;
	        double rss = 0.0;      // residual sum of squares
	        double ssr = 0.0;      // regression sum of squares
	        for (int j = 0; j < n; j++) {
	            double fit = beta1*x[j] + beta0;
	            rss += (fit - y[j]) * (fit - y[j]);
	            ssr += (fit - ybar) * (fit - ybar);
	        }
	        double R2    = ssr / yybar;
	        double svar  = rss / df;
	        double svar1 = svar / xxbar;
	        double svar0 = svar/n + xbar*xbar*svar1;
	        
	        String results = new String();
	        results = "R^2\tCorrelation\tnumber of ERCC transcripts identified\n";
	        results += String.format("%.3g",R2) + "\t" + String.format("%.3g",Math.sqrt(R2)) + "\t" + n + "\n";
	        // write metrics file and html
	        String webpage = makeHtml(R2,Math.sqrt(R2),n);
	        
			try {
		        stats.write(results);
		        stats.close();
		        html.write(webpage);
		        html.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// write html

		}
		return coll;
	}

	private static XYSeriesCollection getData(HashMap<String,Double[]> fpkm,HashMap<String,Double> conc) {

		XYSeriesCollection data = new XYSeriesCollection();
		XYSeries series = new XYSeries("ERCC");
		for (String id : conc.keySet()){
			// check it is in both datasets
			if (!fpkm.containsKey(id)){
				continue;
			}
			if (fpkm.get(id)[0] == 0.0){
				continue;
			}

			Double[] FPKMs= fpkm.get(id);
			Double c = conc.get(id);
			Double Y = Math.log(FPKMs[0])/Math.log(2);
			Double X = Math.log(c)/Math.log(2);
			series.add(X,Y);
		}	
		data.addSeries(series);
		return data;
	}

    public void saveChart(JFreeChart chart)
    {
        String fileName= this.outDir + "ERCC.jpg";
        try {
            ChartUtilities.saveChartAsJPEG(new File(fileName), chart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Problem occurred creating chart.");
        }
    }
    public String makeHtml(Double R, Double C, int n) {
    	String h = new String();
    	Date date = new Date();
    	// add header info
    	h = "<html>\n"+ 
    			"<head><title>ERCC-seq Metrics</title>\n"+ 
    			"<style type='text/css'>\n"+ 
    			"table {\n"+ 
    			"border-width: 1px;\n"+ 
    			"border-spacing: 2px;\n"+ 
    			"border-style: outset;\n"+ 
    			"border-color: gray;\n"+ 
    			"border-collapse: collapse;\n"+ 
    			"background-color: rgb(209, 255, 255);\n"+ 
    			"}\n"+ 
    			"table th {\n"+ 
    			"border-width: 2px;\n"+ 
    			"padding: 2px;\n"+ 
    			"border-style: solid;\n"+ 
    			"border-color: gray;\n"+ 
    			"background-color: rgb(209, 255, 255);\n"+ 
    			"}\n"+ 
    			"table td {\n"+ 
    			"border-width: 2px;\n"+ 
    			"padding: 2px;\n"+ 
    			"border-style: solid;\n"+ 
    			"border-color: gray;\n"+ 
    			"background-color: white;\n"+ 
    			"}\n"+ 
    			"</style>\n"+ 
    			"</head>\n"+ 
    			"<body>\n"+ 
    			"<h1>ERCC Metrics</h1>\n"+ 
    			"<h2>Comparison of ERCC concentrations with Cufflinks FPKM</h1>\n" +
    			"<table><tr><th>Correlation</th><th>R<SUP>2</SUP></th><th>Number of ERCC transcripts identified</th></tr>\n" + 
    			"<tr><td align='center'>" + String.format("%.3g",C) + "</td><td align='center'>" + String.format("%.3g",R)  + "</td><td align='center'>" + n + "</td></tr></table>\n" +
    			"<HR WIDTH='100%' COLOR='#9090900' SIZE='3'>\n" +
    			"<h3>ERCC concentrations vs FPKM from Cufflinks</h3>\n" +
    			"<img src= 'ERCC.jpg'></p>\n" +
    			"<h2>Files</h2>\n"+
    			"<table><tr><th>File</th><th>Description</th></tr>\n" +
    			"<tr><td><a target='_new' href='metrics.tsv'>Metrics Tab Separated Value File</a></td><td>Text file containing all the metrics of the report in a single tab delimited file.</td></tr>\n" +
    			"</table></p><h5>Run on " + date.toString() + "</h5></body></html>";
    	return h;
    }

}



