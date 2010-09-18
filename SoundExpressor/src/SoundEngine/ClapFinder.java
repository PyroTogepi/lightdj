package SoundEngine;

/**
 * A state-machine like object that, when stepped with FFT values, attempts to output the current bass level.
 * Attempts to auto-adapt to changing volume.
 * @author steve
 *
 */
public class ClapFinder extends FrequencyRangeFinder {
	
	private double[] averagedFrequencyLevels;
	
	double lastOutput = 0;
	
	public ClapFinder(int sampleRate, int fftSize) {
		super(sampleRate, fftSize);
		
		minFreq = 1000;
		maxFreq = 16000;
		normalizingVal = 0.1;
		
		averageHalfLife = 0.125;
		
		// This will store a low pass on every frequency.
		averagedFrequencyLevels = new double[fftSize];
		for(int i = 0; i < fftSize; i++) {
			averagedFrequencyLevels[i] = 0;
		}
		
	}
	
	
	@Override
	// Estimate the bass, given an FFT.
	public double getFreqs(double[] frequencies, double[] magnitudes) {

		
		double outputVal;
		
		// Compute an average from everything from minBassFreq to maxBassFreq
		double largetFreq = frequencies[frequencies.length - 1];
		int minIndex = (int) (minFreq / largetFreq * frequencies.length);
		int maxIndex = (int) (maxFreq / largetFreq * frequencies.length);
		
		double sum = 0;
		int n = 0;
		for(int i = minIndex; i <= maxIndex; i++) {
			//sum += Math.pow(magnitudes[i], 0.5);
			averagedFrequencyLevels[i] = phi*averagedFrequencyLevels[i] + (1 - phi) *  magnitudes[i];
			
			if (magnitudes[i] / averagedFrequencyLevels[i] > 1.414) {
				// Compute the percentage of points that is higher than the averaged levels
				sum += 1.0;
			}
			
			n++;
		}
		
		double fractionInExcess = sum / n;
		double preLowPassRetVal;
		
		if (fractionInExcess > 0.0) {
			preLowPassRetVal = fractionInExcess / 0.8;
		} else {
			preLowPassRetVal = 0.0;
		}
		
		// Implement a half lowpass filter, to limit the decay rate
		double c = 0.93;
		double output;
		if (preLowPassRetVal < c * lastOutput) {
			output = c * lastOutput;
		} else {
			output = preLowPassRetVal;
		}
		lastOutput = output;
		
		return output;
		
		//double level = sum / n;
		

//		System.out.println(level + " " + averagedLevel);
//		
//		averagedLevel = averagedLevel * phi + level*(1 - phi);
//
//		if (level / averagedLevel > 1.5) {
//			return 1.0;
//		} else {
//			return 0.0;
//		}
//		
		
	}
	
	
	public double[] getAveragedFreqs() {
		return averagedFrequencyLevels;
	}

	
}