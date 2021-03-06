package Visualizors;

import java.awt.Color;

import Common.ColorOutput;
import Common.FeatureList;
import LightDJGUI.GenericKnob;

/**
 * A basic visualizer that sets the first light to red corresponding to how much bass there is,
 * and the second light to a color rotator based on the clap level.
 * @author Steve Levine0
 *
 */
public class ColorEnergyWave extends Visualizer {

	// Useful values
	double blankingTheta = 0.0;
	double colorTheta = 0.0;

	
	// GUI elements
	GenericKnob heatKnob;
	GenericKnob blankSpeed;
	GenericKnob colorSpeed;
	GenericKnob blankAmplitude;
	
	@Override
	public String getName() {
		return "Energy Wave";
	}
	
	public ColorEnergyWave(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters

		
		// Request user controls
		heatKnob = new GenericKnob(0.0f, scale(50), "Heat");
		blankSpeed = new GenericKnob(0.1f, scale(50), "Blanking speed");
		colorSpeed = new GenericKnob(0.1f, scale(50), "Color rotate speed");
		blankAmplitude = new GenericKnob(1.0f, scale(50), "Blanking Amplitude");
		requestUserControl(blankAmplitude);
		requestUserControl(colorSpeed);
		requestUserControl(heatKnob);
		requestUserControl(blankSpeed);
		
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
	
		// Retrieve parameters from the knobs
		double omegaColor = 0.01 * colorSpeed.getValue();
		double omegaBlanking = 0.2 * blankSpeed.getValue() + 0.07;
		double blankingAmount = blankAmplitude.getValue();
		double heat = heatKnob.getValue();
		
		// Compute a new set of colorings, and store them.
		colorTheta += omegaColor;
		blankingTheta += omegaBlanking;
		double blank = 0.6 * (1.0 - blankingAmount / 2.0 * (1 + Math.sin(blankingTheta)));
		Color c = Color.getHSBColor((float) colorTheta, (float) (1.0 - heat), (float) blank);
		
		ColorOutput colorOutput = new ColorOutput();
		colorOutput.setAllFrontRGBLEDs(c);
		colorOutput.setAllRearRGBLEDs(c);
		
		// Return the result
		return colorOutput;
	}



}
