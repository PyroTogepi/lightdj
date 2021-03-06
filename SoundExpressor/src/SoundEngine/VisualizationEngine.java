package SoundEngine;

import java.security.acl.LastOwnerException;
import java.util.LinkedList;
import java.util.Queue;

import javax.sound.sampled.AudioFormat;

import Signals.FFT;
import Signals.FFTEngine;

/**
 * An abstract class representing the visuals to be synchronized for music. Takes care of
 * timing parameters, and making sure that everything is synced properly.
 * 
 * @author Steve Levine 
 *
 */
public abstract class VisualizationEngine {
	
	// Audio buffers
	static protected final int BUFFER_SIZE = 2048; //256
	static protected final int BUFFER_OVERLAP = 4; // 1  // Must be a power of 2
	protected double[][] buffers;
	protected int[] bufferCursors;
	protected double[] window;
	
	// Audio format information
	protected final int FRAME_SIZE;
	protected final int BYTES_PER_SAMPLE;
	protected final long MAX_SAMPLE_VAL;
	protected final int SAMPLE_RATE;
	protected final boolean INSTANT_PLAY = true;
	
	// A rendering thread and timing queue to ensure that the visuals are rendered at the proper time as the audio
	protected VisualizationEngineRenderThread renderTimingThread;
	protected Queue<RenderFrame> timeQueue;
	protected long frameWidth;
	protected long startTime;
	protected int numBuffersRendered = 0;
	protected long videoDelayOffset;
	
	// The FFT engine
	FFTEngine fftEngine;
	
	public VisualizationEngine(AudioFormat format, double videoDelaySec) {
		
		// Remember stuff about the audio format
		// For simplicity for now, only support 16 bit samples
		if (format.getSampleSizeInBits() != 16) {
			System.out.println("Error: I currently only support 16 bit linear PCM audio data!");
			throw new RuntimeException("I only support 16 bit linear PCM audio data!");	
		} else if (format.isBigEndian()){
			System.out.println("Error: I don't feel like supporting big endian!");
			throw new RuntimeException("I don't feel like supporting big endian!");
		} else {
			// Okay
			BYTES_PER_SAMPLE = 2;
			FRAME_SIZE = format.getFrameSize();
			MAX_SAMPLE_VAL = (long) Math.pow(2, 8*BYTES_PER_SAMPLE - 1);
			SAMPLE_RATE = (int) format.getSampleRate();
			
		}
		
		// Set up sample buffers
		buffers = new double[BUFFER_OVERLAP][BUFFER_SIZE];
		bufferCursors = new int[BUFFER_OVERLAP];
		for(int i = 0; i < BUFFER_OVERLAP; i++) {
			bufferCursors[i] = i*(BUFFER_SIZE/BUFFER_OVERLAP);
		}
		createHannWindow();
		
		// Start the FFT engine
		fftEngine = new FFTEngine(BUFFER_SIZE, SAMPLE_RATE);
		
		// Load up the visualizations
		initVisualizations();	// Done by the subclass
		
		// Set up timing and rendering
		videoDelayOffset = (long) (1000000000 * videoDelaySec);
		timeQueue = new LinkedList<RenderFrame>();
		renderTimingThread = new VisualizationEngineRenderThread(this, timeQueue, INSTANT_PLAY);
		
	}
	
	/**
	 * Signifies that data will be starting soon. Also specifies a startup delay, in milliseconds.
	 */
	public void start(double startupDelay) {
		// Record when "now" is
		startTime = System.nanoTime() + (long) (startupDelay * 1000000000.0);
		frameWidth = (long) ((1.0 * BUFFER_SIZE / SAMPLE_RATE / BUFFER_OVERLAP) * 1000000000.0);
		
		//if (!INSTANT_PLAY) {
			// Start the rendering thread
			Thread renderThread = new Thread(renderTimingThread);
			renderThread.start();
			renderTimingThread.startTime = startTime;
		//}
		
	}
	
	/**
	 * Write data into the buffer, and visualize when appropriate.
	 */
	public void write(byte[] data, int offset, int length) {
		// Data is in the form of frames, which could be multi-channel audio.
		// Read in by samples
		long lValue;
		double dValue;
		
		for(int dataCursor = offset; dataCursor < length + offset; dataCursor += FRAME_SIZE) {
			
			// Read in one sample
			// BYTES_PER_SAMPLE == 2) {
			lValue = ((((short) data[dataCursor + 1]) + 128) << 8) | (((short) data[dataCursor]) + 128);
			
			// Convert this to a double value, and store it!
			dValue = (double) (lValue - MAX_SAMPLE_VAL) / (MAX_SAMPLE_VAL);
			
			// Put in in the buffers!
			for(int i = 0; i < BUFFER_OVERLAP; i++) {
			
				buffers[i][bufferCursors[i]++] = dValue;
				
				// Is it time to visualize?
				if (bufferCursors[i] == BUFFER_SIZE) {
					
					// Compute the synchronization timing parameters for the music
					long timestamp = startTime + numBuffersRendered * frameWidth + videoDelayOffset;
					
					visualize(buffers[i], timestamp, frameWidth);
					numBuffersRendered++;
					
					// Reset the ring buffer
					bufferCursors[i] = 0;
				}
			}
		}
	}
	
