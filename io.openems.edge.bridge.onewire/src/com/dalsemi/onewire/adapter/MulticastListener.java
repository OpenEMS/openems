// CHECKSTYLE:OFF
/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Maxim Integrated Products, All Rights Reserved.
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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Generic Mulitcast broadcast listener. Listens for a specific message and, in
 * response, gives the specified reply. Used by NetAdapterHost for automatic
 * discovery of host components for the network-based DSPortAdapter.
 *
 * @author SH
 * @version 1.00
 */
public class MulticastListener implements Runnable {
	/** boolean flag to turn on debug messages */
	private static final boolean DEBUG = false;

	/** timeout for socket receive */
	private static final int timeoutInSeconds = 3;

	/** multicast socket to receive datagram packets on */
	private MulticastSocket socket = null;
	/** the message we're expecting to receive on the multicast socket */
	private final byte[] expectedMessage;
	/** the message we should reply with when we get the expected message */
	private final byte[] returnMessage;

	/** boolean to stop the thread from listening for messages */
	private volatile boolean listenerStopped = false;
	/** boolean to check if the thread is still running */
	private volatile boolean listenerRunning = false;

	/**
	 * Creates a multicast listener on the specified multicast port, bound to the
	 * specified multicast group. Whenever the byte[] pattern specified by
	 * "expectedMessage" is received, the byte[] pattern specified by
	 * "returnMessage" is sent to the sender of the "expected message".
	 *
	 * @param multicastPort   Port to bind this listener to.
	 * @param multicastGroup  Group to bind this listener to.
	 * @param expectedMessage the message to look for
	 * @param returnMessage   the message to reply with
	 */
	@SuppressWarnings("deprecation")
	public MulticastListener(int multicastPort, String multicastGroup, byte[] expectedMessage, byte[] returnMessage)
			throws IOException, UnknownHostException {
		this.expectedMessage = expectedMessage;
		this.returnMessage = returnMessage;

		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
		if (DEBUG) {
			System.out.println("DEBUG: Creating Multicast Listener");
			System.out.println("DEBUG:    Multicast port: " + multicastPort);
			System.out.println("DEBUG:    Multicast group: " + multicastGroup);
		}
		// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

		// create multicast socket
		this.socket = new MulticastSocket(multicastPort);
		// set timeout at 3 seconds
		this.socket.setSoTimeout(timeoutInSeconds * 1000);
		// join the multicast group
		var group = InetAddress.getByName(multicastGroup);
		this.socket.joinGroup(group);
	}

	/**
	 * Run method waits for Multicast packets with the specified contents and
	 * replies with the specified message.
	 */
	@Override
	public void run() {
		var receiveBuffer = new byte[this.expectedMessage.length];

		this.listenerRunning = true;
		while (!this.listenerStopped) {
			try {
				// packet for receiving messages
				var inPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				if (DEBUG) {
					System.out.println("DEBUG: waiting for multicast packet");
				}
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				// blocks for message until timeout occurs
				this.socket.receive(inPacket);

				// check to see if the received data matches the expected message
				var length = inPacket.getLength();

				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
				if (DEBUG) {
					System.out.println("DEBUG: packet.length=" + length);
					System.out.println("DEBUG: expecting=" + this.expectedMessage.length);
				}
				// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

				if (length == this.expectedMessage.length) {
					var dataMatch = true;
					for (var i = 0; dataMatch && i < length; i++) {
						dataMatch = this.expectedMessage[i] == receiveBuffer[i];
					}
					// check to see if we received the expected message
					if (dataMatch) {
						// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
						if (DEBUG) {
							System.out.println("DEBUG: packet match, replying");
						}
						// \\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
						// packet for sending messages
						var outPacket = new DatagramPacket(this.returnMessage, this.returnMessage.length,
								inPacket.getAddress(), inPacket.getPort());
						// send return message
						this.socket.send(outPacket);
					}
				}
			} catch (IOException ioe) {
				/* drain */}
		}
		this.listenerRunning = false;
	}

	/**
	 * Waits for datagram listener to finish, with a timeout.
	 */
	public void stopListener() {
		this.listenerStopped = true;
		var i = 0;
		var timeout = timeoutInSeconds * 100;
		while (this.listenerRunning && i++ < timeout) {
			try {
				Thread.sleep(10);
			} catch (Exception e) {

			}
		}
	}
}
// CHECKSTYLE:ON
