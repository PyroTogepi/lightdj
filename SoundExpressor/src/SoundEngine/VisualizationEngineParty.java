package SoundEngine;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.swing.JFrame;

import Common.ColorOutput;
import Common.FeatureList;
import FeatureDetectors.BassFinder;
import FeatureDetectors.ClapFinder;
import FeatureDetectors.FeatureDetector;
import FeatureDetectors.FrequencyRangeFinder;
import FeatureDetectors.LevelMeter;
import LightDJGUI.ColorOutputDisplayer;
import LightDJGUI.ColorOutputDisplayerPanel;
import LightDJGUI.CrossfaderKnob;
import LightDJGUI.LightDJGUI;
import LightDJGUI.MouseAcceptorPanel;
import LightDJGUI.ScrollingSpectrum;
import LightDJGUI.VisualizerChooser;
import PartyLightsController.PartyLightsController;
import SignalGUI.ChannelLights;
import SignalGUI.ColoredLight;
import SignalGUI.GraphDisplay;
import SignalGUI.RGBLight;
import SignalGUI.RealtimePlotter;
import SignalGUI.ScrollingChannel;
import SignalGUI.TextLight;
import Signals.FFT;
import Utils.TimerTicToc;
import Visualizors.RainbowSlider;
import Visualizors.RedBassColoredClapVisualizer;
import Visualizors.VUBass;
import Visualizors.Visualizer;
import Visualizors.WhitePulseBass;

/**
 * This class is responsible for music visualizations for Steve's LED's.
 * @author Steve Levine
 */
public class VisualizationEngineParty extends VisualizationEngine implements ComponentListener {

	// Visualization stuff
	LightDJGUI gui;
	GraphDisplay graphMapper;
	ScrollingChannel channelMapper;
	ChannelLights lights;
	ColoredLight bassLight;
	RGBLight rgbLight;
	TextLight textLight;
	RealtimePlotter plotter;

	// The list of feature detectors
	public ArrayList<FeatureDetector> featureDetectors;
	
	// The list of visualizers
	public ArrayList<Visualizer> visualizers;
	
	
	// The arduino LED visualizer
	//LEDVisualizer ledVisuals;
	PartyLightsController ledVisuals;
	
	TimerTicToc tictoc;
	

	public VisualizationEngineParty(AudioFormat format, double videoDelaySec) {
		super(format, videoDelaySec);
		tictoc = new TimerTicToc();
	}
	
	@Override
	protected void initVisualizations() {

		// Set up the feature detector plugins
		featureDetectors = allFeatureDetectors();
		for(FeatureDetector f : featureDetectors) {
			f.init();
			
			// Get the list of controls requested
			
		}
		
		
		// Set up all of the visualizer plugins
		visualizers = allVisualizers();
		for(Visualizer v : visualizers) {
			v.init();
			
			// Get the list of controls requested
			
		}
		
		
		try {
			//ledVisuals = new LEDVisualizer();
			ledVisuals = new PartyLightsController();
		} catch (Throwable o) {
			System.out.println("WARNING: Couldn't connect to LED's via USB!");
		}
		
		
		
		// Set up the GUI
		startGUI();
		
	}
	
	/**
	 * Return the comprehensive list of all FeatureDetectors. Must be added here to show up0 the LightDJ GUI.
	 * @return
	 */
	public ArrayList<FeatureDetector> allFeatureDetectors() {
		ArrayList<FeatureDetector> detectors = new ArrayList<FeatureDetector>();
		
		int FFT_SIZE = BUFFER_SIZE;
		double UPDATES_PER_SECOND = 1.0 * SAMPLE_RATE / FFT_SIZE * BUFFER_OVERLAP; 
		
		// Add the detectors here
		detectors.add(new BassFinder(FFT_SIZE, UPDATES_PER_SECOND));
		detectors.add(new ClapFinder(FFT_SIZE, UPDATES_PER_SECOND));
		detectors.add(new FrequencyRangeFinder(FFT_SIZE, UPDATES_PER_SECOND));
		detectors.add(new LevelMeter(FFT_SIZE, UPDATES_PER_SECOND));
		
		
		// Return them all
		return detectors;
		
	}
	
	/**
	 * Return the comprehensive list of all FeatureDetectors. Must be added here to show up in the LightDJ GUI.
	 * @return
	 */
	public ArrayList<Visualizer> allVisualizers() {
		ArrayList<Visualizer> visualizers = new ArrayList<Visualizer>();
		
		int FFT_SIZE = BUFFER_SIZE;
		double UPDATES_PER_SECOND = 1.0 * SAMPLE_RATE / FFT_SIZE * BUFFER_OVERLAP; 
		
		// Add the detectors here
		
		visualizers.add(new RedBassColoredClapVisualizer(FFT_SIZE, UPDATES_PER_SECOND));
		visualizers.add(new VUBass(FFT_SIZE, UPDATES_PER_SECOND));
		visualizers.add(new WhitePulseBass(FFT_SIZE, UPDATES_PER_SECOND));
		visualizers.add(new RainbowSlider(FFT_SIZE, UPDATES_PER_SECOND));
		
		//visualizers.add(new CrazyStrobe(FFT_SIZE, UPDATES_PER_SECOND));
		
		return visualizers;
		
	}

	
	
