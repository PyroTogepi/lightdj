package Visualizors;

import java.awt.Color;

/**
 * Represents a cyclic linear gradient between n different colors! The last color is the first color. Accepts an argument in the rage 0.0 - 1.0.
 * @author steve
 *
 */
public class RGBGradientCompoundLinear {
	
	
	protected Color[] c;
	protected double[] startPoints;
	
	public RGBGradientCompoundLinear(Color[] c, double[] startPoints) {
		this.c = c;
		this.startPoints = startPoints;
	}
	
	/**
	 * Compute the gradient.
	 */
	public Color computeGradient(double x) {
		
		for(int i = 0; i < c.length - 1; i++) {
			if (x >= startPoints[i] && x <= startPoints[i + 1]) {
				return RGBGradientLinear.linearGradient(c[i], c[i + 1], (x - startPoints[i]) / (startPoints[i + 1] - startPoints[i]));
			}
		}
		
		// If we get here, it didn't work!
		return Color.BLACK;
		
	}
	
}
