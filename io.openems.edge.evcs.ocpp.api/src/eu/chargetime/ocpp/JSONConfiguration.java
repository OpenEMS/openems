package eu.chargetime.ocpp;

/*
 ubitricity.com - Java-OCA-OCPP

 MIT License

 Copyright (C) 2018 Evgeny Pakhomov <eugene.pakhomov@ubitricity.com>

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

import java.util.HashMap;

public class JSONConfiguration {

	public static final String TCP_NO_DELAY_PARAMETER = "TCP_NO_DELAY";
	public static final String REUSE_ADDR_PARAMETER = "REUSE_ADDR";
	public static final String PROXY_PARAMETER = "PROXY";
	public static final String PING_INTERVAL_PARAMETER = "PING_INTERVAL";

	private final HashMap<String, Object> parameters = new HashMap<>();

	private JSONConfiguration() {
	}

	public static JSONConfiguration get() {
		return new JSONConfiguration();
	}

	public <T> JSONConfiguration setParameter(String name, T value) {
		parameters.put(name, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T getParameter(String name) {
		// noinspection unchecked
		return (T) parameters.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T getParameter(String name, T defaultValue) {
		// noinspection unchecked
		T value = (T) parameters.get(name);
		return value != null ? value : defaultValue;
	}
}