	@Override
	protected RenderFrame computeVisualsRendering(FFT fft) {
		
		// Create a featurelist, and pass it along with the FFT to each FeatureDetector
		FeatureList featureList = new FeatureList();
		double[] frequencies = fft.getFrequencies();
		double[] magnitudes = fft.getMagnitudes();
		
		// Compute all of the features
		for(FeatureDetector f : featureDetectors) {
			f.computeFeatures(frequencies, magnitudes, featureList);
		}
		
		// Now that we have a full-fledged FeatureList, pass it to the Visualizers
		ColorOutput[] colorOutputs = new ColorOutput[visualizers.size()];
		for(int i = 0; i < visualizers.size(); i++) {
			Visualizer v = visualizers.get(i);
			ColorOutput c = v.visualize(featureList);
			colorOutputs[i] = c;
		}
		
		
		RenderFrameParty renderFrame = new RenderFrameParty();
		renderFrame.colorOutputs = colorOutputs;
		
		
		
		//plotter.update(new double[] {100.0*((Double) featureList.getFeature("BASS_RAW")), 0.0});
		
		
		//plotter.render();
		spectrumMapper.updateWithNewSpectrum(frequencies, magnitudes);
		
		
		return renderFrame;
	}

	@Override
	protected void renderVisuals(RenderFrame rf) {
		
		RenderFrameParty renderFrame = (RenderFrameParty) rf;

		// Mix the colors as requested by the LightDJ
		ColorOutput colorOutput = mixColors(renderFrame);
		
		// Apply any necessary post-processing
		applyPostProcessing(colorOutput);
		
		// Send the command to the LED's
		ledVisuals.visualize(colorOutput);	// Send SERIAL to the RGB's
		
		
		
		// Store the last frame so that it can be rendered appropriately!
		synchronized(this) {
			lastFrame = renderFrame;
		}
	
	}
	
	protected void applyPostProcessing(ColorOutput colorOutput) {
		switch(lightDJPostProcessing) {
		case POST_PROCESSING_NONE:
			// Do nothing!
			break;
			
		case POST_PROCESSING_EMERGENCY_LIGHTING:
			colorOutput.emergencyLighting();
			break;
			
		case POST_PROCESSING_ALL_OFF:
			colorOutput.allOff();
			break;
			
		case POST_PROCESSING_WHITE_STROBE:
			whiteStrobe(colorOutput);
			break;
			
		case POST_PROCESSING_UV_STROBE:
			uvStrobe(colorOutput);
			break;
			
		}
	}
	
	protected void whiteStrobe(ColorOutput colorOutput) {
		if (strobeFrame == 0) {
			colorOutput.setWhiteStrobe();
		} else {
			colorOutput.allOff();
		}
		strobeFrame = (strobeFrame + 1) % strobeFrameLength;
	}
	
	
	protected void uvStrobe(ColorOutput colorOutput) {
		if (strobeFrame == 0) {
			colorOutput.setUVStrobe();
		} else {
			colorOutput.allOff();
		}
		strobeFrame = (strobeFrame + 1) % strobeFrameLength;
	}
	
	/**
	 * Takes in the current color frame to be rendered. Retrieves the current cross-fade mixing
	 * parameter from the LightingDJ GUI, and mixes the appropriate ColorOutputs together to yield
	 * one color output.
	 */
	protected ColorOutput mixColors(RenderFrameParty rf) {
		
		return ColorOutput.mix(rf.colorOutputs[visualizerLeftIndex], rf.colorOutputs[visualizerRightIndex], alpha);
		
		
	}
	

	/**
	 * TODO
	 * TODO
	 * TODO
	 * 
	 * The following methods and fields are all for the LightDJ GUI!
	 * 
	 * TODO
	 * TODO
	 * TODO
	 */
	// Graphics and GUI-related variables
	protected final static int SIDEBAR_WIDTH = 350;
	protected final static int SPECTRUM_WIDTH = 900;
	protected final static int SPECTRUM_HEIGHT = 200;
	protected final static int BORDER_SIZE = 10;
	protected final static int LIGHTBAR_HEIGHT = 200;
	protected final static int RECORD_CONTROLS_WIDTH = 500;
	protected final static int RECORD_CONTROLS_HEIGHT = 400;
	protected final static int RECORD_WIDTH = 400;
	protected final static int RECORDS_WIDTH = 2*RECORD_CONTROLS_WIDTH + BORDER_SIZE;
	protected static int ACTIVE_LAYER_X = 9*BORDER_SIZE;
	protected static int ACTIVE_LAYER_Y = 9*BORDER_SIZE;
	protected static int ACTIVE_LAYER_WIDTH;
	protected static int ACTIVE_LAYER_HEIGHT;
	protected static int RECORD_BOX_LEFT_X;
	protected static int RECORD_BOX_LEFT_Y;
	protected static int RECORD_BOX_RIGHT_X;
	protected static int RECORD_BOX_RIGHT_Y;
	protected static final int RECORD_BOX_COLOR_DISPLAY_HEIGHT = 100;
	protected static int CROSSFADER_X;
	protected static int CROSSFADER_Y;
	protected static int CROSSFADER_WIDTH;
	protected static int CROSSFADER_HEIGHT;
	protected static int CROSSFADER_INDENT = 10;
	protected static int PLUGIN_THUMBNAIL_WIDTH = 300;
	protected static int PLUGIN_THUMBNAIL_HEIGHT = 200;
	
