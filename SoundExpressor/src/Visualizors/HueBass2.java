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
public class HueBass2 extends Visualizer {

	protected static double phaseOffsetBase = 0.04; // 0.012
	protected static double deltaOmega = -0.001;
	protected static double theta;
	
	protected double bassSmoothed = 0.0;
	
	protected static int NUM_FRONT_RGB_LIGHTS = ColorOutput.NUM_FRONT_RGB_PANELS * ColorOutput.NUM_LEDS_PER_RGB_BOARD;
	
	@Override
	public String getName() {
		return "Hue Bass 2";
	}
	
	public HueBass2(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters

		
		// We don't need to request any user controls for this visualization plugin
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		double clapLevel = (Double) featureList.getFeature("SHARP_LEVEL");
		//System.out.println(bassLevel);
		
		// Compute a new set of colorings, and store them.
		
		ColorOutput colorOutput = new ColorOutput();
		
		double phaseOffset = phaseOffsetBase;// * (1 - bassLevel);
		float brightness = 1.0f; //(float) (1 - bassLevel); //1.0f; //(float) (0.75 + 0.25 * bassLevel);
		float saturation = 1.0f; //(float) (1.0 - 0.5 * bassLevel);
		
		// Make the first light red in proprotion to the bass
		Color c0, c1, c2, c3;
//		colorOutput.rgbLights[0] = Color.getHSBColor((float) (theta - 1.5 * phaseOffset), saturation, brightness);
//		colorOutput.rgbLights[1] = Color.getHSBColor((float) (theta - 0.5 * phaseOffset), saturation, brightness);
//		colorOutput.rgbLights[2] = Color.getHSBColor((float) (theta + 0.5 * phaseOffset), saturation, brightness);
//		colorOutput.rgbLights[3] = Color.getHSBColor((float) (theta + 1.5 * phaseOffset), saturation, brightness);
//		c0 = Color.getHSBColor((float) (theta - 1.5 * phaseOffset), saturation, brightness);
//		c1 = Color.getHSBColor((float) (theta - 0.5 * phaseOffset), saturation, brightness);
//		c2 = Color.getHSBColor((float) (theta + 0.5 * phaseOffset), saturation, brightness);
//		c3 = Color.getHSBColor((float) (theta + 1.5 * phaseOffset), saturation, brightness);
//		
//		colorOutput.setAllFrontPanels(c0, c1, c2, c3);
		
		double alpha = 0.08;
		//bassSmoothed = alpha * bassLevel + (1 - alpha) * bassSmoothed;
		bassSmoothed = bassLevel;
		
		double beta = 0.3;
		
		for(int light = 0; light < NUM_FRONT_RGB_LIGHTS; light++) {
			if (light % ColorOutput.NUM_LEDS_PER_RGB_BOARD < ColorOutput.NUM_LEDS_PER_RGB_BOARD / 2) {
				brightness = (float) (beta + (1 - beta) * bassSmoothed);
			} else {
				//brightness = (float) (beta + (1 - beta) * clapLevel);
				brightness = (float) (beta + (1 - beta) * bassSmoothed);
			}
			//brightness = (float) (beta + (1 - beta) * bassSmoothed);
			colorOutput.setFrontRGBLight(light, Color.getHSBColor((float) (theta + phaseOffsetBase * light), saturation, brightness));
		}
		
		theta += deltaOmega;
		if (theta > 1.0) {theta--;}
		if (theta < 0.0) {theta++;}
		
		// Set the UV's to the bass
		colorOutput.setAllUVWhites(bassLevel, 0.0);
		
		// Return the result
		return colorOutput;
	}

	@Override
	public boolean canAutoDJ() {
		return false;
	}


}
