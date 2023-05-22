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
 * 
 * @author Nico Ketzer
 * @version 1.1
 *
 */

public class GenericHttpWorker extends AbstractCycleWorker {
    
    /*Define default Values*/
    private String[] urls_to_call = {};
    private String[] response_to_call = {};
    private String[] add_external_task = {};
    private boolean com_error = false;
    private int com_error_counter = 0;
    private int timeout = 1000;
    private OpenemsNamedException last_error;
    
    
    /**
     * Generates a new GenericHttp Worker
     * 
     * @param urls_to_call Pass an URL´s with or without a Method in a String[]. The default method is "GET". If you want to use a different one, pass [Method]:[Your URL] instead of the blank URL. Example: "GET:http://192.168.1.1/status".
     * @param timeout Defines the timeout of the individual requests
     */
    public GenericHttpWorker(String[] urls_to_call, int timeout) {
	int elements = urls_to_call.length;
	this.timeout = timeout;
	/*Re-Create Array with right Size and Fill them*/
	this.urls_to_call = new String[elements]; 
	this.urls_to_call = urls_to_call;		
	this.response_to_call = new String[elements];
    }

    /**
     * Sends a Request to the given URL via the provided Method
     * 
     * @param given_url The URL to Call
     * @param requestMethod The Method which is used to Call the URL
     * @return The Response from the HTTP-Request
     * @throws OpenemsNamedException on Error
     */
    
    private String send_req(String given_url, String requestMethod) throws OpenemsNamedException {
	try {
	    var url = new URL(given_url);
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
	    throw new OpenemsException("Error while reading from Device. Response code: " + status + ". " + body);
	} catch (OpenemsNamedException | IOException e) {
	    OpenemsNamedException tmp_e = new OpenemsException("Unable to read from Device. " + e.getClass().getSimpleName() + ": "
		    + e.getMessage() + " URL:" + given_url);
	    this.last_error = tmp_e;
	    throw tmp_e;
	}
    }
    
    /**
     * Parses a given URL (e. g. GET:http://123.123.123.123/state or http://231.231.231.231/state)
     * @param url 
     * @return String[] where the first String is the Method (e.g. "GET") and the Second String is the URL (e.g. http://12.123.123.123/state). If no Method is given in the Standard-Method is "GET"
     * @throws OpenemsNamedException
     */
    
    private String[] parse_url(String url) throws OpenemsNamedException{
	String tmp = url.substring(0,3);
	if(tmp == "http") {
	    /*URL has no method. Default-Method "GET" will be used.*/
	    return new String[] {"GET",url};
	}else{
	    if(url.contains("http")) {
		/*It´s a valid URL*/		
		String[] tmp2 = url.split(":");	
		if(tmp2.length >= 3) {   
		    String method = tmp2[0];
		    /*The longest HTTP-Method is Connect or Options with 7 Chars. Die shortest is Get or Put with 3 Chars*/    
		    if(method.length() >= 3 && method.length() <= 7 && method != "http") {
			/*It´s a valid Method */
			String new_url = "";		    
			for(int i = 1; i<tmp2.length; i++) {
			    new_url = new_url + ":" + tmp2[i];
			}
			/*New Url begin´s with : so remove it*/
			new_url = new_url.substring(1, new_url.length());		
			return new String[] {method,new_url};			
		    }else {
			/*The given URL is not created after the Scheme [METHODE]:[URL] 
			 * Default-Method "GET" is used. URL is started at "http"
			 */
			String[] tmp3 = url.split("http");			    
			String new_url = "";			    
			/*A for loop is used to be able to create URL´s with more than one "http" in it*/
			for(int i = 1; i<tmp3.length; i++) {
			    new_url = new_url + "http" + tmp3[i];
			}			    
			return new String[] {"GET", new_url};
		    }
		}else {
		    /*The given URL is not created after the Scheme [METHODE]:[URL] 
		     * Default-Method "GET" is used. URL is started at "http"
		     */
		    String[] tmp3 = url.split("http");		    
		    String new_url = "";	    
		    /*A for loop is used to be able to create URL´s with more than one "http" in it*/
		    for(int i = 1; i<tmp3.length; i++) {
			new_url = new_url + "http" + tmp3[i];
		    }	    
		    return new String[] {"GET",new_url};
		}		
	    }else {
		/*Es handelt sich um keinen validen URL*/
		throw new OpenemsException("Error while parsing URL. No valid URL was passed");
	    }
	}
    }
    