	// Some color information
	protected static Color PANEL_BACKGROUND_COLOR;
	protected static Color PANEL_BORDER_COLOR;
	protected static Color TEXT_COLOR;
	protected static Color HOT_COLOR;
	protected static AlphaComposite COMPOSITE_TRANSLUCENT;
	protected static AlphaComposite COMPOSITE_OPAQUE;
	protected static Font PANEL_FONT;
	protected static Font PANEL_FONT_LARGE;
	protected static Stroke REGULAR_STROKE;
	protected static Stroke THICK_STROKE;
	
	// Render buffers
	protected BufferedImage background;
	protected BufferedImage buffer;
	
	// Preloaded images
	protected BufferedImage turntableLogo;
	protected BufferedImage recordLeft;
	protected BufferedImage recordRight;
	
	// Controls and displays
	protected ScrollingSpectrum spectrumMapper;
	protected RenderFrameParty lastFrame;
	protected ColorOutputDisplayer colorOutputDisplayer;
	protected CrossfaderKnob crossfaderKnob;
	protected VisualizerChooser visualizerChooser;
	
	// Keep track of all the elements that can receive mouse events.
	protected List<MouseAcceptorPanel> mouseAcceptors;
	protected MouseAcceptorPanel activePanel;
	
	// Whether or not to draw a layer above everything else, for fine-tuned control
	public boolean activeLayer = false;
	
	// Which visualizers are currently being used
	protected static int visualizerLeftIndex = 0;
	protected static int visualizerRightIndex = 1;
	protected static int activePlugin = 1;	// 0 => Left is selected, 1 => Right is selected
	protected static double alpha;	// The mixing between the two.
	protected static boolean controlKeyPressed = false;
	protected static boolean altKeyPressed = false;
	
	// Store the state of the LightDJ
	public enum LightDJState {
		LIGHTDJ_STATE_NORMAL,
		LIGHTDJ_STATE_CHOOSING_VISUALIZER
	}
	protected LightDJState lightDJState;
	
	// Allow for automated cross-fade effects
	public enum CrossfadeAutomator {
		CROSSFADE_MANUAL,
		CROSSFADE_AUTO
	}
	protected CrossfadeAutomator crossfadeAutomator;
	protected double crossfadeSpeed = 0.0;
	protected static double CROSSFADE_SPEED_SLOW = 0.015;
	protected static double CROSSFADE_SPEED_FAST = 0.04;
	
	// Keep track of available post-processings
	public enum LightDJPostProcessing {
		POST_PROCESSING_NONE,	// Send the mixed color-data as is
		POST_PROCESSING_WHITE_STROBE,	// Ignore the color data and instead white strobe
		POST_PROCESSING_UV_STROBE,	// Ignore the color data and instead UV strobe
		POST_PROCESSING_ALL_OFF,	// Turn all lights off temporarily
		POST_PROCESSING_EMERGENCY_LIGHTING	// Turn on all lights to white
	}
	protected LightDJPostProcessing lightDJPostProcessing;
	
	// Keep track of strobing!
	protected int strobeFrame = 0;
	protected int strobeFrameLength = 13;
	
