package PartyLightsController;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;

import Common.ColorOutput;
import Common.ColorOutput.OverallOutputCompression;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

/**
 * This class is responsible for outputting commands over serial to the Next Make Party Lighting System.
 * 
 * @author Steve Levine
 *
 */
public class PartyLightsController {

	// Serial port fields
	private String serialPortName;
	private int speed;
	private boolean isConnected;
	private OutputStream outStream;
	
	// Protocol information
	protected static final int SPECIAL_SYNC_BYTE = 170;
	protected static final int ACTION_EMERGENCY_LIGHTING = 254;
	protected static final int ACTION_EVERYTHING_OFF = 253;
	protected static final int ACTION_FRONT_LEDS_SAME = 252;
	protected static final int ACTION_REAR_LEDS_SAME = 251;
	protected static final int ACTION_SET_ALL_UVS = 250;
	protected static final int ACTION_SET_ALL_WHITES = 249;
	protected static final int ACTION_STROBE_WHITE = 248;
	protected static final int ACTION_FRONT_PANELS_SAME = 247;
	protected static final int ACTION_REAR_PANELS_SAME = 246;	
	protected static final int ACTION_STROBE_UV = 245;	

	
	protected static final int NUM_RGB_FRONT_PANELS = 6;
	protected static final int NUM_RGB_REAR_PANELS = 6;
	protected static final int NUM_UV_STROBE_BOARDS = 7;
	
	protected static final int LENGTH_FRONT_RGB_INDIV_PACKET = 14;
	protected static final int LENGTH_REAR_RGB_INDIV_PACKET = 14;
	protected static final int LENGTH_FRONT_LEDS_SAME_PACKET = 5;
	protected static final int LENGTH_REAR_LEDS_SAME_PACKET = 5;
	protected static final int LENGTH_FRONT_PANELS_SAME_PACKET = 14;
	protected static final int LENGTH_REAR_PANELS_SAME_PACKET = 14;
	
	protected static final int LENGTH_UVWHITE_INDIV_PACKET = 4;
	protected static final int LENGTH_WHITE_STROBE_PACKET = 2;
	protected static final int LENGTH_UV_STROBE_PACKET = 2;
	protected static final int LENGTH_ALL_OFF_PACKET = 2;
	protected static final int LENGTH_EMERGENCY_LIGHTING_PACKET = 2;
	protected static final int LENGTH_WHITE_SET_ALL_PACKET = 3;
	protected static final int LENGTH_UV_SET_ALL_PACKET = 3;
	
	protected static final int START_REAR_PANEL_INDEX = 8;
	protected static final int START_UVWHITE_PANEL_INDEX = 16;
	
	public static double volume = 1.0;
	
	public PartyLightsController() {
		// Set some defaults
		this.serialPortName = "/dev/ttyUSB0";
		this.speed = 38400;
		isConnected = false;
		outStream = null;
		
		// Attempt to connect
		try {
			connect();
		} catch (Exception e) {
			System.out.println("Error: Couldn't connect to Party Lighting System!");
			//e.printStackTrace();
		}
	}
	
