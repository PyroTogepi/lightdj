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
public class ColoredBass extends Visualizer {

	// State
	double colorTheta;
	
	// GUI elements
	ColorKnob hueKnob;
	GenericKnob colorSpeed;
	GenericKnob bassBounce;
	
	@Override
	public String getName() {
		return "Bass Colorz";
	}
	
	public ColoredBass(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters

		
		// Request user controls
		hueKnob = new ColorKnob(0.0f, scale(50), "Hue");
		//colorSpeed = new GenericKnob(0.1f, scale(50), "Color rotate speed");
		bassBounce = new GenericKnob(0.7f, scale(50), "Bass Bounce");
		requestUserControl(hueKnob);
		//requestUserControl(colorSpeed);
		requestUserControl(bassBounce);
		
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		
		// Retrieve parameters from the knobs
		float hue = hueKnob.getValue();
		float bounce = bassBounce.getValue();
		
		
		float saturation = 1.0f;
		float brightness = (float) (bounce * bassLevel + (1 - bounce));
		
		Color c = Color.getHSBColor(hue, saturation, brightness);

		
		ColorOutput colorOutput = new ColorOutput();
		colorOutput.setAllFrontRGBLEDs(c);
		colorOutput.setAllRearRGBLEDs(c);
		
		// Return the result
		return colorOutput;
	}



}
