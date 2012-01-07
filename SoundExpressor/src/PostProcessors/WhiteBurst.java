package PostProcessors;

import java.awt.Color;

import Common.ColorOutput;
import Common.FeatureList;
import LightDJGUI.GenericKnob;
import Visualizors.RGBGradientLinear;

/**
 * This post processor takes care of white bursts and the "emergency lighting" commands.
 * @author steve
 *
 */
public class WhiteBurst extends PostProcessor {

	protected GenericKnob lengthKnob;
	protected boolean active;
	protected long startTime;
	
	
	// White burst
	protected long whiteBurstTime;	// milliseconds
	
	public WhiteBurst(double updatesPerSecond) {
		super(updatesPerSecond);
		
	}

	@Override
	public String getName() {
		return "Burst";
	}

	@Override
	public void init() {
		
		active = false;
		whiteBurstTime = 500;	
		
		// Make a user control
		lengthKnob = new GenericKnob(0.25f, 40, "Length");
		requestUserControl(lengthKnob);
		
	}

	@Override
	public void postProcess(ColorOutput colorOutput, FeatureList featureList) {
		
		
		// Currently not active. Should it became active?
		if (((Double) featureList.getFeature("KEY_ENTER")) == 1.0) {
			// ACTIVATE!
			active = true;
			startTime = System.currentTimeMillis();
		}
		
		
		// Continue a burst that is currently running
		if (active) {
			
			// Get the timing parameters
			long now = System.currentTimeMillis();
			long delta = (now - startTime);
			
			whiteBurstTime = (long) Math.round(lengthKnob.getValue() * 2000);
			
			// See if we're done
			if (delta > whiteBurstTime) {
				active = false;
				return;
			}
			
			// Not done - still fading! Compute how much.
			double alpha = (double) delta / whiteBurstTime;
			
			for(int light = 0; light < ColorOutput.NUM_FRONT_RGB_PANELS*4; light++) {
				colorOutput.rgbLightsFront[light] = RGBGradientLinear.linearGradient(Color.WHITE, colorOutput.rgbLightsFront[light], alpha);
			}

			for(int light = 0; light < ColorOutput.NUM_UVWHITE_PANELS; light++) {
				colorOutput.uvLights[light] = (1 - alpha) * 1.0 + alpha * colorOutput.uvLights[light];
				colorOutput.whiteLights[light] = (1 - alpha) * 1.0 + alpha * colorOutput.whiteLights[light];
			}	
		}
		
	}

	@Override
	public boolean isActive() {
		return active;
	}


}