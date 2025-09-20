// CHECKSTYLE:OFF

/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000,2001 Maxim Integrated Products, All Rights Reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.utils.OWPath;

/**
 * The tag parser parses tagging information.
 */
public class TAGParser {

	/**
	 * Construct the tag parser.
	 *
	 * @param adapter What port adapter will serve the devices created.
	 */
	public TAGParser(DSPortAdapter adapter) {
		this.parser = XML.createSAXParser();
		this.handler = new TAGHandler();

		try {
			this.handler.setAdapter(adapter);
		} catch (Exception e) {
			System.out.println(e);
		}

		this.parser.setDocumentHandler(this.handler);
		this.parser.setErrorHandler(this.handler);
	}

	/**
	 * Returns the vector of TaggedDevice objects described in the TAG file.
	 *
	 * @param in The XML document to parse.
	 *
	 * @return Vector of TaggedDevice objects.
	 * @throws SAXException If a parse error occurs parsing <var>in</var>.
	 * @throws IOException  If an I/O error occurs while reading <var>in</var>.
	 */
	public Vector<TaggedDevice> parse(InputStream in) throws SAXException, IOException {
		var insource = new InputSource(in);

		this.parser.parse(insource);

		return this.handler.getTaggedDeviceList();
	}

	/**
	 * Returns the vector of Branch TaggedDevice objects described in the TAG file.
	 * The XML should already be parsed before calling this method.
	 *
	 * @return Vector of Branch TaggedDevice objects.
	 */
	public Vector<TaggedDevice> getBranches() {

		return this.handler.getAllBranches();
	}

	/**
	 * Returns the vector of OWPath objects discovered through parsing the XML file.
	 * The XML file should already be parsed before calling this method.
	 *
	 * @return Vector of OWPath objects.
	 */
	public Vector<OWPath> getOWPaths() {

		return this.handler.getAllBranchPaths();
	}

	/** Field parser */
	private final SAXParser parser;

	/** Field handler */
	private final TAGHandler handler;
}
// CHECKSTYLE:ON
