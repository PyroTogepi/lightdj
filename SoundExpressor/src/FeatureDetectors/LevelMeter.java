package FeatureDetectors;

import Common.FeatureList;

/**
 * Measures the overall level of the music, like a VU meter. Gently lowpasses the output.
 * @author Steve Levine
 *
 */
public class LevelMeter extends FeatureDetector {
	
	protected double averageHalfLife;
	
	protected double phi;
	protected double decayRate;
	protected double normalizingVal;
	protected double averagedLevel;
	
	// Low-pass smoothing
	protected double timeLowPass = 0.15;
	protected double percentLowPass = 0.05;
	protected double alpha;
	protected double levelSmoothed = 0.0;
	
	public LevelMeter(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initiate other parameters
		normalizingVal = 0.01;
		averageHalfLife = 0.0025;
		decayRate = 1.0 / (20);
		
		alpha = 1 - Math.exp(Math.log(percentLowPass) / (UPDATES_PER_SECOND * timeLowPass));
		
		// Calculate some parameters
		phi = Math.pow(0.5, 1/(averageHalfLife * UPDATES_PER_SECOND));
	}
	
	@Override
	public void computeFeatures(double[] frequencies, double[] magnitudes, FeatureList featureList) {
		// Compute the level of bass
		double level = getLevel(frequencies, magnitudes);
		
		levelSmoothed = alpha * level + (1 - alpha) * levelSmoothed;
		
		// Create a feature of this, and add it to the featureList.
		featureList.addFeature("OVERALL_LEVEL", levelSmoothed);
	}
	

	
	public double getLevel(double[] frequencies, double[] magnitudes) {
		double sum = 0;
		int n = 0;
		for(int i = 1; i < frequencies.length; i++) {
			//sum += Math.log(1 + magnitudes[i]);
			
			sum += magnitudes[i]*magnitudes[i];
			n++;
		}
		
		double level = sum / n;
		normalizingVal = 60.0;
		phi = 0.8;
		averagedLevel = averagedLevel * phi + level*(1 - phi);
		
		double out = averagedLevel / normalizingVal;
		
		// Limit to 0.0 to 1.0
		if (out < 0.0) {
			return 0.0;
		} else if (out > 1.0) {
			return 1.0;
		} else {
			return out;
		}
		
	}
	
}
