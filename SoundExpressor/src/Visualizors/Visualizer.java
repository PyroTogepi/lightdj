package Visualizors;

import java.util.LinkedList;
import java.util.List;

import Common.ColorOutput;
import Common.FeatureList;
import Common.UserControl;


/**
 * Any Visualizer must implement this interface!
 * @author Steve Levine
 *
 */
public abstract class Visualizer {

	private List<UserControl> controls;
	protected int FFT_SIZE;
	protected double UPDATES_PER_SECOND;
	
	/**
	 * Do any necessary initialization. Request any user controls here.
	 */
	public abstract void init();
	
	/**
	 * The core method that visualizes a featureList
	 */
	public abstract ColorOutput visualize(FeatureList featureList);

	/**
	 * Need to know the name!
	 */
	public abstract String getName();
	
	
	public Visualizer(int fftSize, double updatesPerSecond) {
		FFT_SIZE = fftSize;
		UPDATES_PER_SECOND = updatesPerSecond;
		controls = new LinkedList<UserControl>();
	}
	
	/**
	 * FeatureDetectors may request user controls for user input using this function.
	 * Please note that this function only works when called inside init().
	 */
	protected void requestUserControl(UserControl control) {
		controls.add(control);
	}
	
	public List<UserControl> getRequestedUserControls() {
		return controls;
	}
	
}