    /**
     * Add an External URL which get´s called async in the forever() Loop of this worker
     * @param url_to_call The URL that should be called async. The Standard-Request-Method for this Function is "GET". If you want to use another Request Method pass your URL in the Scheme [HTTP_REQUEST_METHOD]:[YOUR_URL]
     */
    
    public void push_task(String url_to_call) {
	
	String tmp_arr = this.add_external_task.toString();
	/*Make sure a asyn Task dont get called multible times*/
	if(!tmp_arr.contains(url_to_call)) {
        	String[] tmp = this.add_external_task;
        	this.add_external_task = new String[tmp.length + 1];
        	for(int i = 0; i<tmp.length; i++) {
        	    this.add_external_task[i] = tmp[i];
        	}
        	this.add_external_task[tmp.length] = url_to_call;
	}
    }
    
    /**
     * Delete a Task which was created by "push_task"
     * @param element The Element which should get deleted
     */
    
    private void delete_task(String element) {
	String tmp_element = this.add_external_task.toString();
	if (tmp_element.contains(element)) {
	    int index = -1;
	    for (int i = 0; i < this.urls_to_call.length; i++) {
		String tmp_element2 = this.urls_to_call[i];
		if (tmp_element2 == element) {
		    index = i;
		    break;
		}
	    }
	    if (index != -1) {
		String[] tmp_array = new String[this.add_external_task.length - 1];
		for(int i = 0, k = 0; i<this.add_external_task.length; i++) {
		    if(i == index) {
			continue;
		    }
		    tmp_array[k] = this.add_external_task[i];
		    k++;
		}
		this.add_external_task = tmp_array;
	    }
	} 
    }
    
    /**
     * Pass a external URL to the existing worker and await the Response (blocking). For adding a URL to the Worker Non-Blocking use push_task.
     * 
     * @param url The URL which should be called. If you want to use another Request Method pass your URL in the Scheme [HTTP_REQUEST_METHOD]:[YOUR_URL] or pass a third Argument with your HTTP-Request-Method as a String.
     * @param expected_output The Output you would expect.
     * @return If the fetched Output is equal to your passed expected_output the function will return true else false is returned. If the expected_output Parameter was set to an empty String "" the Function will return true if the call was successful.
     */
    
    public boolean send_external_url(String url, String expected_output) {
	try {
	    String[] parsed_url = this.parse_url(url);
	    return send_external_url(parsed_url[1], expected_output, parsed_url[0]);
	} catch (OpenemsNamedException e) {
	    this.last_error = e;
	    return false;
	}
    }
    
    /**
     * Pass a external URL to the existing worker and await the Response (blocking). For adding a URL to the Worker Non-Blocking use push_task.
     * 
     * @param url The URL which should be called
     * @param expected_output The Output you would expect.
     * @param requestMethod Pass any HTTP-Request Method which should be used for your call
     * @return If the fetched Output is equal to your passed expected_output the function will return true else false is returned. If the expected_output Parameter was set to an empty String "" the Function will return true if the call was successful.
     */

    public boolean send_external_url(String url, String expected_output, String requestMethod) {
	try {
	    int tmp_timeout = this.timeout;
	    this.timeout = 500; /* Lower the Timeout to not Block the handleEvent() to long */
	    String response = this.send_req(url, requestMethod);
	    this.timeout = tmp_timeout; /*reset*/
	    if (expected_output == "") {
		return true;
	    } else {
		if (response.contains(expected_output) && response.length() == expected_output.length()) {
		    return true;
		} else {
		    return false;
		}
	    }
	} catch (OpenemsNamedException e) {
	    this.last_error = e;
	    return false;
	}
    }
    
