package LightDJGUI;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import Common.UserControl;

/**
 * This class, although implementing UserControl, is actually non-interactive.
 * It displays an indicator light that can either look like it's glowing, or be off.
 * @author steve
 *
 */
public class IndicatorLight implements UserControl {

	protected boolean onoff;
	protected boolean renderNeeded;
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	
	protected BufferedImage onImage;
	protected BufferedImage offImage;
	
	public IndicatorLight() {
		// Set state variables
		onoff = false;
		renderNeeded = true;
		
		// Load images
		try {
			onImage = ImageIO.read(new File("Images/led-bright.png"));
			offImage = ImageIO.read(new File("Images/led-dark.png"));
		} catch (IOException e) {
			System.out.println("Warning: Could not load indicator light images!");
			e.printStackTrace();
			return;
		}

	}
	
	@Override
	public boolean needsToRender() {
		return renderNeeded;
	}

	@Override
	public void render(Graphics2D g2D) {

		if (onoff) {
			g2D.drawImage(onImage, x, y, null);
		} else {
			g2D.drawImage(offImage, x, y, null);
		}
		
		renderNeeded = false;
	}

	@Override
	public void setLocation(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;	
	}

	@Override
	public void setValue(float val) {
		boolean newVal;
		if (val == 0.0f) {
			newVal = false;
		} else {
			newVal = true;
		}
		
		if (newVal != onoff) {
			renderNeeded = true;
		}
		
		onoff = newVal;
	}

	@Override
	public int getHeight() {return height;}

	@Override
	public int getWidth() {return width;}

	@Override
	public int getX() {return x;}

	@Override
	public int getY() {return y;}

	@Override
	public boolean isVisible() {return true;}

	@Override
	public void mouseDown(int x, int y) {}

	@Override
	public void mouseDragged(int x, int y) {}

	@Override
	public void mouseUp(int x, int y) {}

}