package Visualizors;

import java.awt.Color;

import Common.ColorOutput;
import Common.FeatureList;

/**
 * A basic visualizer that sets the first light to red corresponding to how much bass there is,
 * and the second light to a color rotator based on the clap level.
 * @author Steve Levine0
 *
 */
public class WhiteBlackSlider extends Visualizer {

	protected static double phaseOffsetBase = 0.06;
	protected static double deltaOmega = -0.001;
	protected static double theta;
	protected static double NUM_FRONT_RGB_LIGHTS = ColorOutput.NUM_FRONT_RGB_PANELS * ColorOutput.NUM_LEDS_PER_RGB_BOARD;
	
	protected static RGBGradientCompoundLinear gradient;
	
	@Override
	public String getName() {
		return "White/Black Slider";
	}
	
	public WhiteBlackSlider(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters
		gradient = new RGBGradientCompoundLinear(new Color[]{Color.BLACK, Color.WHITE, Color.BLACK}, new double[]{0.0, 0.5, 1.0});
		
		// We don't need to request any user controls for this visualization plugin
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		double clapLevel = (Double) featureList.getFeature("CLAP_LEVEL");
		//System.out.println(bassLevel);
		
		// Compute a new set of colorings, and store them.
		
		ColorOutput colorOutput = new ColorOutput();

		
		double symLight = -(double) NUM_FRONT_RGB_LIGHTS / 2 - 0.5;
		double a = 1.0;
		
		for(int light = 0; light < NUM_FRONT_RGB_LIGHTS; light++) {
			double x = ((theta + phaseOffsetBase*symLight*(1 - bassLevel*Math.exp(-0.00005*symLight*symLight*symLight*symLight)))) % 1.0;
			if (x < 0) {
				x += ((int) x) + 1;
			}
			
			colorOutput.setFrontRGBLight(light, gradient.computeGradient(x));
			symLight++;
		}
		
		theta += deltaOmega;
		if (theta > 1.0) {theta--;}
		if (theta < 0.0) {theta++;}
		
		// Set the UV's to the bass level
		colorOutput.setAllUVWhites(bassLevel, 0.0);
		
		// Return the result
		return colorOutput;
	}



}
