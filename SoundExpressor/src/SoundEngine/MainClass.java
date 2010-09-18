package SoundEngine;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.*;

import Utils.TimerTicToc;


public class MainClass {

	//private static final String soundFilename = "/home/steve/Desktop/01 Replay.wav";
	private static final String soundFilename = "/home/steve/Desktop/04 Troublemaker.wav";
	//private static final String soundFilename = "/home/steve/Desktop/sweep.wav";
	//private static final String soundFilename = "/home/steve/Desktop/whitenoise.wav";
	private static final int AUDIO_READ_BUFFER_SIZE = 256;
	private static final boolean USE_CAPTURED_AUDIO = true;
	
	public static void main(String[] args) {
		// Do stuff here
		System.out.println("Initializing Sound Expressor...");
		
		if (USE_CAPTURED_AUDIO) {
			System.out.println("Using captured audio...");
			runWithCapturedAudio();
			
		} else {
			System.out.println("Using audio file: " + soundFilename);
			runFromSoundFile();
		}
		
		
	}
	
	// Don't operate from live captured audio, but rather from a pre-recorded sound file.
	public static void runFromSoundFile() {
		// For now, attempt to play back from a clever music file.
		File songFile = new File(soundFilename);
		AudioInputStream audioInputStream;
		
		System.out.println("Opening audio file...");
		try {
			audioInputStream = AudioSystem.getAudioInputStream(songFile);
		} catch (UnsupportedAudioFileException e) {
			System.out.println("Invalid audio file!");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.out.println("Could not open audio file!");
			e.printStackTrace();
			return;
		}
		
		AudioFormat format = audioInputStream.getFormat();
		
		SoundVisualizer engine = new SoundVisualizer(format, true);
		// Start sending it data!
		int bytesPerFrame = format.getFrameSize();
		int bytesToRead = AUDIO_READ_BUFFER_SIZE * bytesPerFrame;
		
		System.out.println("Starting playback...");
		try {
			int numBytesRead = 0;
			byte[] audioData = new byte[bytesToRead];
			
			
			TimerTicToc timer = new TimerTicToc();
			
			while((numBytesRead = audioInputStream.read(audioData)) != -1) {
				
				// Send data!
				engine.write(audioData, 0, numBytesRead);
				
			}
			
			
		} catch (Exception e) {
			System.out.println("Error during audio playback!");
			e.printStackTrace();
			return;
		}
		System.out.println("Playback finished!");
	}

	
	// Capture live audio from the computer system, and run on this.
	public static void runWithCapturedAudio() {
		
		// Set up the desired input audio format,  using a default reasonable value
		AudioFormat format;
		format = new AudioFormat((float) 44100, 16, 2, true, false);
		
		
		TargetDataLine line;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
		if (AudioSystem.isLineSupported(info)) {
			try {
				line = (TargetDataLine) AudioSystem.getLine(info);
				line.open(format);
			} catch (Exception e) {
				System.out.println("Error: Could not open input audio line!");
				return;
			}
		} else {
			System.out.println("Unsupported audio input line!");
			return;
		}
		System.out.println("Successfully opened up audio port...");
	
		// Sound visualization engine
		SoundVisualizer engine = new SoundVisualizer(format, false);
		
		// Start reading data from it!
		int bytesPerFrame = format.getFrameSize();
		int bytesToRead = AUDIO_READ_BUFFER_SIZE * bytesPerFrame;
		System.out.println("Starting audio capture...");
		try {
			line.start();
			
			
			int numBytesRead = 0;
			byte[] audioData = new byte[bytesToRead];

			while((numBytesRead = line.read(audioData, 0, bytesToRead)) != -1) {
				
				// Send data!
				engine.write(audioData, 0, numBytesRead);
				
			}
			
			
		} catch (Exception e) {
			System.out.println("Error during audio playback!");
			e.printStackTrace();
			return;
		}
		System.out.println("Audio capture ended!");
		
	}
		
	
	
}