package PartyLightsController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialTest {

	
	// Serial port fields
	private String serialPortName;
	private int speed;
	private boolean isConnected;
	private OutputStream outStream;
	private InputStream inStream;
	
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
				serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				
				
				
				//serialPort.setLowLatency();
				
				
				outStream = serialPort.getOutputStream();
				inStream = serialPort.getInputStream();
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
		System.out.println(data.length + ", " + System.currentTimeMillis());
		if (isConnected) {
			outStream.write(data);
			outStream.flush();
		}
	}

	public void go() throws IOException {

		// Set some defaults
		serialPortName = "/dev/ttyUSB0";
		speed = 115200;
		isConnected = false;
		outStream = null;
		
		// Attempt to connect
		try {
			connect();
		} catch (Exception e) {
			System.out.println("Error: Couldn't connect to Party Lighting System!");
			//e.printStackTrace();
		}
		
		System.out.println("Sending data...");
		
		
		while(true) {
		
			byte[] data = new byte[156];
			for(int i = 0; i < data.length; i++) {
				data[i] = (byte) 0;
			}
			
			try {
				write(data);
				//Thread.sleep(10);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//inStream.skip(inStream.available());
			
			//System.out.println("Done!");
			
		}
		
		//outStream.close();
		
	}
	
	
	public static void main(String[] args) throws IOException {
		SerialTest go = new SerialTest();
		go.go();
		System.exit(0);
	}
}
