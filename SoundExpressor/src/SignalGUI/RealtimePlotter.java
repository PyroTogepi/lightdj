package SignalGUI;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;


/**
 * Plots data as a function of time, and scrolls. Meant for debugging purposes, and to be
 * helpful with designing new algorithms!
 * @author Steve Levine
 */
public class RealtimePlotter {

	private int screenX;
	private int screenY;
	private int width;
	private int height;
	
	private BufferedImage buffer;
	private Graphics2D outputG2D;
	
	private double maxY;
	
	// Store the last N points for each plot.
	private int N = 200;
	private int numPlots;
	private int numPoints;
	private double[][] y_vals;
	private Color[] colors;
	
	private boolean AUTO_Y_SCALE = true;
	
	public RealtimePlotter(Color[] colors, int screenX, int screenY, int width, int height, double maxY, Graphics2D g2D) {
		this.screenX = screenX;
		this.screenY = screenY;
		this.width = width;
		this.height = height;
		this.outputG2D = g2D;
		
		this.numPlots = colors.length;
		this.maxY = maxY;
		numPoints = 0;
		y_vals = new double[numPlots][N];
		this.colors = colors;
		
		buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
	}
	
	public int getScreenX() {return screenX;}
	public int getScreenY() {return screenY;}
	public int getWidth() {return width;}
	public int getHeight() {return height;}
	
	
	public void update(double[] y) {
		
		if (numPoints < N) {
			// Just add this point to the end - not saturated and don't have to scroll yet!
			for(int p = 0; p < numPlots; p++) {
				y_vals[p][numPoints] = y[p];
			}
			numPoints++;
			
		} else {
			// Scroll all the points over by 1, and then add it
			for(int i = 0; i < N - 1; i++) {
				for(int p = 0; p < numPlots; p++) {
					y_vals[p][i] = y_vals[p][i + 1];
				}
			}
			 
			for(int p = 0; p < numPlots; p++) {
				y_vals[p][N - 1]  = y[p];
			}
		}
		
		if (AUTO_Y_SCALE) {
			for(int i = 0; i < numPlots; i++) {
				if (maxY < y[i]) {
					maxY = y[i];
				}
			}
		}
		
	}
	
	private void drawGraph() {
		
		Graphics2D g2D = (Graphics2D) buffer.getGraphics();
		
		g2D.setColor(Color.BLACK);
		g2D.clearRect(0, 0, width, height);
		
		
		g2D.setColor(Color.WHITE);
		g2D.setBackground(Color.BLACK);
		
		double scaleX = (double) width / N;
		double scaleY = (double) height / maxY;
	
		
		// Draw the X and Y axes
		g2D.drawLine(transformX(1), transformY(1), transformX(width-1), transformY(1));
		g2D.drawLine(transformX(1), transformY(1), transformX(1), transformY(height-1));
	
		for(int p = 0; p < numPlots; p++) {
			double lastX = 1;
			double lastY = scaleY * y_vals[p][0];
			g2D.setColor(colors[p]);
			for(int i = 1; i < numPoints; i++) {
				double x = scaleX * i;
				double y = scaleY * y_vals[p][i];
				g2D.drawLine(transformX(lastX), transformY(lastY), transformX(x), transformY(y));
				lastX = x;
				lastY = y;
			}
		}
		
		
		// Output this buffered graph image!
		outputGraph();
	}
	
	public void render() {
		// Draw the graph!
		drawGraph();
	}
	
	private void outputGraph() {
		outputG2D.drawImage(buffer, screenX, screenY, null);
	}
	
	
	/**
	 * Transform from graph to screen coordinates
	 * @param x
	 * @return
	 */
	private int transformX(double x) {
		return (int) (x);
	}
	
	private int transformY(double y) {
		return (int) (height - y);
	}
	
	public void move(int x, int y, int width, int height) {
		screenX = x;
		screenY = y;
		if (width > 0) {
			this.width = width;
		} else {
			this.width = 1;
		}
		if (height > 0) {
			this.height = height;
		} else {
			this.height = 1;
		}
		
		setSize(width, height);
		
	}
	
	private void setSize(int w, int h) {
		buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
	
	public void setGraphics(Graphics2D g2D) {
		this.outputG2D = g2D;
	}
	
}

