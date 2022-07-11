// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2001-2003 Maxim Integrated Products, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL MAXIM INTEGRATED PRODUCTS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Maxim Integrated Products
 * shall not be used except as stated in the Maxim Integrated Products
 * Branding Policy.
 *---------------------------------------------------------------------------
 */
package com.dalsemi.onewire.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

//import javax.comm.*;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * <p>
 * The SerialService class provides serial IO services to the USerialAdapter
 * class.
 * </p>
 *
 * @version 1.00, 1 Sep 2003
 * @author DS
 *
 */
@SuppressWarnings({ "unchecked" })
public class SerialService implements SerialPortEventListener {
	private static final boolean DEBUG = false;
	/** The serial port name of this object (e.g. COM1, /dev/ttyS0) */
	private final String comPortName;
	/** The serial port object for setting serial port parameters */
	private SerialPort serialPort = null;
	/** The input stream, for reading data from the serial port */
	private InputStream serialInputStream = null;
	/** The output stream, for writing data to the serial port */
	private OutputStream serialOutputStream = null;
	/** The hash code of the thread that currently owns this serial port */
	private int currentThreadHash = 0;
	/** temporary array, used for converting characters to bytes */
	private byte[] tempArray = new byte[128];

	/** Vector of thread hash codes that have done an open but no close */
	private final Vector<Thread> users = new Vector<>(4);

	/** Flag to indicate byte banging on read */
	private final boolean byteBang;

	/** Vector of serial port ID strings (i.e. "COM1", "COM2", etc) */
	private static final Vector<String> vPortIDs = new Vector<>(2);
	/** static list of threadIDs to the services they are using */
	private static Hashtable<Thread, SerialService> knownServices = new Hashtable<>();
	/** static list of all unique SerialService classes */
	private static Hashtable<String, SerialService> uniqueServices = new Hashtable<>();

	/**
	 * Cleans up the resources used by the thread argument. If another thread starts
	 * communicating with this port, and then goes away, there is no way to
	 * relinquish the port without stopping the process. This method allows other
	 * threads to clean up.
	 *
	 * @param thread that may have used a <code>USerialAdapter</code>
	 */
	public static void CleanUpByThread(Thread t) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.CleanUpByThread(Thread)");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		try {
			var temp = knownServices.get(t);
			if (temp == null) {
				return;
			}

			synchronized (temp) {
				if (t.hashCode() == temp.currentThreadHash) {
					// then we need to release the lock...
					temp.currentThreadHash = 0;
				}
			}

