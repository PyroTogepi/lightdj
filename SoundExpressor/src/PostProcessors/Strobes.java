package PostProcessors;

import Common.ColorOutput;
import Common.FeatureList;
import LightDJGUI.GenericKnob;

/**
 * A post processor that takes care of white strobes, uv strobes, and strobe scattering effects.
 * @author steve
 *
 */
public class Strobes extends PostProcessor {

	protected GenericKnob lengthKnob;
	protected boolean active;
	protected int strobeFrame;
	protected int strobeFrameLength;
	
	// Scatter strobes information
	double triggerValLast;
	protected long[] scatterStrobes = new long[ColorOutput.NUM_UVWHITE_PANELS];
	protected long scatterTimeLength = 150; // milliseconds
	protected int lastScatterStrobe = -1;
	protected long lastScatterTime = 0;
	
	public Strobes(double updatesPerSecond) {
		super(updatesPerSecond);
		triggerValLast = 0.0;
	}

	@Override
	public String getName() {
		return "Strobes";
	}

	@Override
	public void init() {
		
		strobeFrame = 0;
		strobeFrameLength = 8;
		
	}

	@Override
	public void postProcess(ColorOutput colorOutput, FeatureList featureList) {
		// See if a scatter strobe is being triggered
		long now = System.currentTimeMillis();
		double triggerVal = ((Double) featureList.getFeature("KEY_[")) + ((Double) featureList.getFeature("KEY_]"));
		if (triggerVal > triggerValLast) {
			// Trigger a random scatter strobe!
			int strobe = (int) (ColorOutput.NUM_UVWHITE_PANELS * Math.random());
			while (strobe == lastScatterStrobe) {
				strobe = (int) (ColorOutput.NUM_UVWHITE_PANELS * Math.random());
			}
			lastScatterStrobe = strobe;
			
			// Turn on that strobe at full power
			scatterStrobes[strobe] = now;
			lastScatterTime = now;
		}
		triggerValLast = triggerVal;
		
		// Check and see if any scattering strobes are currently active
		boolean scattering = (now - lastScatterTime < scatterTimeLength);
		
		if (scattering) {
			for(int strobe = 0; strobe < ColorOutput.NUM_UVWHITE_PANELS; strobe++) {
				double uv = colorOutput.uvLights[strobe];	// Preserve the UV value - just modify the strobe
				double delta = (now - scatterStrobes[strobe]);
				double white;
				
				if (delta > scatterTimeLength) {
					white = 0.0;
				} else {
					white = 1.0 - delta / scatterTimeLength;
					scattering = true;
				}
				
				// Set the output of this strobe accordingly
				colorOutput.setUVWhitePanel(strobe, uv, white);
				
			}
		}
		
	
		// Also see is a normal strobe is being triggered!
		boolean normalStrobe = (((Double) featureList.getFeature("KEY_F12")) == 1.0);
		
		// Process normal strobing, if that's what we're doing
		if (normalStrobe) {
			if (strobeFrame == 0) {
				colorOutput.setWhiteStrobe();
			} else {
				colorOutput.allOff();
			}
			strobeFrame = (strobeFrame + 1) % strobeFrameLength;
		}
		
		// Set active-ness
		if (normalStrobe || scattering) {
			active = true;
		} else {
			active = false;
			strobeFrame = 0;
		}

	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return active;
	}


}
