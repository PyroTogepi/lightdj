package SignalGUI;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


/**
 * Represents a scrolling display used for displaying different channel values
 * @author Steve Levine
 *
 */
public class ChannelLights {
	private int screenX;
	private int screenY;
	private int width;
	private int height;
	
	private BufferedImage buffer;
	private Graphics2D outputG2D;

	int lightSize;
	Color[] colors;
	
	static final int spacing = 10;
	
	public ChannelLights(Color[] colors, int lightSize, int screenX, int screenY, int width, int height, Graphics2D g2D) {
		this.screenX = screenX;
		this.screenY = screenY;
		this.width = width;
		this.height = height;
		outputG2D = g2D;
		
		this.colors = colors;
		this.lightSize = lightSize;
		
		buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
	}
	
	
	// PRECONDITION: The number of channels is equal to the number of colors passed in earlier.
	
	public void updateWithNewChannelVals(double[] channelVals) {
		Graphics2D g2D = (Graphics2D) buffer.getGraphics();
		
				
		for(int i = 0; i < colors.length; i++) {
			// Draw a light of the appropriate color!
			g2D.setColor(scaleColor(colors[i], channelVals[i]));
			g2D.fillRect((lightSize + spacing) * i, 0, lightSize, lightSize);
		}
		
		
		// Output!
		outputGraph();
		
	}
	
	
	// Select a color based on a value between 0 and 1
	private Color scaleColor(Color c, double normalizedVal) {
		if (normalizedVal > 1.0) {
			normalizedVal = 1.0;
		} else if (normalizedVal < 0.0) {
			normalizedVal = 0.0;
		}
		
		return new Color((int) Math.round(normalizedVal * c.getRed()), (int) Math.round(normalizedVal * c.getGreen()), (int) Math.round(normalizedVal * c.getBlue()));		
	}
	
	
	
	private void outputGraph() {
		outputG2D.drawImage(buffer, screenX, screenY, null);
	}
	
	
	
}
