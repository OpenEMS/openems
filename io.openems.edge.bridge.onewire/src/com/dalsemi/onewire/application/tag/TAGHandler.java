// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 1999-2001 Maxim Integrated Products, All Rights Reserved.
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

package com.dalsemi.onewire.application.tag;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.AttributeList;
import org.xml.sax.DocumentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.utils.OWPath;

/**
 * SAX parser handler that handles XML 1-wire tags.
 */
@SuppressWarnings({ "deprecation", "unchecked" })
class TAGHandler implements ErrorHandler, DocumentHandler {

	/**
	 * Method setDocumentLocator
	 *
	 *
	 * @param locator
	 *
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
	}

	/**
	 * Method startDocument
	 *
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void startDocument() throws SAXException {

		// Instantiate deviceList and clusterStack
		this.deviceList = new Vector<>();
		this.clusterStack = new Stack<>(); // keep track of clusters
		this.branchStack = new Stack<>(); // keep track of current branches
		this.branchVector = new Vector<>(); // keep track of every branch
		this.branchVectors = new Vector<>(); // keep a vector of cloned branchStacks
		// to use in making the OWPaths Vector
		this.branchPaths = new Vector<>(); // keep track of OWPaths
	}

	/**
	 * Method endDocument
	 *
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void endDocument() throws SAXException {

		// Iterate through deviceList and make all the
		// OWPaths from the TaggedDevice's vector of Branches.
		TaggedDevice device;
		OWPath branchPath;
		Vector<TaggedDevice> singleBranchVector;

		for (var i = 0; i < this.deviceList.size(); i++) {
			device = this.deviceList.elementAt(i);

			device.setOWPath(this.adapter, device.getBranches());
		}

		// Now, iterate through branchVectors and make all the
		// OWPaths for the Vector of OWPaths

		for (var i = 0; i < this.branchVectors.size(); i++) {
			singleBranchVector = this.branchVectors.elementAt(i);
			branchPath = new OWPath(this.adapter);
			for (var j = 0; j < singleBranchVector.size(); j++) {
				device = singleBranchVector.elementAt(i);

				branchPath.add(device.getDeviceContainer(), device.getChannel());
			}
			this.branchPaths.addElement(branchPath);
		}
	}

	/**
	 * Method startElement
	 *
	 *
	 * @param name
	 * @param atts
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void startElement(String name, AttributeList atts) throws SAXException {
		this.currentElement = name; // save current element name

		var attributeAddr = "null";
		var attributeType = "null";
		String className;
		var i = 0;

		// Parse cluster elements here, keeping track of them with a Stack.
		if (name.toUpperCase().equals("CLUSTER")) {
			for (i = 0; i < atts.getLength(); i++) {
				if (atts.getName(i).toUpperCase().equals("NAME")) {
					this.clusterStack.push(atts.getValue(i));
				}
			}
		}

		// Parse sensor, actuator, and branch elements here
		if (name.toUpperCase().equals("SENSOR") || name.toUpperCase().equals("ACTUATOR")
				|| name.toUpperCase().equals("BRANCH")) {
			for (i = 0; i < atts.getLength(); i++) {
				var attName = atts.getName(i);

				if (attName.toUpperCase().equals("ADDR")) {
					attributeAddr = atts.getValue(i);
				}

				if (attName.toUpperCase().equals("TYPE")) {
					attributeType = atts.getValue(i);
				}
			}

			// instantiate the appropriate object based on tag type
			// (i.e., "Contact", "Switch", etc). The only exception
			// is of type "branch"
			if (name.toUpperCase().equals("BRANCH")) {
				attributeType = "branch";
				this.currentDevice = new TaggedDevice(); // instantiates object
			} else {

				// first, find tag type to instantiate by CLASS NAME!
				// if the tag has a "." in it, it indicates the package
				// path was included in the tag type.
				if (attributeType.indexOf(".") > 0) {
					className = attributeType;
				} else {
					className = "com.dalsemi.onewire.application.tag." + attributeType;
				}

				// instantiate the appropriate object based on tag type (i.e., "Contact",
				// "Switch", etc)
				try {
					Class<?> genericClass = Class.forName(className);

					this.currentDevice = (TaggedDevice) genericClass.newInstance();
				} catch (Exception e) {
					throw new RuntimeException(
							"Can't load 1-Wire Tag Type class (" + className + "): " + e.getMessage());
				}
			}

			// set the members (fields) of the TaggedDevice object
			this.currentDevice.setDeviceContainer(this.adapter, attributeAddr);
			this.currentDevice.setDeviceType(attributeType);
			this.currentDevice.setClusterName(this.getClusterStackAsString(this.clusterStack, "/"));
			this.currentDevice.setBranches((Vector<TaggedDevice>) this.branchStack.clone()); // copy branchStack to it's
																								// related
			// object in TaggedDevice

			// ** do branch specific work here: **
			if (name.equals("branch")) {

				// push the not-quite-finished branch TaggedDevice on the branch stack.
				this.branchStack.push(this.currentDevice);

				// put currentDevice in the branch vector that holds all branch objects.
				this.branchVector.addElement(this.currentDevice);

				// put currentDevice in deviceList (if it is of type "branch", of course)
				this.deviceList.addElement(this.currentDevice);
			}
		}
	}

	/**
	 * Method endElement
	 *
	 *
	 * @param name
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void endElement(String name) throws SAXException {
		if (name.toUpperCase().equals("SENSOR") || name.toUpperCase().equals("ACTUATOR")) {

			// System.out.println(name + " element finished");
			this.deviceList.addElement(this.currentDevice);

			this.currentDevice = null;
		}

		if (name.toUpperCase().equals("BRANCH")) {
			this.branchVectors.addElement((Vector<TaggedDevice>) this.branchStack.clone()); // adds a snapshot of
			// the stack to
			// make OWPaths later

			this.branchStack.pop();

			this.currentDevice = null; // !!! not sure if this is needed.
		}

		if (name.toUpperCase().equals("CLUSTER")) {
			this.clusterStack.pop();
		}
	}

	/**
	 * Method characters
	 *
	 *
	 * @param ch
	 * @param start
	 * @param length
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (this.currentElement.toUpperCase().equals("LABEL")) {
			if (this.currentDevice == null) {

				// This means we have a branch instead of a sensor or actuator
				// so, set label accordingly
				try {
					this.currentDevice = this.branchStack.peek();

					this.currentDevice.setLabel(new String(ch, start, length));

					this.currentDevice = null;
				} catch (EmptyStackException ese) {

					// don't do anything yet.
				}
			} else {
				this.currentDevice.setLabel(new String(ch, start, length));
			}

			// System.out.println("This device's label is: " + currentDevice.label);
		}

		if (this.currentElement.toUpperCase().equals("CHANNEL")) {
			if (this.currentDevice == null) {

				// This means we have a branch instead of a sensor or actuator
				// so, set channel accordingly
				try {
					this.currentDevice = this.branchStack.peek();

					this.currentDevice.setChannelFromString(new String(ch, start, length));

					this.currentDevice = null;
				} catch (EmptyStackException ese) {

					// don't do anything yet.
				}
			} else {
				this.currentDevice.setChannelFromString(new String(ch, start, length));
			}
		}

		if (this.currentElement.toUpperCase().equals("MAX")) {
			this.currentDevice.max = new String(ch, start, length);

			// System.out.println("This device's max message is: " + currentDevice.max);
		}

		if (this.currentElement.toUpperCase().equals("MIN")) {
			this.currentDevice.min = new String(ch, start, length);

			// System.out.println("This device's min message is: " + currentDevice.min);
		}

		if (this.currentElement.toUpperCase().equals("INIT")) {
			this.currentDevice.setInit(new String(ch, start, length));

			// System.out.println("This device's init message is: " + currentDevice.init);
		}
	}

	/**
	 * Method ignorableWhitespace
	 *
	 *
	 * @param ch
	 * @param start
	 * @param length
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
	}

	/**
	 * Method processingInstruction
	 *
	 *
	 * @param target
	 * @param data
	 *
	 * @throws SAXException
	 *
	 */
	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	/**
	 * Method getTaggedDeviceList
	 *
	 *
	 * @return
	 *
	 */
	public Vector<TaggedDevice> getTaggedDeviceList() {
		return this.deviceList;
	}