	/**
	 * Attempts to connect to the serial port, returning true on success.
	 * If there's an error, it is thrown.
	 */
	protected void connect() throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
		if (portIdentifier.isCurrentlyOwned()) {
			throw new RuntimeException("Error: The serial port " + serialPortName + " is already owned!");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
				
				
				//serialPort.setLowLatency();
				
				
				outStream = serialPort.getOutputStream();
				isConnected = true;
				System.out.println("Serial successfully connected...");
			} else {
				throw new RuntimeException("Got a non-serial port, but only serial ports supported!");
			}
		}
		
		
		
		
	}
	
	/**
	 * Writes data to the port
	 */
	protected void write(byte[] data) throws IOException {
		if (isConnected) {
			outStream.write(data);
			outStream.flush();
		}
	}

	/**
	 * Write color output data!
	 */
	public void visualize(ColorOutput colorOutput) {
		
		// Apply any overall compression!
		switch(colorOutput.overallOutputCompression) {
		case OVERALL_COMPRESSION_EMERGENCY_LIGHTING:
			sendEmergencyLighting();
			break;
			
		case OVERALL_COMPRESSION_ALL_OFF:
			sendAllOff();
			break;
			
		case OVERALL_COMPRESSION_WHITE_STROBE:
			sendWhiteStrobe();
			break;
			
		case OVERALL_COMPRESSION_UV_STROBE:
			sendUVStrobe();
			break;
			
		case OVERALL_COMPRESSION_NONE:
			// There's no overall compression; so send commands to the RGB fronts, RGB rears, and the strobes.
			// Fronts
			switch(colorOutput.rgbFrontColorOutputCompression) {
			case RGB_FRONT_COMPRESSION_LEDS_SAME:
				sendFrontLEDSame(colorOutput);
				break;
				
			case RGB_FRONT_COMPRESSION_PANELS_SAME:
				sendFrontPanelSame(colorOutput);
				break;
				
			case RGB_FRONT_COMPRESSION_DIFF:
				sendFrontRGBPanelDiff(colorOutput);
				break;
			}
			
			//Rears
			switch(colorOutput.rgbRearColorOutputCompression) {
			case RGB_REAR_COMPRESSION_LEDS_SAME:
				sendRearLEDSame(colorOutput);
				break;
				
			case RGB_REAR_COMPRESSION_PANELS_SAME:
				sendRearPanelSame(colorOutput);
				break;
				
			case RGB_REAR_COMPRESSION_DIFF:
				sendRearRGBPanelDiff(colorOutput);
				break;
			}
			
			
			// Send UV/Whites
			switch(colorOutput.uvWhiteColorOutputCompression) {
			case UVWHITE_COMPRESSION_WHITE_AND_UV_SAME:
				sendAllWhitesSame(colorOutput);
				sendAllUVSame(colorOutput);
				break;
				
			case UVWHITE_COMPRESSION_WHITE_AND_UV_DIFF:
				sendWhiteUVPanelsDiff(colorOutput);
				break;
				
			}
			
			// Wait a bit to let the board catch up - other wise we're slamming it with data!
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				
			}
			break;
		
		}
			
	}
	
	protected void sendFrontRGBPanelDiff(ColorOutput colorOutput) {
		byte[] data = new byte[NUM_RGB_FRONT_PANELS * LENGTH_FRONT_RGB_INDIV_PACKET];
		// For now don't implement any compression to send the smallest command.
		int cursor = 0;
		for(int board = 0; board < NUM_RGB_FRONT_PANELS; board++) {
			
			Color c;
			
			data[cursor] = (byte) SPECIAL_SYNC_BYTE;
			data[cursor + 1] = (byte) board;
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 0)];
			data[cursor + 2] = limit(c.getRed());
			data[cursor + 3] = limit(c.getGreen());
			data[cursor + 4] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 1)];
			data[cursor + 5] = limit(c.getRed());
			data[cursor + 6] = limit(c.getGreen());
			data[cursor + 7] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 2)];
			data[cursor + 8] = limit(c.getRed());
			data[cursor + 9] = limit(c.getGreen());
			data[cursor + 10] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 3)];
			data[cursor + 11] = limit(c.getRed());
			data[cursor + 12] = limit(c.getGreen());
			data[cursor + 13] = limit(c.getBlue());

			
			cursor += LENGTH_FRONT_RGB_INDIV_PACKET;
			
		}
		
		debugPrint(data);
		try {
			write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void sendFrontLEDSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_FRONT_LEDS_SAME_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_FRONT_LEDS_SAME;
		output[2] = (byte) limit(colorOutput.rgbLights[0].getRed());
		output[3] = (byte) limit(colorOutput.rgbLights[0].getGreen());
		output[4] = (byte) limit(colorOutput.rgbLights[0].getBlue());
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendFrontPanelSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_FRONT_PANELS_SAME_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_FRONT_PANELS_SAME;
		output[2] = (byte) limit(colorOutput.rgbLights[0].getRed());
		output[3] = (byte) limit(colorOutput.rgbLights[0].getGreen());
		output[4] = (byte) limit(colorOutput.rgbLights[0].getBlue());
		output[5] = (byte) limit(colorOutput.rgbLights[1].getRed());
		output[6] = (byte) limit(colorOutput.rgbLights[1].getGreen());
		output[7] = (byte) limit(colorOutput.rgbLights[1].getBlue());
		output[8] = (byte) limit(colorOutput.rgbLights[2].getRed());
		output[9] = (byte) limit(colorOutput.rgbLights[2].getGreen());
		output[10] = (byte) limit(colorOutput.rgbLights[2].getBlue());
		output[11] = (byte) limit(colorOutput.rgbLights[3].getRed());
		output[12] = (byte) limit(colorOutput.rgbLights[3].getGreen());
		output[13] = (byte) limit(colorOutput.rgbLights[3].getBlue());
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendRearRGBPanelDiff(ColorOutput colorOutput) {
		byte[] data = new byte[NUM_RGB_REAR_PANELS * LENGTH_REAR_RGB_INDIV_PACKET];
		// For now don't implement any compression to send the smallest command.
		int cursor = 0;
		for(int board = 0; board < NUM_RGB_REAR_PANELS; board++) {
			
			Color c;
			
			data[cursor] = (byte) SPECIAL_SYNC_BYTE;
			data[cursor + 1] = (byte) (board + START_REAR_PANEL_INDEX);
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 0)];
			data[cursor + 2] = limit(c.getRed());
			data[cursor + 3] = limit(c.getGreen());
			data[cursor + 4] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 1)];
			data[cursor + 5] = limit(c.getRed());
			data[cursor + 6] = limit(c.getGreen());
			data[cursor + 7] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 2)];
			data[cursor + 8] = limit(c.getRed());
			data[cursor + 9] = limit(c.getGreen());
			data[cursor + 10] = limit(c.getBlue());
			
			c = colorOutput.rgbLights[getFrontLightIndexFromBoard(board, 3)];
			data[cursor + 11] = limit(c.getRed());
			data[cursor + 12] = limit(c.getGreen());
			data[cursor + 13] = limit(c.getBlue());

			
			cursor += LENGTH_REAR_RGB_INDIV_PACKET;
			
		}
		
		debugPrint(data);
		try {
			write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void sendRearLEDSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_REAR_LEDS_SAME_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_REAR_LEDS_SAME;
		output[2] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4].getRed());
		output[3] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4].getGreen());
		output[4] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4].getBlue());
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void sendRearPanelSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_REAR_PANELS_SAME_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_REAR_PANELS_SAME;
		output[2] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 0].getRed());
		output[3] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 0].getGreen());
		output[4] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 0].getBlue());
		output[5] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 1].getRed());
		output[6] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 1].getGreen());
		output[7] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 1].getBlue());
		output[8] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 2].getRed());
		output[9] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 2].getGreen());
		output[10] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 2].getBlue());
		output[11] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 3].getRed());
		output[12] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 3].getGreen());
		output[13] = (byte) limit(colorOutput.rgbLights[START_REAR_PANEL_INDEX*4 + 3].getBlue());
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendAllWhitesSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_WHITE_SET_ALL_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_SET_ALL_WHITES;
		output[2] = (byte) limit((int) (256.0*colorOutput.whiteLights[0]));
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void sendAllUVSame(ColorOutput colorOutput) {
		byte[] output = new byte[LENGTH_UV_SET_ALL_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_SET_ALL_UVS;
		output[2] = (byte) limit((int) (256.0*colorOutput.uvLights[0]));
		
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	protected void sendWhiteUVPanelsDiff(ColorOutput colorOutput) {
		byte[] data = new byte[NUM_UV_STROBE_BOARDS * LENGTH_UVWHITE_INDIV_PACKET];
		// For now don't implement any compression to send the smallest command.
		int cursor = 0;
		for(int board = 0; board < NUM_UV_STROBE_BOARDS; board++) {
			
			
			data[cursor] = (byte) SPECIAL_SYNC_BYTE;
			data[cursor + 1] = (byte) (board + START_UVWHITE_PANEL_INDEX);
			data[cursor + 2] = (byte) limit((int) (256.0 * colorOutput.uvLights[board]));
			data[cursor + 3] = (byte) limit((int) (256.0 * colorOutput.whiteLights[board]));
			
			cursor += LENGTH_REAR_RGB_INDIV_PACKET;
			
		}
		
		debugPrint(data);
		try {
			write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	protected void sendWhiteStrobe() {
		byte[] output = new byte[LENGTH_WHITE_STROBE_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_STROBE_WHITE;
		
		// Try to write to output
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendUVStrobe() {
		byte[] output = new byte[LENGTH_UV_STROBE_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_STROBE_UV;
		
		// Try to write to output
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendEmergencyLighting() {
		byte[] output = new byte[LENGTH_EMERGENCY_LIGHTING_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_EMERGENCY_LIGHTING;
		
		// Try to write to output
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendAllOff() {
		byte[] output = new byte[LENGTH_ALL_OFF_PACKET];
		output[0] = (byte) SPECIAL_SYNC_BYTE;
		output[1] = (byte) ACTION_EVERYTHING_OFF;
		
		// Try to write to output
		debugPrint(output);
		try {
			write(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void debugPrint(byte[] data) {
		System.out.println("*****");
		for(int i = 0; i < data.length; i++) {
			System.out.println(convertSignedByteToUnsignedInt(data[i]));
		}
	}
	
	protected int convertSignedByteToUnsignedInt(byte b) {
		if (b >= 0) {
			return (int) b;
		} else {
			return ((int) b) + 256;
		}
	}
	
	
	protected byte limit(int a) {
		int val = (int) (volume * a);
		
		if (val == 170) {
			return (byte) 171 ;
		} else {
			return (byte) val;
		}
	}
	
	protected int getFrontLightIndexFromBoard(int board, int light) {
		return 4*board + light;
	}
	
	protected int getRearLightIndexFromBoard(int board, int light) {
		return 4*board + light + START_REAR_PANEL_INDEX * 4;
	}
	
	
}