	protected void visualize(double[] buffer, long timestamp, long timewidth) {
		
		// Compute an FFT on a windowed buffer
		for(int i = 0; i < BUFFER_SIZE; i++) {
			buffer[i] *= window[i];
		}
		
		// Compute an FFT
		FFT fft = fftEngine.computeFFT(buffer);  //new FFT(buffer, SAMPLE_RATE);
		
		// Compute a rendering - light colors, graphs, etc.
		RenderFrame renderFrame = computeVisualsRendering(fft);
		renderFrame.timestamp = timestamp;
		renderFrame.frameTimeWidth = timewidth;
		
		if (!INSTANT_PLAY) {
			// Now, add this rendered frame to the render queue to be rendered!
			synchronized(timeQueue) {
				timeQueue.add(renderFrame);
			}
		} else {
			// Play this render frame immediately.
			synchronized(timeQueue) {
				timeQueue.clear();	// Clear any other junk - only care about the latest!
				timeQueue.add(renderFrame);
			}
		}
		
	}
	
	// Create a Hann window buffer!
	protected void createHannWindow() {
		int N = BUFFER_SIZE;
		window = new double[N];
		for(int n = 0; n < N; n++) {
			window[n] = 0.5 * (1.0 - Math.cos(2*Math.PI*n / N));
		}
	}
	
	
	// Abstract methods - to be defined by the subclass
	protected abstract void initVisualizations();
	protected abstract RenderFrame computeVisualsRendering(FFT fft);
	protected abstract void renderVisuals(RenderFrame renderFrame);

}



class VisualizationEngineRenderThread implements Runnable {
	
	private Queue<RenderFrame> timeQueue;
	private VisualizationEngine engine;
	public long startTime;
	protected final boolean INSTANT_PLAY;
	
	public VisualizationEngineRenderThread(VisualizationEngine engine, Queue<RenderFrame> timeQueue, boolean INSTANT_PLAY) {
		this.engine = engine;
		this.timeQueue = timeQueue;
		this.INSTANT_PLAY = INSTANT_PLAY;
	}
	
	// Precondition: timeQueue is in order
	public void run() {
		
		// Instant play mode - just send out the last rendered frame
		if (INSTANT_PLAY) {
			
			while (true) {
				RenderFrame renderFrame = null;
				synchronized(timeQueue) {
					if (timeQueue.size() != 0) {
						renderFrame = timeQueue.remove();
					}
				}
				
				// Have anything to render?
				if (renderFrame != null) {
					engine.renderVisuals(renderFrame);
				}
				
				// Wait a small amount of time
				try {
					Thread.sleep(0, 100000); // 100 uS	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
			}
			
		} else { // NOT INSTANT PLAY MODE
			
			// Continually see if it's time to render one of the render frames.
			RenderFrame frameToRender = null;
			while(true) {
				long now = System.nanoTime();
				
				//System.out.println("*** (" + ((double) (now - startTime) / 1000000)+ ")");
				frameToRender = null;
				synchronized(timeQueue) {// Must synchronize, since this is the consumer or a producer-consumer process
					while(!timeQueue.isEmpty()) {
						RenderFrame frame = timeQueue.peek();
						if (frame.timestamp + frame.frameTimeWidth < now) {
							// This frame occurred in the past; we're running too slowly. 
							// Just drop this rendering andi move on to the next.
							//System.out.println("D: " + ((double) (frame.timestamp + frame.frameTimeWidth - startTime) / 1000000));
							timeQueue.remove();
							
						} else if (frame.timestamp <= now) {
							// It is time to render this frame!
							frameToRender = frame;
							timeQueue.remove();
							//System.out.println("R");
							break;
							
						} else {
							// This frame must be in the future, so wait.
							//System.out.println("F");
							break;
						}
					}
				}
				
				// If necessary, render!
				if (frameToRender != null) {
					engine.renderVisuals(frameToRender);
				}
				
				// Wait a small amount of time
				try {
					Thread.sleep(0, 100000); // 100 uS	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			
		}
		

	}
	
	
}

class RenderFrame {
	// Timing information
	public long timestamp;			// When this frame should start to be rendered - absolute time in nanoseconds
	public long frameTimeWidth;		// How wide this frame is, in nanoseconds

	// Other fields to be defined by the subclass that actually contain the rendering information!
	
}