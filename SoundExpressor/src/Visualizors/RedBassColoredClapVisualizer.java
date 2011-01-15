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
public class RedBassColoredClapVisualizer extends Visualizer {

	protected ColorGenerator rgbController;
	
	@Override
	public String getName() {
		return "Red Bass, Colored Claps";
	}
	
	public RedBassColoredClapVisualizer(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters
		rgbController = new HueRotator(0.0, 0.373);
		
		// We don't need to request any user controls for this visualization plugin
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		double clapLevel = (Double) featureList.getFeature("CLAP_LEVEL");
		
		// Compute a new set of colorings, and store them.
		ColorOutput colorOutput = new ColorOutput();
		rgbController.step(bassLevel);
		
		// Make the first light red in proprotion to the bass
		colorOutput.rgbLights[1] = new Color((float) bassLevel, 0.0f, 0.0f);
		
		// Make the second color a randomized hue, with brightness determined by the clap level.
		//colorOutput.rgbLights[0] = rgbController.getColor();
		colorOutput.rgbLights[0] = Color.getHSBColor((float) bassLevel / 3, (float) clapLevel, (float) bassLevel);
		
		// Return the result
		return colorOutput;
	}



}