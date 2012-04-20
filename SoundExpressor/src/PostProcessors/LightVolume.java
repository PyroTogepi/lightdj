package PostProcessors;

import java.awt.Color;

import Common.ColorOutput;
import Common.FeatureList;
import LightDJGUI.GenericKnob;
import PartyLightsController.PartyLightsController16.LightPlacement;
import Visualizors.RGBGradientLinear;

/**
 * This post processor adjusts the overall "light volume" of our lights.
 * @author steve
 *
 */
public class LightVolume extends PostProcessor {

	protected GenericKnob volumeKnob;
	
	
	public LightVolume(double updatesPerSecond) {
		super(updatesPerSecond);
		
	}

	@Override
	public String getName() {
		return "Light Volume";
	}

	@Override
	public void init() {
		// Make a user control
		volumeKnob = new GenericKnob(1.0f, 40, "Volume");
		requestUserControl(volumeKnob);
		
	}

	@Override
	public void postProcess(ColorOutput colorOutput, FeatureList featureList) {
		float volume = volumeKnob.getValue();
		
		// Scale all of the front panels
		for(int i = 0; i < ColorOutput.NUM_RGB_LIGHTS_FRONT; i++) {
			Color c = colorOutput.rgbLightsFront[i];
			float[] rgb = new float[3];
			c.getRGBColorComponents(rgb);
			for(int j = 0; j < 3; j++) {
				rgb[j] *= volume;
			}
			c = new Color(rgb[0], rgb[1], rgb[2]);
			colorOutput.rgbLightsFront[i] = c;
		}
		
	}

	@Override
	public boolean isActive() {
		return true;
	}


}