	public void startGUI() {
		gui = new LightDJGUI(this);
		JFrame frame = new JFrame("Light DJ");
		frame.setUndecorated(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(gui);
		frame.pack();
		gui.setBackground(Color.BLACK);
		frame.setSize(1500, 1000);
		frame.setLocation(0, 0);
		frame.setExtendedState(Frame.MAXIMIZED_BOTH);
		frame.setVisible(true);
		frame.addComponentListener(this);
		
		
		// Set some colors
		PANEL_BACKGROUND_COLOR = new Color(10, 10, 10);
		PANEL_BORDER_COLOR = new Color(30, 30, 30);
		TEXT_COLOR = new Color(160, 160, 160);
		HOT_COLOR = new Color(200, 114, 0);
		COMPOSITE_OPAQUE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
		COMPOSITE_TRANSLUCENT = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f);
		PANEL_FONT = new Font("Eraser", Font.PLAIN, 24);
		PANEL_FONT_LARGE = new Font("Eraser", Font.PLAIN, 48);
		REGULAR_STROKE = new BasicStroke(1.0f);
		THICK_STROKE = new BasicStroke(3.0f);
		alpha = 0.0;
		
		// Set the default states
		lightDJState = LightDJState.LIGHTDJ_STATE_NORMAL;
		lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_NONE;
		crossfadeAutomator = CrossfadeAutomator.CROSSFADE_MANUAL;
		
		// Set up some other GUI elements
		spectrumMapper = new ScrollingSpectrum(0, 0, SPECTRUM_WIDTH, SPECTRUM_HEIGHT, null, 30, 20000, 100.0, BUFFER_SIZE, SAMPLE_RATE);
		
		// Choose which color output displayer to use!
		colorOutputDisplayer = new ColorOutputDisplayerPanel();
		
		// Start the crossfader knob
		crossfaderKnob = new CrossfaderKnob(this);
		
		// Start the visualizer chooser
		visualizerChooser = new VisualizerChooser(this, visualizers);
		
		// Set up the mouse acceptors
		mouseAcceptors = new LinkedList<MouseAcceptorPanel>();
		activePanel = null;
		mouseAcceptors.add(visualizerChooser);
		mouseAcceptors.add(crossfaderKnob);
		
		
		
		// Generate the background
		generateBackground();
	
		
		// Start a tast to render regularly!
		Timer t = new Timer();
		t.scheduleAtFixedRate(new RenderTask(this), 0, 50);
		
		System.out.println("Light DJ started.");
	}
	
	
	/**
	 * Generate a pretty looking background image
	 */
	private void generateBackground() {
		// Allocate an image of the proper size
		int width = gui.getWidth();
		int height = gui.getHeight();
		background = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = (Graphics2D) background.getGraphics();
		
		// Redefine some geometric parameters
		RECORD_BOX_LEFT_X = SIDEBAR_WIDTH + (width - RECORDS_WIDTH - SIDEBAR_WIDTH) / 2 + BORDER_SIZE;
		RECORD_BOX_LEFT_Y = BORDER_SIZE*2 + LIGHTBAR_HEIGHT;
		RECORD_BOX_RIGHT_X = SIDEBAR_WIDTH + (width - RECORDS_WIDTH - SIDEBAR_WIDTH) / 2 + BORDER_SIZE*2 + RECORD_CONTROLS_WIDTH;
		RECORD_BOX_RIGHT_Y =  BORDER_SIZE*2 + LIGHTBAR_HEIGHT;
		
		ACTIVE_LAYER_WIDTH = width - 18*BORDER_SIZE;
		ACTIVE_LAYER_HEIGHT = height - 18*BORDER_SIZE;
		
		CROSSFADER_X = RECORD_BOX_LEFT_X + RECORD_CONTROLS_WIDTH / 2;
		CROSSFADER_Y = RECORD_BOX_LEFT_Y + RECORD_CONTROLS_HEIGHT + 200;
		CROSSFADER_WIDTH = RECORD_CONTROLS_WIDTH + BORDER_SIZE;
		CROSSFADER_HEIGHT = 150;
		
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		
		// Paint a black background
		g2D.setBackground(Color.BLACK);
		g2D.clearRect(0, 0, width, height);
		
		
		// Display the turntable image, loading it if necessary.
		if (turntableLogo == null) {
			// Try and load it
			try {
				turntableLogo = ImageIO.read(new File("Images/background.png"));
				recordLeft = ImageIO.read(new File("Images/record_left.png"));
				recordRight = ImageIO.read(new File("Images/record_right.png"));
			} catch (IOException e) {
				System.out.println("Warning: Could not load LightDJ background image!");
				e.printStackTrace();
				return;
			}
		}
		g2D.drawImage(turntableLogo, SIDEBAR_WIDTH, 0, null);
		
		// Draw the sidebar
		//g2D.setColor(new Color(10,10,10));
		//g2D.fillRect(0, 0, SIDEBAR_WIDTH, height);
		g2D.setColor(PANEL_BORDER_COLOR);
		g2D.drawLine(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH, height);
		

		g2D.drawImage(recordLeft, RECORD_BOX_LEFT_X + (RECORD_CONTROLS_WIDTH - RECORD_WIDTH) /2, RECORD_BOX_LEFT_Y + RECORD_CONTROLS_HEIGHT, null);
		g2D.drawImage(recordRight, RECORD_BOX_RIGHT_X + (RECORD_CONTROLS_WIDTH - RECORD_WIDTH) /2, RECORD_BOX_RIGHT_Y + RECORD_CONTROLS_HEIGHT, null);
		

		
		// Render the visualizer plugins
		loadVisualizerPlugin(true, visualizerLeftIndex);
		loadVisualizerPlugin(false, visualizerRightIndex);
		
		// Resize where the scrolling spectrum goes
		//spectrumMapper.move(SIDEBAR_WIDTH + BORDER_SIZE, gui.getHeight() - SPECTRUM_HEIGHT - BORDER_SIZE , gui.getWidth() - SIDEBAR_WIDTH - 2*BORDER_SIZE, SPECTRUM_HEIGHT);
		spectrumMapper.move(BORDER_SIZE, gui.getHeight() - SPECTRUM_HEIGHT - BORDER_SIZE , SIDEBAR_WIDTH - 2*BORDER_SIZE, SPECTRUM_HEIGHT);
		spectrumMapper.setGraphics((Graphics2D) background.getGraphics());
		
		// Make sure the visualizer chooser is set up correctly
		visualizerChooser.setPosition(ACTIVE_LAYER_X, ACTIVE_LAYER_Y, ACTIVE_LAYER_WIDTH, ACTIVE_LAYER_HEIGHT);
		
		// Make sure the crossfader knob is set up correctly!
		crossfaderKnob.setPosition(CROSSFADER_X, CROSSFADER_Y, CROSSFADER_WIDTH, CROSSFADER_HEIGHT);
		
		// Draw the cross-fader knob
		paintCrossfader(false);
		
		
	}
	