    /**
     * Fetch the local Variable "com_error"
     * @return Boolean Value of com_error. If "true" the Worker has a Com-Error to at least one passed URL
     */
    
    public boolean get_com_error() {
	return this.com_error;
    }

    /**
     * Fetch the last Error that happened.
     * @return If a Error happened in the past this Function will Return the last one. If no error happened so far a Dummy Error (OpenemsNamedException) of the Error-Type OpenemsError.Generic will be generated and returned.
     */
    
    public OpenemsNamedException get_last_error() {
	if (this.last_error != null) {
	    return this.last_error;
	} else {
	    return new OpenemsNamedException(OpenemsError.GENERIC, this); /*Generate a Dummy-Error*/
	}
    }
    
    /**
     * Fetch the last Value of the passed URL (URL = Passed URLs at the given Index)
     * @param element Index of the URL in the URLs-String-Array
     * @return If the Worker has a Com-Error (see: get_com_error()) a String "_com_error_" is returned. If the Element does not exist a String "_undefined_" is returned. If the Element exists but has not received a value since the worker started a String "_no_value_" is returned. If the Element exists and has a value than the value is returned
     */

    public String get_last_by_id(int element) {
	if (this.com_error) {
	    return "_com_error_";
	}
	if (element <= this.response_to_call.length && element > -1) {
	    if (this.response_to_call[element] != null) {
		return this.response_to_call[element];
	    } else {
		return "_no_value_";
	    }
	} else {
	    return "_undefined_";
	}
    }
    
    /**
     * Fetch the last Value of the passed URL 
     * @param element The URL where the last Value should be fetched.
     * @return If the Worker has a Com-Error (see: get_com_error()) a String "_com_error_" is returned. If the Element does not exist (the passed Element is not part of the passed URLs in the constructor) a String "_undefined_" is returned. If the Element exists but has not received a value since the worker started a String "_no_value_" is returned. If the Element exists and has a value than the value is returned
     */

    public String get_last(String element) {
	if (this.com_error) {
	    return "_com_error_";
	}
	String tmp_element = this.urls_to_call.toString();
	if (tmp_element.contains(element)) {
	    int index = -1;
	    for (int i = 0; i < this.urls_to_call.length; i++) {
		String tmp_element2 = this.urls_to_call[i];
		if (tmp_element2 == element) {
		    index = i;
		    break;
		}
	    }
	    // Catch
	    if (index == -1) {
		return "_undefined_";
	    } else {
		return this.get_last_by_id(index);
	    }
	} else {
	    return "_undefined_";
	}
    }

    /**
     * Internal Function where the URLs given in urls_to_call are called in a loop. Also Tasks provided by add_external_task (via the function push_task) are called (at max. 5 per Loop)
     */
    
    @Override
    protected void forever() throws Throwable {
	try {
	    for (int i = 0; i < this.urls_to_call.length; i++) {
		/*Com-Error Handling to not Spam requests to a Device */
		if (this.com_error) {
		    if (this.com_error_counter > 10) {
			this.com_error = false;
		    } else {
			this.com_error_counter++;
			break;
		    }
		}
		try {
		    String url = this.urls_to_call[i];
		    
		    String[] parsed_url = this.parse_url(url);
		    
		    String response = this.send_req(parsed_url[1],parsed_url[0]);
		    this.response_to_call[i] = response;
		    /*Work of external Tasks*/
		    if(this.add_external_task.length >= 1) {
			/*Just five external Tasks per Run*/
			int k = (this.add_external_task.length <= 4 ? this.add_external_task.length : 4);
			for(int j=0; j<k; j++) {
			    if(this.add_external_task[j] != null) {
				String url2 = this.add_external_task[j];
				
				String[] parsed_url2 = this.parse_url(url2);
				
			    	this.send_external_url(parsed_url2[1], "", parsed_url2[0]);
			    	this.delete_task(url2);
			    }else {
				break;
			    }
			}
		    }
		} catch (OpenemsNamedException e) {
		    this.last_error = e;
		    throw e;
		}
	    }
	} catch (OpenemsNamedException e) {
	    this.last_error = e;
	    this.com_error = true;
	}

    }
}