package io.openems.common.worker;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * Defines a generic HTTP-Worker Thread.
 *
 * <p>
 * The logic of the Worker is based on the "AbstractCycleWorker". As soon as an
 * Instance is created all given URL´s will be called based on the Logic of the
 * AbstractCycleWorker.
 * 
 * <p>
 * Error Monitoring is possible via multiple functions. E. g. with
 * "getLastError()" the last available Error get´s passed to the Parent.
 * 
 * @author Nico Ketzer
 * @version 1.2
 *
 */

public class GenericHttpWorker extends AbstractCycleWorker {

	/* Define default Values */
	private String[] urlsToCall;
	private String[] responseToCall;
	private String[] addExternalTask;
	private boolean comError = false;
	private int comErrorCounter = 0;
	private int timeout;
	private OpenemsNamedException lastError;

	/**
	 * Generates a new GenericHttp Worker.
	 * 
	 * @param urlsToCall Pass an URL´s with or without a Method in a String[]. The
	 *                   default method is "GET". If you want to use a different
	 *                   one, pass [Method]:[Your URL] instead of the blank URL.
	 *                   Example: "GET:http://192.168.1.1/status".
	 * @param timeout    Defines the timeout of the individual requests
	 */
	public GenericHttpWorker(String[] urlsToCall, int timeout) {
		int elements = urlsToCall.length;
		this.timeout = timeout;
		/* Re-Create Array with right Size and Fill them */
		this.urlsToCall = new String[elements];
		this.urlsToCall = urlsToCall;
		this.responseToCall = new String[elements];
	}

	/**
	 * Sends a Request to the given URL via the provided Method.
	 * 
	 * @param givenUrl      The URL to Call
	 * @param requestMethod The Method which is used to Call the URL
	 * @return The Response from the HTTP-Request
	 * @throws OpenemsNamedException on Error
	 */

