package io.openems.edge.io.generic_http_worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractCycleWorker;



public class generic_http_worker extends AbstractCycleWorker {

	private String[] urls_to_call = {};
	private String[] response_to_call = {};
	private boolean com_error = false; //First value is false
	private int com_error_counter = 0; //To Reset the error
	private int timeout = 1000; //Default Value
	private Exception last_error;

	public generic_http_worker(String[] urls_to_call, int timeout) {
		int elements = urls_to_call.length;
		this.urls_to_call = new String[elements]; //Redefine Array-Size
		this.urls_to_call = urls_to_call;
		this.response_to_call = new String[elements]; //Redefine Array-Size
		this.timeout = timeout;
	}
	
	//Used Functions
	private String send_req(String given_url) throws OpenemsNamedException {
		try {
			var url = new URL(given_url);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
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
				// Parse response to JSON
				return body;
			}
			throw new OpenemsException("Error while reading from Device. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			this.last_error = e;
			throw new OpenemsException(
					"Unable to read from Device. " + e.getClass().getSimpleName() + ": " + e.getMessage() + " URL:" + given_url);
		}
	}
	
	public boolean send_external_url(String url,String expected_output) {
		try {
			String response = this.send_req(url);
			if(expected_output == "") {
				return true;
			}else{
				if(response.contains(expected_output) && response.length() == expected_output.length()) {
					return true;
				}else {
					return false;
				}
			}
		} catch (OpenemsNamedException e) {
			this.last_error = e;
			return false;
		}
	}
	public Exception get_last_error() {
		if(this.last_error != null) {
			return this.last_error;
		} else {
			return new Exception();
		}
	}
	
	public String get_last_by_id(int element) {
		if(this.com_error) {
			return "_com_error_";
		}
		if(element <= this.response_to_call.length && element > -1) {
			if(this.response_to_call[element] != null) {
				return this.response_to_call[element];
			}else {
				return "_no_value_";
			}
		} else {
			return "_undefined_";
		}
	}
	public String get_last(String element) {
		if(this.com_error) {
			return "_com_error_";
		}
		String tmp_element = this.urls_to_call.toString();
		if(tmp_element.contains(element)) {
			int index = -1;
			for(int i=0; i<this.urls_to_call.length; i++) {
				String tmp_element2 = this.urls_to_call[i];
				if(tmp_element2 == element) {
					index = i;
					break;
				}
			}
			//Catch
			if(index == -1) {
				return "_undefined_";
			}else {
				return this.get_last_by_id(index);
			}
		}else {
			return "_undefined_";
		}
	}
	
	//Worker Part

	@Override
	protected void forever() throws Throwable {
		try {
			for(int i=0; i<this.urls_to_call.length; i++) {
				//Com-Error Handling to not Spam requests
				if(this.com_error) {
					if(this.com_error_counter > 500) {
						this.com_error = false;
					}else {
						this.com_error_counter++;
						break;
					}
				}
				//Normal Process
				try {
					String url = this.urls_to_call[i]; //Get URL
					
					String response = this.send_req(url); //Get Result
					this.response_to_call[i] = response; //Write Result
				} catch (OpenemsNamedException e) {
					this.last_error = e;
					throw e;
				}
			}
		} catch (OpenemsNamedException e) {
			this.last_error = e;
			//Failed so for this Part a Communication Error is there
			this.com_error = true;
		}

	}
}