	/**
	 * Draws everything for the visualizer plugin
	 */
	private void loadVisualizerPlugin(boolean left, int pluginIndex) {
		
		int x; int y; int width; int height;
		
		Visualizer visualizer = visualizers.get(pluginIndex);
		Graphics2D g2D = (Graphics2D) background.getGraphics();
		
		if (left) {
			g2D.setColor(PANEL_BACKGROUND_COLOR);
			g2D.fillRoundRect(RECORD_BOX_LEFT_X, RECORD_BOX_LEFT_Y, RECORD_CONTROLS_WIDTH, RECORD_CONTROLS_HEIGHT, 40, 40);
			g2D.setColor(PANEL_BORDER_COLOR);
			g2D.drawRoundRect(RECORD_BOX_LEFT_X, RECORD_BOX_LEFT_Y, RECORD_CONTROLS_WIDTH, RECORD_CONTROLS_HEIGHT, 40, 40);
			x = RECORD_BOX_LEFT_X + BORDER_SIZE;
			y = RECORD_BOX_LEFT_Y + BORDER_SIZE;
			width = RECORD_CONTROLS_WIDTH - 2*BORDER_SIZE;
			height = RECORD_CONTROLS_HEIGHT - 2*BORDER_SIZE;
			
		} else {
			g2D.setColor(PANEL_BACKGROUND_COLOR);
			g2D.fillRoundRect(RECORD_BOX_RIGHT_X, RECORD_BOX_RIGHT_Y, RECORD_CONTROLS_WIDTH, RECORD_CONTROLS_HEIGHT, 40, 40);
			g2D.setColor(PANEL_BORDER_COLOR);
			g2D.drawRoundRect(RECORD_BOX_RIGHT_X, RECORD_BOX_RIGHT_Y, RECORD_CONTROLS_WIDTH, RECORD_CONTROLS_HEIGHT, 40, 40);
			x = RECORD_BOX_RIGHT_X + BORDER_SIZE;
			y = RECORD_BOX_RIGHT_Y + BORDER_SIZE;
			width = RECORD_CONTROLS_WIDTH - 2*BORDER_SIZE;
			height = RECORD_CONTROLS_HEIGHT - 2*BORDER_SIZE;
		}
		
		
		g2D.setColor(TEXT_COLOR);
		g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2D.setFont(PANEL_FONT);
		g2D.drawString(visualizer.getName(), x, y + 16);
		g2D.setColor(PANEL_BORDER_COLOR);
		g2D.drawLine(x, y + 25, x + width, y + 25);
		
	}
	
	public void setMixerAlpha(double a) {
		
		// Disable any automated cross-fading
		crossfadeAutomator = CrossfadeAutomator.CROSSFADE_MANUAL;
		
		// Set the mixer!
		if (a < 0.0) {
			alpha = 0.0;
		} else if (a > 1.0) {
			alpha = 1.0;
		} else {
			alpha = a;
		}
		
	}
	
