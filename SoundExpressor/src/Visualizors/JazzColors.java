package Visualizors;

import java.awt.Color;

import Common.ColorOutput;
import Common.FeatureList;
import LightDJGUI.ColorKnob;
import LightDJGUI.GenericKnob;

/**
 * A basic visualizer that sets the first light to red corresponding to how much bass there is,
 * and the second light to a color rotator based on the clap level.
 * @author Steve Levine0
 *
 */
public class JazzColors extends Visualizer {

	// State
	double colorTheta;
	
	// Jazz color gradient
	RGBGradientCompoundLinear gradient;
	
	// GUI elements
	GenericKnob colorSpeed;
	GenericKnob bassBounce;
	
	@Override
	public String getName() {
		return "Jazz";
	}
	
	public JazzColors(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters

		
		// Request user controls
		colorSpeed = new GenericKnob(0.05f, scale(50), "Color rotate speed");
		bassBounce = new GenericKnob(0.7f, scale(50), "Bass Bounce");
		requestUserControl(colorSpeed);
		requestUserControl(bassBounce);
		
		// Set up a nice jazzy gradient!
		Color purple = new Color(255, 0, 128);
		Color nightBlue = new Color(0, 14, 122);
		gradient = new RGBGradientCompoundLinear(new Color[]{Color.BLUE, purple, nightBlue, Color.BLACK, Color.BLUE}, new double[]{0.0, 0.25, 0.5, 0.75, 1.0});
		
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		
		// Retrieve parameters from the knobs
		float omega = colorSpeed.getValue();
		float bounce = bassBounce.getValue();
		
		colorTheta = (colorTheta + 0.01 * omega) % 1.0f;
		double lightPhaseShiftCoeff = 0.08;
		
		float saturation = 1.0f;
		float brightness = (float) (bounce * bassLevel + (1 - bounce));
		
		ColorOutput colorOutput = new ColorOutput();
		for(int light = 0; light < ColorOutput.NUM_FRONT_RGB_PANELS * ColorOutput.NUM_LEDS_PER_RGB_BOARD; light++) {
			
			double theta = (colorTheta + lightPhaseShiftCoeff * light) % 1.0;
			Color c = gradient.computeGradient(theta);
			
			Color lightColor = RGBGradientCompoundLinear.scaleColor(c, brightness);
			
			colorOutput.setFrontRGBLight(light, lightColor);
		}
		
		// Return the result
		return colorOutput;
	}


}
