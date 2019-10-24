package erccPlot;


import java.util.HashMap;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;


public class Plot extends JFrame {

	private static final long serialVersionUID = 1L;
    private XYSeriesCollection dataset;

	public Plot(HashMap<String,Double[]> fpkm,HashMap<String,Double> conc) {
		super("ERCC vs FPKM");
		// This will create the dataset 
		createDataset(fpkm ,conc);
	}


	/**
	 * Creates a sample dataset 
	 */

	private  void createDataset(HashMap<String,Double[]> fpkm, HashMap<String,Double> conc) {
		XYSeriesCollection coll = new XYSeriesCollection();
		XYSeries data = new XYSeries("ERCC");
		for (String id : conc.keySet()){
			// check it is in both datasets
			if (!fpkm.containsKey(id)){
				System.err.println("ID " + id + " not found in cufflinks output");
				continue;
			}
			if (fpkm.get(id)[0] == 0.0){
				System.err.println("ID " + id + " is zero");
				continue;
			}
			System.out.print("ID " + id);
			Double[] FPKMs= fpkm.get(id);
			Double c = conc.get(id);
			System.out.print(" " + c);
			for(Double FPKM : FPKMs){
				System.out.print(" " + FPKM );
			}
			System.out.println();
			Double X = Math.log(FPKMs[0])/Math.log(2);
			Double Y = Math.log(c)/Math.log(2);
			data.add(X,Y);
			
			System.out.println(" " + X + " - " +Y);
		}	
		dataset.addSeries(data);
//		coll.addSeries(data);

//		XYSeriesCollection regression = regress(coll);
		//dataset.addSeries(regression);
		showGraph();
	}

	private static XYSeriesCollection regress(XYSeriesCollection data) {
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
			double sx = 0, sy = 0, sxx = 0, sxy = 0, syy = 0;
			for (int j = 0; j < n; j++) {
				double x = ser.getX(j).doubleValue();
				double y = ser.getY(j).doubleValue();
				sx += x;
				sy += y;
				sxx += x * x;
				sxy += x * y;
				syy += y * y;
			}
			double b = (n * sxy - sx * sy) / (n * sxx - sx * sx);
			double a = sy / n - b * sx / n;
			XYSeries regr = new XYSeries(ser.getKey());
			regr.add(xMin, a + b * xMin);
			regr.add(xMax, a + b * xMax);
			coll.addSeries(regr);
		}
		return coll;
	}
	
	
	private void showGraph() {
		final JFreeChart chart = createChart(dataset);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(700, 500));
		final ApplicationFrame frame = new ApplicationFrame("ERCC");
		frame.setContentPane(chartPanel);
		frame.pack();
		frame.setVisible(true);
	}

	private JFreeChart createChart(final XYDataset dataset) {
		final JFreeChart chart = ChartFactory.createScatterPlot(
				"log2 ERCC concentration vs log2 FPKM",                  // chart title
				"log2 ERCC concentration",                      // x axis label
				"log2 FPKM",                      // y axis label
				dataset,                  // data
				PlotOrientation.VERTICAL,
				true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);
		XYPlot plot = (XYPlot) chart.getPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, false);
		plot.setRenderer(renderer);
		return chart;
	}
} 