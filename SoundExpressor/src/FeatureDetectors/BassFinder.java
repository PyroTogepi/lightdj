package FeatureDetectors;

import Common.FeatureList;
import LightDJGUI.FrequencyRangeControl;
import Utils.TimerTicToc;

/**
 * A state-machine like object that, when stepped with FFT values, attempts to output the current bass level.
 * Attempts to auto-adapt to changing volume.
 * @author Steve Levine
 *
 */
public class BassFinder extends FeatureDetector {

	protected double averageHalfLife;
	
	protected double updatesPerSecond;
	protected double phi;
	protected double decayRate;
	protected double normalizingVal;
	protected double minFreq;
	protected double maxFreq;
	protected double lastOutput;
	
	// Low-pass smoothing
	protected double timeLowPass = 0.2;
	protected double percentLowPass = 0.05;
	protected double alpha;
	protected double bassLevelSmoothed = 0.0;
	
	protected double averagedLevel;
	protected double currentBassLevel;
	protected double threshold;
	protected double averagedSpread;
	
	protected int NUM_RECENT_BASS_VALS = 8;
	protected double[] recentBassLevels;
	protected int recentBassIndex;
	
	public BassFinder(int fftSize, double updatesPerSecond) {
		super(fftSize, updatesPerSecond);
	}
	
	@Override
	public void init() {
		// Initialize some parameters
		minFreq = 0;
		maxFreq = 120;
		normalizingVal = 30.0;
		averageHalfLife = 0.5;
		decayRate = 1.0 / (UPDATES_PER_SECOND / 4.0);
		phi = Math.pow(0.5, 1/(averageHalfLife * UPDATES_PER_SECOND));
		recentBassIndex = 0;
		recentBassLevels = new double[NUM_RECENT_BASS_VALS];
		
		alpha = 1 - Math.exp(Math.log(percentLowPass) / (UPDATES_PER_SECOND * timeLowPass));
		
		// Request some controls
		FrequencyRangeControl freqRangeControl = new FrequencyRangeControl(minFreq, maxFreq);
		requestUserControl(freqRangeControl);
		
	}
	
	@Override
	public void computeFeatures(double[] frequencies, double[] magnitudes, FeatureList featureList) {
		// Compute the level of bass
		double bassLevel = getFreqs(frequencies, magnitudes);
		bassLevelSmoothed = alpha * bassLevel + (1 - alpha) * bassLevelSmoothed;
		
		// Create a feature of this, and add it to the featureList.
		featureList.addFeature("BASS_LEVEL", bassLevelSmoothed);
		featureList.addFeature("BASS_RAW", currentBassLevel);
	}

	
	
	/**
	 * Does the computation behind actually measuring the bass.
	 * @return The nice bass level, smoothed out etc.
	 */
	public double getFreqs(double[] frequencies, double[] magnitudes) {

		
		double outputVal;
		
		// Compute an average from everything from minBassFreq to maxBassFreq
		double largetFreq = frequencies[frequencies.length - 1];
		int minIndex = (int) (minFreq / largetFreq * frequencies.length);
		int maxIndex = (int) (maxFreq / largetFreq * frequencies.length);
		
		double sum = 0;
		int n = 0;
		for(int i = minIndex; i <= maxIndex; i++) {
			sum += magnitudes[i]*magnitudes[i];
			n++;
		}
		
		//double level = Math.sqrt(sum);
		double level = sum / n;
		currentBassLevel = level;
		recentBassLevels[recentBassIndex] = level;
		recentBassIndex = (recentBassIndex + 1) % NUM_RECENT_BASS_VALS;
		
		
		// Compute a very low-passed version of the signal to use as an estimate of the overall
		// level of this frequency range. This is the "adaptive" part that allows the frequency
		// range finder to adjust to different volume levels
		double threshold = averagedLevel + 1.5 * averagedSpread + 10.0; //* 1.25 + 5.0;
		this.threshold = threshold;
		double spread = Math.abs(level - averagedLevel);
		
		if (level > threshold) { 
			outputVal =  (level - threshold) / averagedSpread; //normalizingVal;
		} else {
			outputVal = 0.0;
		}
		if (outputVal > 1.0) {
			outputVal = 1.0;
		}
		
		
		averagedLevel = averagedLevel * phi + level*(1 - phi);
		averagedSpread = averagedSpread * phi + spread*(1 - phi);
		
		double actualOutput;
		
		// Limit how fast the output can fall, in an attempt to minimize flicker
		if (outputVal < lastOutput - decayRate) {
			actualOutput = lastOutput - decayRate;
		} else {
			actualOutput = outputVal;
		}
		
		
		// Output
		lastOutput = actualOutput;
		return actualOutput;
		
	}

	
	public double getCurrentLevel() {
		return currentBassLevel;
	}
	
	public double getAveragedLevel() {
		return averagedLevel;
	}
	
	public double getThreshold() {
		return threshold;
	}
	
	public double getAveragedSpread() {
		return averagedSpread;
	}
	
	public double getBassDelta() {
		double deltaSum = 0;
		for(int i = 0; i < NUM_RECENT_BASS_VALS; i++) {
			deltaSum += positivify(recentBassLevels[(recentBassIndex + 1) % NUM_RECENT_BASS_VALS] - recentBassLevels[recentBassIndex % NUM_RECENT_BASS_VALS]);
		}
		
		return deltaSum;
	}
	
	private double positivify(double x) {
		if (x > 0) {
			return x;
		} else {
			return 0;
		}
	}

	
}