	/**
	 * Method setAdapter
	 *
	 *
	 * @param adapter
	 *
	 * @throws com.dalsemi.onewire.OneWireException
	 *
	 */
	public void setAdapter(DSPortAdapter adapter) throws com.dalsemi.onewire.OneWireException {
		this.adapter = adapter;
	}

	/**
	 * Method fatalError
	 *
	 *
	 * @param exception
	 *
	 * @throws SAXParseException
	 *
	 */
	@Override
	public void fatalError(SAXParseException exception) throws SAXParseException {
		System.err.println(exception);

		throw exception;
	}

	/**
	 * Method error
	 *
	 *
	 * @param exception
	 *
	 * @throws SAXParseException
	 *
	 */
	@Override
	public void error(SAXParseException exception) throws SAXParseException {
		System.err.println(exception);

		throw exception;
	}

	/**
	 * Method warning
	 *
	 *
	 * @param exception
	 *
	 */
	@Override
	public void warning(SAXParseException exception) {
		System.err.println(exception);
	}

	/**
	 * Method getAllBranches
	 *
	 *
	 * @param no parameters
	 *
	 * @return Vector of all TaggedDevices of type "branch".
	 *
	 */
	public Vector<TaggedDevice> getAllBranches() {

		return this.branchVector;

	}

	/**
	 * Method getAllBranchPaths
	 *
	 *
	 * @param no parameters
	 *
	 * @return Vector of all possible OWPaths.
	 *
	 */
	public Vector<OWPath> getAllBranchPaths() {

		return this.branchPaths;

	}

	/**
	 * Method getClusterStackAsString
	 *
	 *
	 * @param clusters
	 * @param separator
	 *
	 * @return
	 *
	 */
	private String getClusterStackAsString(Stack<String> clusters, String separator) {
		var returnString = new StringBuilder();

		for (var j = 0; j < clusters.size(); j++) {
			returnString.append(separator).append(clusters.elementAt(j));
		}

		return returnString.toString();
	}

	/** Field adapter */
	private DSPortAdapter adapter;

	/** Field currentElement */
	private String currentElement;

	/** Field currentDevice */
	private TaggedDevice currentDevice;

	/** Field deviceList */
	private Vector<TaggedDevice> deviceList;

	/** Field clusterStack */
	private Stack<String> clusterStack;

	/** Field branchStack */
	private Stack<TaggedDevice> branchStack; // keep a stack of current branches

	/** Field branchVector */
	private Vector<TaggedDevice> branchVector; // to hold all branches

	/** Field branchVectors */
	private Vector<Vector<TaggedDevice>> branchVectors; // to hold all branches to eventually
	// make OWPaths

	/** Field branchPaths */
	private Vector<OWPath> branchPaths; // to hold all OWPaths to 1-Wire devices.

}
// CHECKSTYLE:ON
