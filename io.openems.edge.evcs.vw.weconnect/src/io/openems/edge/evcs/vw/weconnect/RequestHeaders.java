package io.openems.edge.evcs.vw.weconnect;

import java.util.HashMap;

import org.eclipse.jetty.http.HttpHeader;

public class RequestHeaders {
	
	public static final HashMap<String,String> LOGIN_URL_HEADERS = new HashMap<String,String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put(HttpHeader.USER_AGENT.asString(),"Mozilla/5.0 (iPhone; CPU iPhone OS 14_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.1 Mobile/15E148 Safari/604.1");
		put(HttpHeader.ACCEPT.asString(),"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		put(HttpHeader.ACCEPT_LANGUAGE.asString(),"de-de");
		put(HttpHeader.HOST.asString(),"login.apps.emea.vwapps.io");
		
	}};
	
	public static final HashMap<String,String> LOGIN_1_HEADERS = new HashMap<String,String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put(HttpHeader.USER_AGENT.asString(),"Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.185 Mobile Safari/537.36");
		put(HttpHeader.ACCEPT.asString(),"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		put(HttpHeader.ACCEPT_LANGUAGE.asString(),"en-US,en;q=0.9");
		put(HttpHeader.ACCEPT_ENCODING.asString(),"gzip, deflate");
		put("X-Requested-With","com.volkswagen.weconnect");
		put("Upgrade-Insecure-Requests","1");
	}};

	public static final HashMap<String,String> LOGIN_2_HEADERS = new HashMap<String,String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put(HttpHeader.USER_AGENT.asString(),"Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.185 Mobile Safari/537.36");
		put(HttpHeader.ACCEPT.asString(),"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		put(HttpHeader.ACCEPT_LANGUAGE.asString(),"en-US,en;q=0.9");
		put(HttpHeader.ACCEPT_ENCODING.asString(),"gzip, deflate");
		put(HttpHeader.CONTENT_TYPE.asString(),"application/x-www-form-urlencoded");
		put("X-Requested-With","com.volkswagen.weconnect");
	}};
	
	public static final HashMap<String,String> LOGIN_3_HEADERS = new HashMap<String,String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put(HttpHeader.USER_AGENT.asString(),"WeConnect/5 CFNetwork/1206 Darwin/20.1.0");
		put(HttpHeader.ACCEPT.asString(),"*/*");
		put(HttpHeader.ACCEPT_LANGUAGE.asString(),"de-de");
		put(HttpHeader.CONTENT_TYPE.asString(),"application/json");
		put("X-NewRelic-ID","VgAEWV9QDRAEXFlRAAYPUA==");
	}};
	
	public static final HashMap<String,String> BASE_REQUEST_HEADERS = new HashMap<String,String>(){/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
		put(HttpHeader.CONTENT_TYPE.asString(),"application/json");
		put(HttpHeader.USER_AGENT.asString(),"WeConnect/5 CFNetwork/1206 Darwin/20.1.0");
		put(HttpHeader.ACCEPT.asString(),"*/*");
		put(HttpHeader.ACCEPT_LANGUAGE.asString(),"de-de");
		put("X-NewRelic-ID","VgAEWV9QDRAEXFlRAAYPUA==");
		put("Content-Version","1");
	}};
}
