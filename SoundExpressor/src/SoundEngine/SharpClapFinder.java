package SoundEngine;

/**
 * A state-machine like object that, when stepped with FFT values, attempts to output the current bass level.
 * Attempts to auto-adapt to changing volume.
 * @author steve
 *
 */
public class SharpClapFinder  {
	
	protected double averageHalfLife;
	
	protected double updatesPerSecond;
	protected double phi;
	protected double decayRate;
	protected double normalizingVal;
	protected double minFreq;
	protected double maxFreq;
	protected double lastOutput;
	
	protected double averagedLevel;
	protected double currentBassLevel;
	protected double threshold;
	protected double averagedSpread;
	
	protected int NUM_RECENT_BASS_VALS = 8;
	protected double[] recentBassLevels;
	protected int recentBassIndex;
	
	public SharpClapFinder(int sampleRate, int fftSize) {

		// Initiate other parameters
		minFreq = 8000;
		maxFreq = 15000;
		normalizingVal = 30.0;
		averageHalfLife = 1.0;
		decayRate = 1.0 / (25);
		
		// Calculate some parameters
		updatesPerSecond = 1.0 * sampleRate / fftSize; 
		phi = Math.pow(0.5, 1/(averageHalfLife * updatesPerSecond));
		
		recentBassIndex = 0;
		recentBassLevels = new double[NUM_RECENT_BASS_VALS];
		
	}
	
	public double getFreqs(double[] frequencies, double[] magnitudes) {

		
		double outputVal;
		
		// Compute an average from everything from minBassFreq to maxBassFreq
		double largetFreq = frequencies[frequencies.length - 1];
		int minIndex = (int) (minFreq / largetFreq * frequencies.length);
		int maxIndex = (int) (maxFreq / largetFreq * frequencies.length);
		
		double sum = 0;
		int n = 0;
		for(int i = minIndex; i <= maxIndex; i++) {
			sum += magnitudes[i];
			n++;
		}
		
		double level = sum / n;
		currentBassLevel = level;
		
		recentBassLevels[recentBassIndex] = level;
		recentBassIndex = (recentBassIndex + 1) % NUM_RECENT_BASS_VALS;
		
		
		//level = getBassDelta();
		
		
		// Compute a very low-passed version of the signal to use as an estimate of the overall
		// level of this frequency range. This is the "adaptive" part that allows the frequency
		// range finder to adjust to different volume levels
		double threshold = averagedLevel + averagedSpread*2.0 + 0.07; //* 1.25 + 5.0;
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
		
//		// Limit how fast the output can fal, in an attempt to minimize flicker
//		if (outputVal < decayRate * lastOutput) {
//			actualOutput = decayRate * lastOutput;
//		} else {
//			actualOutput = outputVal;
//		}
		
		// Limit how fast the output can fal, in an attempt to minimize flicker
		if (outputVal < lastOutput - decayRate) {
			actualOutput = lastOutput - decayRate;
		} else {
			actualOutput = outputVal;
		}
		
		//actualOutput = Math.log(actualOutput + 1.0) / Math.log(2);
		
		
		
//		if (actualOutput > 0.5) {
//			actualOutput = actualOutput;
//		} else {
//			actualOutput = 0;
//		}
		
		
		
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