	private String sendReq(String givenUrl, String requestMethod) throws OpenemsNamedException {
		try {
			var url = new URL(givenUrl);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(requestMethod);
			con.setConnectTimeout(this.timeout);
			con.setReadTimeout(this.timeout);
			var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				return body;
			}
			throw new OpenemsException("Error while reading from Device. Response code: " + status + ": " + body);
		} catch (OpenemsNamedException | IOException e) {
			OpenemsNamedException tmpError = new OpenemsException("Unable to read from Device. "
					+ e.getClass().getSimpleName() + ": " + e.getMessage() + " URL:" + givenUrl);
			this.lastError = tmpError;
			throw tmpError;
		}
	}

	/**
	 * Parses a given URL (e. g. GET:http://123.123.123.123/state or
	 * http://231.231.231.231/state)
	 * 
	 * @param url Normal Url as String
	 * @return String[] where the first String is the Method (e.g. "GET") and the
	 *         Second String is the URL (e.g. http://12.123.123.123/state). If no
	 *         Method is given in the Standard-Method is "GET".
	 * @throws OpenemsNamedException on Error
	 */

	private String[] parseUrl(String url) throws OpenemsNamedException {
		String tmp = url.substring(0, 3);
		if (tmp == "http") {
			/* URL has no method. Default-Method "GET" will be used. */
			return new String[] { "GET", url };
		} else {
			if (url.contains("http")) {
				/* It´s a valid URL */
				String[] tmp2 = url.split(":");
				if (tmp2.length >= 3) {
					String method = tmp2[0];
					/*
					 * The longest HTTP-Method is Connect or Options with 7 Chars. The shortest is
					 * Get or Put with 3 Chars
					 */
					if (method.length() >= 3 && method.length() <= 7 && method != "http") {
						/* It´s a valid Method */
						String newUrl = "";
						for (int i = 1; i < tmp2.length; i++) {
							newUrl = newUrl + ":" + tmp2[i];
						}
						/* New Url begin´s with : so remove it */
						newUrl = newUrl.substring(1, newUrl.length());
						return new String[] { method, newUrl };
					} else {
						/*
						 * The given URL is not created after the Scheme [METHOD]:[URL] Default-Method
						 * "GET" is used. URL is started at "http"
						 */
						String[] tmp3 = url.split("http");
						String newUrl = "";
						/*
						 * A for loop is used to be able to create URL´s with more than one "http" in it
						 */
						for (int i = 1; i < tmp3.length; i++) {
							newUrl = newUrl + "http" + tmp3[i];
						}
						return new String[] { "GET", newUrl };
					}
				} else {
					/*
					 * The given URL is not created after the Scheme [METHOD]:[URL] Default-Method
					 * "GET" is used. URL is started at "http"
					 */
					String[] tmp3 = url.split("http");
					String newUrl = "";
					/*
					 * A for loop is used to be able to create URL´s with more than one "http" in it
					 */
					for (int i = 1; i < tmp3.length; i++) {
						newUrl = newUrl + "http" + tmp3[i];
					}
					return new String[] { "GET", newUrl };
				}
			} else {
				/* It´s no valid URL */
				throw new OpenemsException("Error while parsing URL. No valid URL was passed");
			}
		}
	}

	/**
	 * Add an External URL which get´s called async in the forever() Loop of this
	 * worker.
	 * 
	 * @param urlToCall The URL that should be called async. The
	 *                  Standard-Request-Method for this Function is "GET". If you
	 *                  want to use another Request Method pass your URL in the
	 *                  Scheme [HTTP_REQUEST_METHOD]:[YOUR_URL]
	 */

	public void pushTask(String urlToCall) {
		/* Make sure a asyn Task dont get called multiple times */
		boolean urlExists = false;
		for (int i = 0; i < this.addExternalTask.length; i++) {
			String testElement = this.addExternalTask[i];
			if (testElement.trim().toLowerCase() == urlToCall.trim()
					.toLowerCase()) { /*
										 * Trim is needed to remove additional Spaces, toLowerCase ensures that
										 * http://123 and HTTP://123 are seen as the same URL
										 */
				urlExists = true;
				break;
			}
		}
		if (!urlExists) {
			String[] tmp = this.addExternalTask;
			this.addExternalTask = new String[tmp.length + 1];
			for (int i = 0; i < tmp.length; i++) {
				this.addExternalTask[i] = tmp[i];
			}
			this.addExternalTask[tmp.length] = urlToCall;
		}
	}

	/**
	 * Delete a Task which was created by "pushTask".
	 * 
	 * @param element The Element which should get deleted
	 */

	private void deleteTask(String element) {
		if (this.addExternalTask.length < 1) {
			return;
		}
		int index = -1;
		for (int i = 0; i < this.addExternalTask.length; i++) {
			String tmpEl = this.addExternalTask[i];
			if (element == tmpEl) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return;
		}
		String[] tmpArray = new String[this.addExternalTask.length - 1];
		for (int i = 0, k = 0; i < this.addExternalTask.length; i++) {
			if (i == index) {
				continue;
			}
			tmpArray[k] = this.addExternalTask[i];
			k++;
		}
		this.addExternalTask = tmpArray;
	}

	/**
	 * Pass a external URL to the existing worker and await the Response (blocking).
	 * For adding a URL to the Worker Non-Blocking use pushTask.
	 * 
	 * @param url             The URL which should be called. If you want to use
	 *                        another Request Method pass your URL in the Scheme
	 *                        [HTTP_REQUEST_METHOD]:[YOUR_URL] or pass a third
	 *                        Argument with your HTTP-Request-Method as a String.
	 * @param expectedOutput The Output you would expect.
	 * @return If the fetched Output is equal to your passed expected_output the
	 *         function will return true else false is returned. If the
	 *         expected_output Parameter was set to an empty String "" the Function
	 *         will return true if the call was successful.
	 */

	public boolean sendExternalUrl(String url, String expectedOutput) {
		try {
			String[] parsedUrl = this.parseUrl(url);
			return this.sendExternalUrl(parsedUrl[1], expectedOutput, parsedUrl[0]);
		} catch (OpenemsNamedException e) {
			this.lastError = e;
			return false;
		}
	}

	/**
	 * Pass a external URL to the existing worker and await the Response (blocking).
	 * For adding a URL to the Worker Non-Blocking use pushTask.
	 * 
	 * @param url             The URL which should be called
	 * @param expectedOutput The Output you would expect.
	 * @param requestMethod   Pass any HTTP-Request Method which should be used for
	 *                        your call
	 * @return If the fetched Output is equal to your passed expected_output the
	 *         function will return true else false is returned. If the
	 *         expected_output Parameter was set to an empty String "" the Function
	 *         will return true if the call was successful.
	 */

	public boolean sendExternalUrl(String url, String expectedOutput, String requestMethod) {
		try {
			int tmpTimeout = this.timeout;
			this.timeout = 500; /* Lower the Timeout to not Block the handleEvent() to long */
			String response = this.sendReq(url, requestMethod);
			this.timeout = tmpTimeout; /* reset */
			if (expectedOutput == "") {
				return true;
			} else {
				if (response.contains(expectedOutput) && response.length() == expectedOutput.length()) {
					return true;
				} else {
					return false;
				}
			}
		} catch (OpenemsNamedException e) {
			this.lastError = e;
			return false;
		}
	}

	/**
	 * Fetch the local Variable "comError".
	 * 
	 * @return Boolean Value of comError. If "true" the Worker has a Com-Error to at
	 *         least one passed URL
	 */

	public boolean getComError() {
		return this.comError;
	}

	/**
	 * Fetch the last Error that happened.
	 * 
	 * @return If a Error happened in the past this Function will Return the last
	 *         one. If no error happened so far a Dummy Error
	 *         (OpenemsNamedException) of the Error-Type OpenemsError.Generic will
	 *         be generated and returned.
	 */

	public OpenemsNamedException getLastError() {
		if (this.lastError != null) {
			return this.lastError;
		} else {
			return new OpenemsNamedException(OpenemsError.GENERIC, this); /* Generate a Dummy-Error */
		}
	}

	/**
	 * Fetch the last Value of the passed URL (URL = Passed URLs at the given Index).
	 * 
	 * @param element Index of the URL in the URLs-String-Array
	 * @return If the Worker has a Com-Error (see: get_comError()) a String
	 *         "_comError_" is returned. If the Element does not exist a String
	 *         "_undefined_" is returned. If the Element exists but has not received
	 *         a value since the worker started a String "_no_value_" is returned.
	 *         If the Element exists and has a value than the value is returned
	 */

	public String getLastById(int element) {
		if (this.comError) {
			return "_comError_";
		}
		if (element <= this.responseToCall.length && element > -1) {
			if (this.responseToCall[element] != null) {
				return this.responseToCall[element];
			} else {
				return "_no_value_";
			}
		} else {
			return "_undefined_";
		}
	}

	/**
	 * Fetch the last Value of the passed URL.
	 * 
	 * @param element The URL where the last Value should be fetched.
	 * @return If the Worker has a Com-Error (see: get_comError()) a String
	 *         "_comError_" is returned. If the Element does not exist (the passed
	 *         Element is not part of the passed URLs in the constructor) a String
	 *         "_undefined_" is returned. If the Element exists but has not received
	 *         a value since the worker started a String "_no_value_" is returned.
	 *         If the Element exists and has a value than the value is returned
	 */

	public String getLast(String element) {
		if (this.comError) {
			return "_comError_";
		}
		String tmpElement = this.urlsToCall.toString();
		if (tmpElement.contains(element)) {
			int index = -1;
			for (int i = 0; i < this.urlsToCall.length; i++) {
				String tmpElement2 = this.urlsToCall[i];
				if (tmpElement2 == element) {
					index = i;
					break;
				}
			}
			// Catch
			if (index == -1) {
				return "_undefined_";
			} else {
				return this.getLastById(index);
			}
		} else {
			return "_undefined_";
		}
	}

	/**
	 * Internal Function where the URLs given in urlsToCall are called in a loop.
	 * Also Tasks provided by addExternalTask (via the function pushTask) are called
	 * (at max. 5 per Loop)
	 */

	@Override
	protected void forever() throws Throwable {
		try {
			for (int i = 0; i < this.urlsToCall.length; i++) {
				/* Com-Error Handling to not Spam requests to a Device */
				if (this.comError) {
					if (this.comErrorCounter > 10) {
						this.comError = false;
					} else {
						this.comErrorCounter++;
						break;
					}
				}
				try {
					String url = this.urlsToCall[i];
					String[] parsedUrl = this.parseUrl(url);
					String response = this.sendReq(parsedUrl[1], parsedUrl[0]);
					this.responseToCall[i] = response;
					/* Work of external Tasks */
					if (this.addExternalTask.length >= 1) {
						/* Just five external Tasks per Run */
						int k = (this.addExternalTask.length <= 4 ? this.addExternalTask.length : 4);
						for (int j = 0; j < k; j++) {
							/*
							 * We have to use the Value of this.addExternalTask[0] every time. E.g. the
							 * String[] holds two Elements After one Run the this.deleteTask() function is
							 * called. This Function modifies the this.addExternalTask an Reduce the Number
							 * of Elements to one. In this case before the this.deleteTask() function is
							 * called there is a Index 1 after this only Index 0 exists. However, if j is
							 * now used as a variable for the index, j is increased to 1 after the first run
							 * of the for-Loop. The index 1 no longer exists on the next run and therefore a
							 * Worker Error is thrown ("ArrayIndexOutOfBoundsException").
							 */
							if (this.addExternalTask[0] != null) {
								String url2 = this.addExternalTask[0];
								String[] parsedUrl2 = this.parseUrl(url2);
								this.sendReq(parsedUrl2[1], parsedUrl2[0]);
								this.deleteTask(url2);
							} else {
								break;
							}
						}

					}
				} catch (OpenemsNamedException e) {
					this.lastError = e;
					throw e;
				}
			}
		} catch (OpenemsNamedException e) {
			this.lastError = e;
			this.comError = true;
		}
	}
}