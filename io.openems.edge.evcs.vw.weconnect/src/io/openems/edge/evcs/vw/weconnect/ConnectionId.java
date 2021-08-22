package io.openems.edge.evcs.vw.weconnect;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConnectionId {
	
	private final Logger log = LoggerFactory.getLogger(LoginParameter.class);
	
	private static final int TOKEN_VALID_TIME_MINUTES = 50;
	private List<VehicleId> vehicles;
	private LoginParameter loginParameter;
	private HttpClient httpClient;
	
	private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private long TIMEOUT = 30;
	private String clientId = "a24fba63-34b3-4d43-b181-942111e6bda8@apps_vw-dilab_com";

	private boolean loginRunning = true;

	public ConnectionId(String username, String password) {
		this.loginParameter = new LoginParameter(username,password);
		vehicles = new ArrayList<>();
		httpClient = new HttpClient(new SslContextFactory(true));
	}

	public boolean isVehicleConnected() {
		return getConnectedVehicle().isPresent();
	}

	public boolean isVehicleCharging() {
		return getChargingVehicle().isPresent();
	}

	public void startCharingConnectedVehicle() {
		getConnectedVehicle().ifPresent( vehicle -> {
			String vin = vehicle.getVin();
			
			try {
				startCharging(vin);
			} catch (InterruptedException | TimeoutException | ExecutionException e) {
				log.error("error start charging vehicle with vin="+vin);
				e.printStackTrace();
			}
		});
	}
	
	public void stopCharingConnectedVehicle() {
		getChargingVehicle().ifPresent(vehicle -> {
			String vin = vehicle.getVin();
			
			try {
				stopCharging(vin);
			} catch (InterruptedException | TimeoutException | ExecutionException e) {
				log.error("error stop charging vehicle with vin="+vin);
				e.printStackTrace();
			}
		});
	}
	
	public int getConnectedVehicleSoc() {
		return getConnectedVehicle().map(vehicle -> vehicle.getSoc()).orElse(0);
	}
	
	public int getVehicleSoc() {
		return this.vehicles.stream().findFirst().map(vehicle -> vehicle.getSoc()).orElse(0);
	}
	
	public void login() {
		this.loginRunning  = true;
		
		try {
			httpClient.start();
			clearCookies();
			vehicles.clear();
			
			receiveLoginUrl();
	    	
	    	requestLoginForm();
	    	
	    	sendUserName();
	    	
	    	sendPasswordAndGetLoginParameter();
	    	
	    	getTokens();
	    	
	    	log.error("logged in");
	    	
	    	getVehicles();
	    	
	    	updateVehicles();
	    	
		} catch (Exception e) {
			log.error("Exception during login");
			e.printStackTrace();
		}
		
		loginRunning = false;
	}
	
	public boolean isLoginRunning() {
		return loginRunning;
	}

	public void updateVehicles() {
		try {
			if(loginParameter.getAgeTokens().plusMinutes(TOKEN_VALID_TIME_MINUTES).isBeforeNow()) {
				this.refreshToken();
			}
		}
		catch(Exception e) {
			log.error("Error refresh token");
			e.printStackTrace();
			log.error("Try relogin");
			login();
		}
		
		if(vehicles.isEmpty()) {
			log.error("Vehicles empty. This should not happen. Refetching vehicles");
			try {
				getVehicles();
			} catch (InterruptedException | TimeoutException | ExecutionException e) {
				log.error("error getting vehicles");
				e.printStackTrace();
			}
		}
		
		vehicles.forEach(vehicle -> {
			try {
				vehicle.update(getIdStatus(vehicle.getVin()));
			}
			catch(Exception e) {
				log.error("Error update vehicle");
				e.printStackTrace();
				log.error("Try relogin");
				login();
			}
		});
	}

	private Optional<VehicleId> getConnectedVehicle() {
		return vehicles.stream().filter(vehicle -> vehicle.isReadyToCharge() || vehicle.isCharging()).findFirst();
	}

	private Optional<VehicleId> getChargingVehicle() {
		return vehicles.stream().filter(vehicle -> vehicle.isCharging()).findFirst();
	}

	private void refreshToken() throws InterruptedException, TimeoutException, ExecutionException {
		log.error("refresh Token");
		String url = "https://login.apps.emea.vwapps.io/refresh/v1";
		
		httpClient.setFollowRedirects(true);
		Request request = httpClient.newRequest(url);
		
		addHeaders(request,loginParameter.getCurrentRefreshHeaders());
		
		ContentResponse response = request.send();
		
		//TODO: Error handling
		
		loginParameter.updateTokens(response.getContentAsString());
	}
	
	private void clearCookies() {
		httpClient.getCookieStore().removeAll();
	}
	
	private String randomString(int length) {
        String result = "";
        int charactersLength = CHARS.length();
        for (int i = 0; i < length; i++) {
            result += CHARS.charAt((int) Math.floor(Math.random() * charactersLength));
        }
        return result;
    }
	
	private void addHeaders(Request request, Map<String,String> headerMap) {
		headerMap.forEach((k,v) -> request.header(k, v));
	}
	
	private void receiveLoginUrl() {
		String url = "https://login.apps.emea.vwapps.io/authorize?nonce=" + randomString(16) + "&redirect_uri=weconnect://authenticated";
		
		httpClient.setFollowRedirects(false);
		Request request = httpClient.newRequest(url);
		addHeaders(request, RequestHeaders.LOGIN_URL_HEADERS);
		request.timeout(TIMEOUT , TimeUnit.SECONDS);
		ContentResponse response;
		
		String login_url_standard = "https://login.apps.emea.vwapps.io/authorize?nonce=" + this.randomString(16) + "&redirect_uri=weconnect://authenticated";;
		
		try {
			response = request.send();
			
			String login_url = response.getHeaders().get(HttpHeader.LOCATION);
			
	    	if (login_url == null || login_url.isEmpty()) {
	            login_url = login_url_standard;
	        }
	    	
	    	log.error("Login URL: "+login_url);
	    	loginParameter.currentUrl = login_url;
		} catch (InterruptedException | TimeoutException | ExecutionException e) {
			log.error("Error retrieving login url");
			e.printStackTrace();
			loginParameter.currentUrl = login_url_standard;
		}
	}
	
	private void requestLoginForm() throws Exception {
		httpClient.setFollowRedirects(true);
		Request request = httpClient.newRequest(loginParameter.currentUrl);
		addHeaders(request, RequestHeaders.LOGIN_1_HEADERS);
		request.timeout(TIMEOUT , TimeUnit.SECONDS);
		
		ContentResponse response = request.send();
		
		//TODO: Fehlerbehandlung
		
		String contentAsString = response.getContentAsString();
		log.error("Request1: Content->"+contentAsString);
		String login_form_action = getFormAction(contentAsString,"emailPasswordForm");
		
		if(login_form_action == null || login_form_action.isEmpty()) {
			throw new Exception("login_form_action is null or empty");
		}
		
		loginParameter.fillCurrentParameterMapIncludingUser(contentAsString);
		loginParameter.currentUrl = "https://identity.vwgroup.io/signin-service/v1/" + this.clientId + "/login/identifier";		
	}
	
	private String getFormAction(String content, String name) {
		Document document = Jsoup.parse(content);
		
		Elements foundElements = document.select("form");
		
		String form_action = "";
		
		if(foundElements.isEmpty()) {
		    	log.error("No Login Form found");
		    	return "";
		    }
		    else {
		    	for(Element element : foundElements) {
				if(element.attr("name").equals(name)) {
					form_action = element.attr("action");
				}
			}
		}
		
		log.error("form_action:"+form_action);
		
		return form_action;
	}
	
	private void sendUserName() throws Exception {
		httpClient.setFollowRedirects(true);
		ContentResponse post1Response = performPost();
		
		String contentAsString = post1Response.getContentAsString();
		
		String credentials_formAction = getFormAction(contentAsString, "credentialsForm");
		
		if(credentials_formAction == null || credentials_formAction.isEmpty())
		{
			throw new Exception("credentials_formAction is null or empty");
		}
		
		loginParameter.fillCurrentParameterMapIncludingUserAndPassword(contentAsString);
		loginParameter.currentUrl = "https://identity.vwgroup.io/signin-service/v1/" + this.clientId + "/login/authenticate";		
	}

	private ContentResponse performPost() throws InterruptedException, TimeoutException, ExecutionException {
		Request postRequest = httpClient.POST(loginParameter.currentUrl);
		postRequest.timeout(TIMEOUT , TimeUnit.SECONDS);
		
		addHeaders(postRequest,loginParameter.getCurrentAuthHeaders());
		
		if(loginParameter.currentParameterMap != null) {
			String postData = convertMapToFormUrlEncoded(loginParameter.currentParameterMap);
			
			log.error("postData:"+postData);
			
			postRequest.content(new StringContentProvider(postData));
		}
		
		ContentResponse response = postRequest.send();
		
		log.error("postResponse Content: " + response.getContentAsString());
		
		return response;
	}
	
	private String convertMapToFormUrlEncoded(Map<String, String> parameterMap) {
		if(parameterMap.size() == 0)
			return "";
		
		StringBuffer content = new StringBuffer();
		parameterMap.forEach((k,v)->{
			try {
				content.append(URLEncoder.encode(k, StandardCharsets.UTF_8.toString()) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8.toString()) + "&");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
		return content.substring(0, content.length()-1);
	}
	
	private void sendPasswordAndGetLoginParameter() throws Exception {
		httpClient.setFollowRedirects(false);
		ContentResponse response = performPost();
		
		//TODO: Error handling
		//Test auf valide location URL
		
		String login_redirect_url = response.getHeaders().get(HttpHeader.LOCATION);
		String[] splitResults = login_redirect_url.split("&");
		
		if(splitResults.length <= 2) {
			log.error("Location:"+login_redirect_url);
			throw new Exception("No valid userid, please visit this link or logout and login in your app account:0");
		}
	
		//loginParameter.setUserId(splitResults[2].split("=")[1]);
		
		//TODO: Error handling
		response = httpClient.GET(login_redirect_url);
		
		login_redirect_url = response.getHeaders().get(HttpHeader.LOCATION);
		
		response = httpClient.GET(login_redirect_url);
		
		login_redirect_url = response.getHeaders().get(HttpHeader.LOCATION);
		
		response = httpClient.GET(login_redirect_url);
		
		login_redirect_url = response.getHeaders().get(HttpHeader.LOCATION);
		
		String[] login_split = login_redirect_url.split("&");
		
		loginParameter.setAuthorizationCode("");
		loginParameter.setAccess_token("");
		loginParameter.setIdToken("");
		loginParameter.setState("");
		
		for(String parameter : login_split) {
			String[] harray = parameter.split("=");
            if (harray[0].contains("state")) {
            	loginParameter.setState(harray[1]);
            }
            if (harray[0].contains("code")) {
            	loginParameter.setAuthorizationCode(harray[1]);
            }
            if (harray[0].contains("access_token")) {
            	loginParameter.setAccess_token(harray[1]);
            }
            if (harray[0].contains("id_token")) {
            	loginParameter.setIdToken(harray[1]);
            }
		}
	}

	private void getTokens() throws InterruptedException, TimeoutException, ExecutionException {
		String loginJsonPayload = loginParameter.getLoginJson();
		
		log.error("loginJsonPayload:"+loginJsonPayload);
		
		httpClient.setFollowRedirects(false);
		
		String login_url = "https://login.apps.emea.vwapps.io/login/v1";
		Request postRequest = httpClient.POST(login_url);
		postRequest.timeout(TIMEOUT , TimeUnit.SECONDS);
		
		addHeaders(postRequest,RequestHeaders.LOGIN_3_HEADERS);
		
		postRequest.content(new StringContentProvider(loginJsonPayload));
		
		ContentResponse response = postRequest.send();
		
		loginParameter.updateTokens(response.getContentAsString());
	}
	
	private void getVehicles() throws InterruptedException, TimeoutException, ExecutionException {
		String url = "https://mobileapi.apps.emea.vwapps.io/vehicles";	
		
		httpClient.setFollowRedirects(true);
		Request request = httpClient.newRequest(url);
		addHeaders(request,loginParameter.getCurrentRequestHeaders());
		ContentResponse response = request.send();
		
		//TODO: Error handling
		
		JsonObject jsonObject = new JsonParser().parse(response.getContentAsString()).getAsJsonObject();
		log.error("vehicles jsonObject: "+jsonObject.toString());
		
		JsonArray vehicleArray = jsonObject.get("data").getAsJsonArray();
		
		vehicleArray.forEach(vehicleJson -> {
			vehicles.add(new VehicleId(vehicleJson));
		});
	}

	private JsonObject getIdStatus(String vin) throws InterruptedException, TimeoutException, ExecutionException {
		
		String url = "https://mobileapi.apps.emea.vwapps.io/vehicles/" + vin + "/status";
		
		httpClient.setFollowRedirects(true);
		Request request = httpClient.newRequest(url);
		addHeaders(request,loginParameter.getCurrentRequestHeaders());
		
		ContentResponse response = request.send();
		
		return new JsonParser().parse(response.getContentAsString()).getAsJsonObject();
	}
	
	private void startCharging(String vin) throws InterruptedException, TimeoutException, ExecutionException {
		changeChargeState("start",vin);
	}
	
	private void stopCharging(String vin) throws InterruptedException, TimeoutException, ExecutionException {
		changeChargeState("stop",vin);
	}
	
	private void changeChargeState(String state,String vin) throws InterruptedException, TimeoutException, ExecutionException {
		String url = "https://mobileapi.apps.emea.vwapps.io/vehicles/" + vin + "/charging/" + state;
		httpClient.setFollowRedirects(true);
		Request postRequest = httpClient.POST(url);
		addHeaders(postRequest,loginParameter.getCurrentRequestHeaders());
		ContentResponse response = postRequest.send();

		log.error("postResponse Content: " + response.getContentAsString());
	}

	public void logout() {
		// TODO Auto-generated method stub
		
	}
}
