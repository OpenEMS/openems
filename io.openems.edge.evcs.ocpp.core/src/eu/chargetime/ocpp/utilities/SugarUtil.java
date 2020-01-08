package eu.chargetime.ocpp.utilities;
/*
   ChargeTime.eu - Java-OCA-OCPP

   MIT License

   Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>
   Copyright (C) 2018 Mikhail Kladkevich <kladmv@ecp-share.com>

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   SOFTWARE.
*/

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class SugarUtil {

	public static String calendarToString(Calendar calendar) {
		if (calendar == null) {
			return "";
		}
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(calendar.getTime());
	}

	public static String docToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

	public static String soapMessageToString(SOAPMessage message) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			message.writeTo(out);
		} catch (SOAPException | IOException e) {
			return "";
		}
		return new String(out.toByteArray());
	}
}