	/**
	 * Paint the crossfader knob
	 */
	public void paintCrossfader(boolean hot) {
		Graphics2D g2D = (Graphics2D) background.getGraphics();
		
		// Erase what was there before
		g2D.setColor(Color.BLACK);
		g2D.fillRect(CROSSFADER_X, CROSSFADER_Y, CROSSFADER_WIDTH, CROSSFADER_HEIGHT);
		
		
		g2D.setColor(PANEL_BORDER_COLOR);
		//g2D.drawRect(CROSSFADER_X, CROSSFADER_Y, CROSSFADER_WIDTH, CROSSFADER_HEIGHT);
		g2D.setStroke(THICK_STROKE);
		int yCenter = CROSSFADER_Y + CROSSFADER_HEIGHT / 2;
		g2D.drawLine(CROSSFADER_X + CROSSFADER_INDENT, yCenter, CROSSFADER_X + CROSSFADER_WIDTH - CROSSFADER_INDENT, yCenter);
	
		// Draw hair lines
		for (int i = 0; i  <= 8; i++) {
			int x = CROSSFADER_X + CROSSFADER_INDENT + i * (CROSSFADER_WIDTH - 2*CROSSFADER_INDENT) / 8;
			g2D.drawLine(x, yCenter - 10, x, yCenter + 10);
		}
	
		// Now draw the main box
		g2D.setStroke(REGULAR_STROKE);
		int x = (int) (CROSSFADER_X + CROSSFADER_INDENT + alpha * (CROSSFADER_WIDTH - 2*CROSSFADER_INDENT));
		if (hot) {
			g2D.setColor(HOT_COLOR);
		} else {
			g2D.setColor(PANEL_BACKGROUND_COLOR);
		}
		g2D.fillRect(x - 10, yCenter - 40, 19, 80);
		if (hot) {
			g2D.setColor(Color.WHITE);
		} else {
			g2D.setColor(PANEL_BORDER_COLOR);
		}
		
		g2D.drawRect(x - 10, yCenter - 40, 19, 80);
		
	}
	
	
	/**
	 * Paint the visualizer chooser
	 */
	public void paintVisualizerChooser(RenderFrameParty renderFrame) {
		Graphics2D g2D = (Graphics2D) buffer.getGraphics();
	
		g2D.setFont(PANEL_FONT_LARGE);
		g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING , RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2D.setColor(TEXT_COLOR);
		g2D.drawString("Visualizorz", ACTIVE_LAYER_X, ACTIVE_LAYER_Y - 10);
		
		// Now, draw each plugin.
		int col = 0;
		int row = 0;
		int i = 0;
		for(int pluginIndex = 0; pluginIndex < visualizers.size(); pluginIndex++) {
			Visualizer v = visualizers.get(pluginIndex);
			int x = ACTIVE_LAYER_X + 2*BORDER_SIZE + (PLUGIN_THUMBNAIL_WIDTH + BORDER_SIZE) * col;
			int y = ACTIVE_LAYER_Y + 2*BORDER_SIZE + (PLUGIN_THUMBNAIL_HEIGHT + BORDER_SIZE) * row;
			
			g2D.setColor(PANEL_BORDER_COLOR);
			g2D.drawRoundRect(x, y, PLUGIN_THUMBNAIL_WIDTH, PLUGIN_THUMBNAIL_HEIGHT, 20, 20);
			
			
			g2D.setFont(PANEL_FONT);
			g2D.setColor(TEXT_COLOR);
			g2D.drawString(v.getName(), x + 30, y);
			
			g2D.setFont(PANEL_FONT);
			g2D.setColor(HOT_COLOR);
			g2D.drawString("abcdefghijklmnopqrstuvwxyz".substring(i, i+1), x, y);
			
			colorOutputDisplayer.render(renderFrame.colorOutputs[pluginIndex], g2D, x, y + 40, PLUGIN_THUMBNAIL_WIDTH, PLUGIN_THUMBNAIL_HEIGHT - 40);
			
			
			// Now increment
			col++;
			i++;
			if (col * PLUGIN_THUMBNAIL_WIDTH > ACTIVE_LAYER_WIDTH - 4*BORDER_SIZE) {
				col = 0;
				row++;
			}
			
			if (row * PLUGIN_THUMBNAIL_HEIGHT > ACTIVE_LAYER_HEIGHT - 4*BORDER_SIZE) {
				return;
			}
			
			
		}
	
	}
	
	
	public void chooseVisualizer(int pluginIndex, int activePlugin) {
		
		if (activePlugin == 0) {
			visualizerLeftIndex = pluginIndex;
			loadVisualizerPlugin(true, pluginIndex);
		} else {
			visualizerRightIndex = pluginIndex;
			loadVisualizerPlugin(false, pluginIndex);
		}
		
		
		// Set the state 
		lightDJState = LightDJState.LIGHTDJ_STATE_NORMAL;
		
		
	}
	
	
	/**
	 * Render the Light DJ GUI 
	 * (Called automagically by a timer)
	 */
	public void renderDJ() {
		RenderFrameParty renderFrame;
		synchronized(this) {
			renderFrame = lastFrame;
		}
		
		if (renderFrame == null) {
			// Don't render!
			return;
		}
		
		Graphics2D g2D = (Graphics2D) background.getGraphics();
		int width = gui.getWidth();
		int height = gui.getHeight();
		
		// Compute the mixed colors
		ColorOutput mixedColors = mixColors(renderFrame); 
		ColorOutput leftColors = renderFrame.colorOutputs[visualizerLeftIndex];
		ColorOutput rightColors = renderFrame.colorOutputs[visualizerRightIndex];
		
		colorOutputDisplayer.render(mixedColors, g2D, SIDEBAR_WIDTH + BORDER_SIZE, BORDER_SIZE, width - SIDEBAR_WIDTH - 2*BORDER_SIZE, LIGHTBAR_HEIGHT);
		colorOutputDisplayer.render(leftColors, g2D, RECORD_BOX_LEFT_X + BORDER_SIZE, RECORD_BOX_LEFT_Y + RECORD_CONTROLS_HEIGHT - BORDER_SIZE - RECORD_BOX_COLOR_DISPLAY_HEIGHT, RECORD_CONTROLS_WIDTH - 2*BORDER_SIZE, RECORD_BOX_COLOR_DISPLAY_HEIGHT); 
		colorOutputDisplayer.render(rightColors, g2D, RECORD_BOX_RIGHT_X + BORDER_SIZE, RECORD_BOX_RIGHT_Y + RECORD_CONTROLS_HEIGHT - BORDER_SIZE - RECORD_BOX_COLOR_DISPLAY_HEIGHT, RECORD_CONTROLS_WIDTH - 2*BORDER_SIZE, RECORD_BOX_COLOR_DISPLAY_HEIGHT); 
		
		
		// Render the spectrum in the lower left corner
		spectrumMapper.render();
		
		Graphics2D g2DGui = (Graphics2D) gui.getGraphics();
		Graphics2D g2DBuf = (Graphics2D) buffer.getGraphics();
		
		
		if (activeLayer) {
			g2DBuf.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			g2DBuf.drawImage(background, 0, 0, null);
			
			g2DBuf.setColor(PANEL_BACKGROUND_COLOR);
			g2DBuf.setComposite(COMPOSITE_TRANSLUCENT);
			g2DBuf.fillRoundRect(ACTIVE_LAYER_X, ACTIVE_LAYER_Y - BORDER_SIZE, ACTIVE_LAYER_WIDTH, ACTIVE_LAYER_HEIGHT, 50, 50);
			g2DBuf.setComposite(COMPOSITE_OPAQUE);
			
			g2DBuf.setColor(PANEL_BORDER_COLOR);
			g2DBuf.drawRoundRect(ACTIVE_LAYER_X, ACTIVE_LAYER_Y - BORDER_SIZE, ACTIVE_LAYER_WIDTH, ACTIVE_LAYER_HEIGHT, 50, 50);
			
			// Now, depending on the state
			if (lightDJState == LightDJState.LIGHTDJ_STATE_CHOOSING_VISUALIZER) {
				// Draw more stuff
				paintVisualizerChooser(renderFrame);
			}
			
			
			g2DGui.drawImage(buffer, 0, 0, null);
		
		} else {
			
			g2DGui.drawImage(background, 0, 0, null);
		}
		
	}
	
	
	
