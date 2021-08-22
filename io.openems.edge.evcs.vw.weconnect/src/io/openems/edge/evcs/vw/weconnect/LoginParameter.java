package io.openems.edge.evcs.vw.weconnect;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.HttpHeader;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;

public class LoginParameter {
	
	private final Logger log = LoggerFactory.getLogger(LoginParameter.class);

	private String username = "";
	private String password = "";
	private DateTime ageOfTokens = null;
	
	@Expose()
	private String state = "";
	
	@Expose()
	private String id_token = "";
	
	@Expose()
	private final String redirect_uri = "weconnect://authenticated";
	
	@Expose()
	private final String region = "emea";
	
	@Expose()
	private String access_token = "";

	@Expose()
	private String authorizationCode = "";

	public String currentUrl = "";
	public Map<String,String> currentParameterMap;
	private HashMap<String,String> currentAuthHeaders;
	private String refreshToken = "";
	private HashMap<String,String> currentRequestHeaders;
	private HashMap<String,String> currentRefreshHeaders;
	
	public LoginParameter(String username, String password) {
		this.username = username;
		this.password = password;
		currentAuthHeaders = new HashMap<>(RequestHeaders.LOGIN_2_HEADERS);
	}

	public String getUser() {
		return username;
	}

	public void fillCurrentParameterMap(String content) {
		this.currentParameterMap = findElementsOfTypeHidden(content);
	}
	
	public void fillCurrentParameterMapIncludingUser(String content) {
		this.currentParameterMap = findElementsOfTypeHidden(content);
		this.currentParameterMap.put("email", username);
	}
	
	public void fillCurrentParameterMapIncludingUserAndPassword(String content) {
		this.currentParameterMap = findElementsOfTypeHidden(content);
		this.currentParameterMap.put("email", username);
		this.currentParameterMap.put("password", password);
	}
	
	public String getLoginJson() {
		Gson builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		return builder.toJson(this);
	}
	
	private void updateHeaders() {
		currentRequestHeaders = new HashMap<>(RequestHeaders.BASE_REQUEST_HEADERS);
		currentRequestHeaders.put(HttpHeader.AUTHORIZATION.asString(),"Bearer "+access_token);
		
		currentRefreshHeaders = new HashMap<>(RequestHeaders.BASE_REQUEST_HEADERS);
		currentRefreshHeaders.put(HttpHeader.AUTHORIZATION.asString(),"Bearer "+refreshToken);
	}

	private Map<String, String> findElementsOfTypeHidden(String content) {
		Document document = Jsoup.parse(content);			

		final Map<String,String> parameterMap = new HashMap<String,String>();

		Elements foundElements = document.select("input");

		if(foundElements.isEmpty()) {
		    	log.error("No element of type input found");
		    	return parameterMap;
		    }
		    else {
		    	foundElements.forEach((element)->{
				if(element.attr("type").equals("hidden")) {
					parameterMap.put(element.attr("name"), element.attr("value"));
				}
			});
	    }
		
		return parameterMap;
	}

	public void updateTokens(String jsonContent) {
		log.error("updateTokens: jsonContent="+jsonContent);
		
		JsonObject lg_urlJson = new JsonParser().parse(jsonContent).getAsJsonObject();
		
		this.access_token = lg_urlJson.get("accessToken").getAsString();
		this.refreshToken = lg_urlJson.get("refreshToken").getAsString();
		this.id_token = lg_urlJson.get("idToken").getAsString();
		
		updateHeaders();

		this.ageOfTokens = DateTime.now();
		
		log.error("accessToken:"+access_token);
		log.error("refreshToken:"+this.refreshToken);
		log.error("idToken:"+id_token);
	}

	public void setAuthorizationCode(String authorizationCode) {
		this.authorizationCode = authorizationCode;
		log.error("jwtauth_code="+authorizationCode);
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
		log.error("jwtaccess_token="+access_token);
	}

	public void setIdToken(String id_token) {
		this.id_token = id_token; 
		log.error("jwtid_token="+id_token);
	}

	public void setState(String state) {
		this.state = state;
		log.error("jwtstate="+state);
	}

	public DateTime getAgeTokens() {
		return ageOfTokens;
	}

	public Map<String, String> getCurrentRequestHeaders() {
		return currentRequestHeaders;
	}

	public Map<String, String> getCurrentRefreshHeaders() {
		return currentRefreshHeaders;
	}

	public Map<String, String> getCurrentAuthHeaders() {
		return currentAuthHeaders;
	}
}
