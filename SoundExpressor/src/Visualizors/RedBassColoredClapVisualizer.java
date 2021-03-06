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
public class RedBassColoredClapVisualizer extends Visualizer {

	protected ColorGenerator rgbController;
	protected double lastPulse = 0.0;
	protected int state = 0;
	
	// GUI controls
	GenericKnob knob;
	ColorKnob colorKnob;
	
	@Override
	public String getName() {
		return "Original";
	}
	
	public RedBassColoredClapVisualizer(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters
		rgbController = new HueRotator(0.0, 0.373);
		
		// Request controls for this plugin
		knob = new GenericKnob(0.4f, scale(50), "Label");
		colorKnob = new ColorKnob(0.0f, scale(50), "Color!one!");
		requestUserControl(knob);
		requestUserControl(colorKnob);
		
	}

	@Override
	public ColorOutput visualize(FeatureList featureList) {
		
		// Retreive any necessary parameters from the FeatureList
		double bassLevel = (Double) featureList.getFeature("BASS_LEVEL");
		double clapLevel = (Double) featureList.getFeature("CLAP_LEVEL");
		double pulse = (Double) featureList.getFeature("PULSE");
		
		// Compute a new set of colorings, and store them.
		ColorOutput colorOutput = new ColorOutput();
		rgbController.step(clapLevel);
		
		Color colorBass = RGBGradientLinear.linearGradient(Color.BLACK, colorKnob.getColor(), bassLevel);
		Color colorHighs = rgbController.getColor();
		
		// Detect a transition to a new measure
		if (pulse < lastPulse) {
			state = (state + 1) % 2;
		}
		lastPulse = pulse;
		
		if (state == 0) {
			// Make the first light red in proprotion to the bass
			colorOutput.setAllFrontPanels(colorBass, colorHighs, colorBass, colorHighs);
		} else {
			// Make the second light red in proprotion to the bass
			colorOutput.setAllFrontPanels(colorHighs, colorBass, colorHighs, colorBass);
		}
		
		
		// Set the UV's to the bass level
		colorOutput.setAllUVWhites(bassLevel, 0.0);
		
		// Set the back to the highs
		colorOutput.setAllRearRGBLEDs(colorHighs);
		
		
		// Return the result
		return colorOutput;
	}



}