	public void paintDJ(Graphics g) {
		// Repaint the background
		Graphics2D g2D = (Graphics2D) g;
		g2D.drawImage(background, 0, 0, null);
		
	}
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {generateBackground();}

	/**
	 * Mouse stuff
	 */
	public void mouseDown(int xM, int yM) {
		for(MouseAcceptorPanel p : mouseAcceptors) {
			// Does the mouse event fall into this panel? If it does, cast it!
			int x = p.getX();
			int y = p.getY();
			int w = p.getWidth();
			int h = p.getHeight();
			
			if (p.isVisible() && xM >= x && xM < x + w && yM >= y && yM < y + h) {
				// Found it! Convert to relative coordinates and go.
				activePanel = p;
				p.mouseDown(xM - x, yM - y);
				return;
			}
			
		}
	}
	
	public void mouseUp(int xM, int yM) {
		if (activePanel == null) {
			return;
		}
		activePanel.mouseUp(xM - activePanel.getX(), yM - activePanel.getY());
		activePanel = null;
	}
	
	public void mouseDragged(int xM, int yM) {
		if (activePanel == null) {
			return;
		}
		activePanel.mouseDragged(xM - activePanel.getX(), yM - activePanel.getY());
	}
	
	/**
	 * Keyboard stuff
	 */
	public void keyDown(int keyCode) {
		
		// Is it a special key?
		if (keyCode == KeyEvent.VK_CONTROL) {
			controlKeyPressed = true;
		} else if (keyCode == KeyEvent.VK_ALT) {
			altKeyPressed = true;
		}
		
		// Emergency lighting?
		if (keyCode == KeyEvent.VK_ESCAPE) {
			
		}
		
		
		// Switch based on the current LightDJ state
		switch(lightDJState) {

		case LIGHTDJ_STATE_NORMAL:
			if (keyCode == KeyEvent.VK_SPACE) {
				activeLayer = true;
				lightDJState = LightDJState.LIGHTDJ_STATE_CHOOSING_VISUALIZER;
			} else if (keyCode == KeyEvent.VK_LEFT) {
				if (controlKeyPressed) {
					startAutoCrossfade(-CROSSFADE_SPEED_SLOW);
				} else if (altKeyPressed) {
					startAutoCrossfade(-CROSSFADE_SPEED_FAST);
				} else {
					// Left arrow - cross-fade all the way to the left if not automated
					alpha = 0.0;
					paintCrossfader(false);
				}
				
			} else if (keyCode == KeyEvent.VK_RIGHT) {
				if (controlKeyPressed) {
					startAutoCrossfade(CROSSFADE_SPEED_SLOW);
				} else if (altKeyPressed) {
					startAutoCrossfade(CROSSFADE_SPEED_FAST);
				} else {
					// Right arrow - cross-fade all the way to the right if not automated
					alpha = 1.0;
					paintCrossfader(false);
				}
			} else if (keyCode == KeyEvent.VK_BACK_SPACE) {
				 // This is a trigger for all off - turn off all lights!
				lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_ALL_OFF;
				
			} else if (keyCode == KeyEvent.VK_F12) {
				// This is a trigger for white-strobing!
				lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_WHITE_STROBE;
			} else if (keyCode == KeyEvent.VK_ESCAPE) {
				// Turn emergency lighting on/off
				if (lightDJPostProcessing == LightDJPostProcessing.POST_PROCESSING_EMERGENCY_LIGHTING) {
					lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_NONE;
				} else {
					lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_EMERGENCY_LIGHTING;
				}
				
				
			}
			
			break;
			
			
		case LIGHTDJ_STATE_CHOOSING_VISUALIZER:
			if (keyCode == KeyEvent.VK_SPACE) {
				activeLayer = false;
				lightDJState = LightDJState.LIGHTDJ_STATE_NORMAL;
			} else if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
				int rawIndex = keyCode - KeyEvent.VK_A;
				if (rawIndex >= visualizers.size()) {
					rawIndex = visualizers.size() - 1;
				}
				
				// Choose which to modify - the left or the right plugin
				int activePlugin = (int) (1.0 - Math.round(alpha));
				
				// Set that effect
				chooseVisualizer(rawIndex, activePlugin);
				activeLayer = false;
				
			}
		
			break;
		
		}
	}
	
	public void keyUp(int keyCode) {
		// Is it a special key?
		if (keyCode == KeyEvent.VK_CONTROL) {
			controlKeyPressed = false;
		} else if (keyCode == KeyEvent.VK_ALT) {
			altKeyPressed = false;
		} else if (keyCode == KeyEvent.VK_BACK_SPACE) {
			// This was a trigger for all-off. Now turn stuff back on again.
			lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_NONE;
		} else if (keyCode == KeyEvent.VK_F12) {
			// This was a trigger for white-strobing.
			lightDJPostProcessing = LightDJPostProcessing.POST_PROCESSING_NONE;
		}
		
	}

	
	/**
	 * Automated cross-fade effects
	 */
	public void startAutoCrossfade(final double speed) {
		crossfadeSpeed = speed;
		if (crossfadeAutomator == CrossfadeAutomator.CROSSFADE_AUTO) {
			// Already cross-fading don't start a new thread!
			System.out.println("Already cross-fading!");
			return;
		}
		
		crossfadeAutomator = CrossfadeAutomator.CROSSFADE_AUTO;
		
		(new Thread(new Runnable() {
			public void run() {
				while(crossfadeAutomator == CrossfadeAutomator.CROSSFADE_AUTO) {
					automateCrossfades();
			
					// Wait a little bit
					try {
						Thread.sleep(30);
					} catch (Exception e) {
						// Couldn't sleep
						e.printStackTrace();
					}
				}
				System.out.println("Done with automatic crossfade.");
			}
		})).start();
	}
	
	
	public void automateCrossfades() {
		switch (crossfadeAutomator) {
		case CROSSFADE_MANUAL:
			// Don't do anything!
			break;
			
		case CROSSFADE_AUTO:
			// Move over to the left.
			alpha += crossfadeSpeed;
			if (alpha < 0.0) {
				alpha = 0.0;
				crossfadeAutomator = CrossfadeAutomator.CROSSFADE_MANUAL;
				paintCrossfader(false);
			} else if (alpha > 1.0) {
				alpha = 1.0;
				crossfadeAutomator = CrossfadeAutomator.CROSSFADE_MANUAL;
				paintCrossfader(false);
			} else {
				paintCrossfader(true);
			}
			break;
			
			
		}
	}
	
	
	
	
}


class RenderFrameParty extends RenderFrame {

	ColorOutput[] colorOutputs;
	
}

class RenderTask extends TimerTask {
	private VisualizationEngineParty engine;
	public RenderTask(VisualizationEngineParty eng) {engine = eng;}
	public void run() {
		try {
			engine.renderDJ();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