			temp.closePortByThreadID(t);
			knownServices.remove(t);
		} catch (Exception e) {
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("Exception cleaning: " + e.toString());
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}
		}
	}

	/**
	 * this constructor only for use in the static method: getSerialService(String)
	 */
	protected SerialService(String strComPort) {
		this.comPortName = strComPort;

		// check to see if need to byte-bang the reads
		var prop = com.dalsemi.onewire.OneWireAccessProvider.getProperty("onewire.serial.bytebangread");
		if (prop != null) {
			if (prop.indexOf("true") != -1) {
				this.byteBang = true;
			} else {
				this.byteBang = false;
			}
		} else {
			this.byteBang = false;
		}
	}

	public static SerialService getSerialService(String strComPort) {
		synchronized (uniqueServices) {
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.getSerialService called: strComPort=" + strComPort);
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}

			var strLowerCaseComPort = strComPort.toLowerCase();
			Object o = uniqueServices.get(strLowerCaseComPort);
			if (o != null) {
				return (SerialService) o;
			}
			var sps = new SerialService(strComPort);
			uniqueServices.put(strLowerCaseComPort, sps);
			return sps;
		}
	}

	/**
	 * SerialPortEventListener method. This just calls the notify method on this
	 * object, so that all blocking methods are kicked awake whenever a serialEvent
	 * occurs.
	 */
	@Override
	public void serialEvent(SerialPortEvent spe) {
		if (DEBUG) {
			switch (spe.getEventType()) {
			case SerialPortEvent.BI:
				System.out.println("SerialPortEvent: Break interrupt.");
				break;
			case SerialPortEvent.CD:
				System.out.println("SerialPortEvent: Carrier detect.");
				break;
			case SerialPortEvent.CTS:
				System.out.println("SerialPortEvent: Clear to send.");
				break;
			case SerialPortEvent.DATA_AVAILABLE:
				System.out.println("SerialPortEvent: Data available at the serial port.");
				break;
			case SerialPortEvent.DSR:
				System.out.println("SerialPortEvent: Data set ready.");
				break;
			case SerialPortEvent.FE:
				System.out.println("SerialPortEvent: Framing error.");
				break;
			case SerialPortEvent.OE:
				System.out.println("SerialPortEvent: Overrun error.");
				break;
			case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
				System.out.println("SerialPortEvent: Output buffer is empty.");
				break;
			case SerialPortEvent.PE:
				System.out.println("SerialPortEvent: Parity error.");
				break;
			case SerialPortEvent.RI:
				System.out.println("SerialPortEvent: Ring indicator.");
				break;
			}
			System.out.println("SerialService.SerialEvent: oldValue=" + spe.getOldValue());
			System.out.println("SerialService.SerialEvent: newValue=" + spe.getNewValue());
		}
		// try
		// {
		// serialInputStream.notifyAll();
		// }
		// catch(Exception e)
		// {
		// e.printStackTrace();
		// }
	}

	public synchronized void openPort() throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.openPort() called");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		this.openPort(null);
	}

	public synchronized void openPort(SerialPortEventListener spel) throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.openPort: Thread.currentThread()=" + Thread.currentThread());
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		// record this thread as an owner
		if (this.users.indexOf(Thread.currentThread()) == -1) {
			this.users.addElement(Thread.currentThread());
		}

		if (this.isPortOpen()) {
			return;
		}

		CommPortIdentifier port_id;
		try {
			port_id = CommPortIdentifier.getPortIdentifier(this.comPortName);
		} catch (NoSuchPortException nspe) {
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.openPort: No such port (" + this.comPortName + "). " + nspe);
			}
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			throw new IOException("No such port (" + this.comPortName + "). " + nspe);
		}

		// check if the port is currently used
		if (port_id.isCurrentlyOwned()) {
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.openPort: Port In Use (" + this.comPortName + ")");
			}
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			throw new IOException("Port In Use (" + this.comPortName + ")");
		}

		// try to acquire the port
		try {
			// get the port object
			this.serialPort = (SerialPort) port_id.open("Maxim Integrated Products", 2000);

			// serialPort.setInputBufferSize(4096);
			// serialPort.setOutputBufferSize(4096);

			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println(
						"SerialService.openPort: getInputBufferSize = " + this.serialPort.getInputBufferSize());
				System.out.println(
						"SerialService.openPort: getOutputBufferSize = " + this.serialPort.getOutputBufferSize());
			}
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

			if (spel != null) {
				this.serialPort.addEventListener(spel);
			} else {
				this.serialPort.addEventListener(this);
			}
			this.serialPort.notifyOnOutputEmpty(true);
			this.serialPort.notifyOnDataAvailable(true);

			// flow i/o
			this.serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

			this.serialInputStream = this.serialPort.getInputStream();
			this.serialOutputStream = this.serialPort.getOutputStream();
			// bug workaround
			this.serialOutputStream.write(0);

			// settings
			this.serialPort.disableReceiveFraming();
			this.serialPort.disableReceiveThreshold();
			this.serialPort.enableReceiveTimeout(1);

			// set baud rate
			this.serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			this.serialPort.setDTR(true);
			this.serialPort.setRTS(true);

			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.openPort: Port Opened (" + this.comPortName + ")");
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}
		} catch (Exception e) {
			// close the port if we have an object
			if (this.serialPort != null) {
				this.serialPort.close();
			}

			this.serialPort = null;

			throw new IOException("Could not open port (" + this.comPortName + ") :" + e);
		}
	}

	public synchronized void setNotifyOnDataAvailable(boolean notify) {
		this.serialPort.notifyOnDataAvailable(notify);
	}

	public static Enumeration<String> getSerialPortIdentifiers() {
		synchronized (vPortIDs) {
			if (vPortIDs.size() == 0) {
				Enumeration<CommPortIdentifier> e = CommPortIdentifier.getPortIdentifiers();
				while (e.hasMoreElements()) {
					var portID = e.nextElement();
					if (portID.getPortType() == CommPortIdentifier.PORT_SERIAL) {
						vPortIDs.addElement(portID.getName());
					}
				}
			}
			return vPortIDs.elements();
		}
	}

	public synchronized String getPortName() {
		return this.comPortName;
	}

	public synchronized boolean isPortOpen() {
		return this.serialPort != null;
	}

	public synchronized boolean isDTR() {
		return this.serialPort.isDTR();
	}

	public synchronized void setDTR(boolean newDTR) {
		this.serialPort.setDTR(newDTR);
	}

	public synchronized boolean isRTS() {
		return this.serialPort.isRTS();
	}

	public synchronized void setRTS(boolean newRTS) {
		this.serialPort.setRTS(newRTS);
	}

	/**
	 * Send a break on this serial port
	 *
	 * @param duration - break duration in ms
	 */
	public synchronized void sendBreak(int duration) {
		this.serialPort.sendBreak(duration);
	}

	public synchronized int getBaudRate() {
		return this.serialPort.getBaudRate();
	}

	public synchronized void setBaudRate(int baudRate) throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		try {
			// set baud rate
			this.serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.setBaudRate: baudRate=" + baudRate);
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}
		} catch (UnsupportedCommOperationException uncoe) {
			throw new IOException("Failed to set baud rate: " + uncoe);
		}

	}

	/**
	 * Close this serial port.
	 *
	 * @throws IOException - if port is in use by another application
	 */
	public synchronized void closePort() throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.closePort");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		this.closePortByThreadID(Thread.currentThread());
	}

	public synchronized void flush() throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.flush");
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		}

		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		this.serialOutputStream.flush();
		while (this.serialInputStream.available() > 0) {
			this.serialInputStream.read();
		}
	}

	// ------------------------------------------------------------------------
	// BeginExclusive/EndExclusive Mutex Methods
	// ------------------------------------------------------------------------
	/**
	 * Gets exclusive use of the 1-Wire to communicate with an iButton or 1-Wire
	 * Device. This method should be used for critical sections of code where a
	 * sequence of commands must not be interrupted by communication of threads with
	 * other iButtons, and it is permissible to sustain a delay in the special case
	 * that another thread has already been granted exclusive access and this access
	 * has not yet been relinquished.
	 * <p>
	 *
	 * @param blocking <code>true</code> if want to block waiting for exclusive
	 *                 access to the adapter
	 * @return <code>true</code> if blocking was false and a exclusive session with
	 *         the adapter was acquired
	 *
	 */
	public boolean beginExclusive(boolean blocking) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.beginExclusive(bool)");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (!blocking) {
			return this.beginExclusive();
		}
		while (!this.beginExclusive()) {
			try {
				Thread.sleep(50);
			} catch (Exception e) {
			}
		}

		return true;
	}

	/**
	 * Relinquishes exclusive control of the 1-Wire Network. This command
	 * dynamically marks the end of a critical section and should be used when
	 * exclusive control is no longer needed.
	 */
	public synchronized void endExclusive() {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.endExclusive");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		// if own then release
		if (this.currentThreadHash == Thread.currentThread().hashCode()) {
			this.currentThreadHash = 0;
		}
		knownServices.remove(Thread.currentThread());
	}

	/**
	 * Check if this thread has exclusive control of the port.
	 */
	public synchronized boolean haveExclusive() {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.haveExclusive");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		return this.currentThreadHash == Thread.currentThread().hashCode();
	}

	/**
	 * Gets exclusive use of the 1-Wire to communicate with an iButton or 1-Wire
	 * Device. This method should be used for critical sections of code where a
	 * sequence of commands must not be interrupted by communication of threads with
	 * other iButtons, and it is permissible to sustain a delay in the special case
	 * that another thread has already been granted exclusive access and this access
	 * has not yet been relinquished. This is private and non blocking
	 * <p>
	 *
	 * @return <code>true</code> a exclusive session with the adapter was acquired
	 *
	 * @throws IOException
	 */
	private synchronized boolean beginExclusive() {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.beginExclusive()");
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (this.currentThreadHash == 0) {
			// not owned so take
			this.currentThreadHash = Thread.currentThread().hashCode();
			knownServices.put(Thread.currentThread(), this);

			return true;
		}
		if (this.currentThreadHash == Thread.currentThread().hashCode()) {
			// already own
			return true;
		} else {
			// want port but don't own
			return false;
		}
	}

	/**
	 * Allows clean up port by thread
	 */
	private synchronized void closePortByThreadID(Thread t) {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.closePortByThreadID(Thread), Thread=" + t);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		}

		// added singleUser object for case where one thread creates the adapter
		// (like the main thread), and another thread closes it (like the AWT event)
		var singleUser = this.users.size() == 1;

		// remove this thread as an owner
		this.users.removeElement(t);

		// if this is the last owner then close the port
		if (singleUser || this.users.isEmpty()) {
			// if don't own a port then just return
			if (!this.isPortOpen()) {
				return;
			}

			// close the port
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println(
						"SerialService.closePortByThreadID(Thread): calling serialPort.removeEventListener() and .close()");
			}
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			this.serialPort.removeEventListener();
			this.serialPort.close();
			this.serialPort = null;
			this.serialInputStream = null;
			this.serialOutputStream = null;
		} else
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.closePortByThreadID(Thread): can't close port, owned by another thread");
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		}
	}

	// ------------------------------------------------------------------------
	// Standard InputStream methods
	// ------------------------------------------------------------------------

	public synchronized int available() throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		return this.serialInputStream.available();
	}

	public synchronized int read() throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		return this.serialInputStream.read();
	}

	public synchronized int read(byte[] buffer) throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		return this.read(buffer, 0, buffer.length);
	}

	public synchronized int read(byte[] buffer, int offset, int length) throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		return this.serialInputStream.read(buffer, offset, length);
	}

	public synchronized int readWithTimeout(byte[] buffer, int offset, int length) throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.readWithTimeout(): length=" + length);
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		}

		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		// set max_timeout to be very long
		var max_timeout = System.currentTimeMillis() + length * 20 + 800;
		var count = 0;

		// check which mode of reading
		if (this.byteBang) {
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			if (DEBUG) {
				System.out.println("SerialService.readWithTimeout(): byte-banging read");
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
			}

			int new_byte;
			do {
				new_byte = this.serialInputStream.read();

				if (new_byte != -1) {
					buffer[count + offset] = (byte) new_byte;
					count++;
				} else {
					// check for timeout
					if (System.currentTimeMillis() > max_timeout) {
						break;
					}

					// no bytes available yet so yield
					Thread.yield();

					// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
					if (DEBUG) {
						System.out.print("y");
						// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
					}
				}
			} while (length > count);
		} else {
			do {
				var get_num = this.serialInputStream.available();
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
				// if(DEBUG)
				// System.out.println("SerialService.readWithTimeout(): get_num=" + get_num + ",
				// ms left=" + (max_timeout - System.currentTimeMillis()));
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
				if (get_num > 0) {
					// check for block bigger then buffer
					if (get_num + count > length) {
						get_num = length - count;
					}

					// read the block
					count += this.serialInputStream.read(buffer, count + offset, get_num);
				} else {
					// check for timeout
					if (System.currentTimeMillis() > max_timeout) {
						length = 0;
					}
					Thread.yield();
				}
			} while (length > count);
		}

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.readWithTimeout: read " + count + " bytes");
			System.out.println("SerialService.readWithTimeout: "
					+ com.dalsemi.onewire.utils.Convert.toHexString(buffer, offset, count));
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		// return the number of characters found
		return count;
	}

	public synchronized char[] readWithTimeout(int length) throws IOException {
		var buffer = new byte[length];

		var count = this.readWithTimeout(buffer, 0, length);

		if (length != count) {
			throw new IOException(
					"readWithTimeout, timeout waiting for return bytes (wanted " + length + ", got " + count + ")");
		}

		var returnBuffer = new char[length];
		for (var i = 0; i < length; i++) {
			returnBuffer[i] = (char) (buffer[i] & 0x00FF);
		}

		return returnBuffer;
	}

	// ------------------------------------------------------------------------
	// Standard OutputStream methods
	// ------------------------------------------------------------------------
	public synchronized void write(int data) throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.write: write 1 byte");
			System.out.println("SerialService.write: " + com.dalsemi.onewire.utils.Convert.toHexString((byte) data));
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		try {
			this.serialOutputStream.write(data);
			this.serialOutputStream.flush();
		} catch (IOException e) {

			// drain IOExceptions that are 'Interrrupted' on Linux
			// convert the rest to IOExceptions
			if (!(System.getProperty("os.name").indexOf("Linux") != -1 && e.toString().indexOf("Interrupted") != -1)) {
				throw new IOException("write(char): " + e);
			}
		}
	}

	public synchronized void write(byte[] data, int offset, int length) throws IOException {
		if (!this.isPortOpen()) {
			throw new IOException("Port Not Open");
		}

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.write: write " + length + " bytes");
			System.out.println(
					"SerialService.write: " + com.dalsemi.onewire.utils.Convert.toHexString(data, offset, length));
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

		try {
			this.serialOutputStream.write(data, offset, length);
			this.serialOutputStream.flush();
		} catch (IOException e) {

			// drain IOExceptions that are 'Interrrupted' on Linux
			// convert the rest to IOExceptions
			if (!(System.getProperty("os.name").indexOf("Linux") != -1 && e.toString().indexOf("Interrupted") != -1)) {
				throw new IOException("write(char): " + e);
			}
		}
	}

	public synchronized void write(byte[] data) throws IOException {
		this.write(data, 0, data.length);
	}

	public synchronized void write(String data) throws IOException {
		var dataBytes = data.getBytes();
		this.write(dataBytes, 0, dataBytes.length);
	}

	public synchronized void write(char data) throws IOException {
		this.write((int) data);
	}

	public synchronized void write(char[] data) throws IOException {
		this.write(data, 0, data.length);
	}

	public synchronized void write(char[] data, int offset, int length) throws IOException {
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		if (DEBUG) {
			System.out.println("SerialService.write: write " + length + " chars");
			// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
		}

		if (length > this.tempArray.length) {
			this.tempArray = new byte[length];
		}

		for (var i = 0; i < length; i++) {
			this.tempArray[i] = (byte) data[i];
		}

		this.write(this.tempArray, 0, length);
	}
}
// CHECKSTYLE:ON
