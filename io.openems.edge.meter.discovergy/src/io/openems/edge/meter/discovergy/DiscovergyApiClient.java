package io.openems.edge.meter.discovergy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.utils.StreamUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.meter.discovergy.jsonrpc.Field;

/**
 * Client for the Discovergy API (<a href=
 * "https://api.discovergy.com/docs/">https://api.discovergy.com/docs/</a>)
 */
public class DiscovergyApiClient {

	private final static String CLIENT_ID = "OpenEMS";

	private final DiscovergyApi api;

	public DiscovergyApiClient(DiscovergyApi api) {
		this.api = api;
	}

	/**
	 * Returns all meters that the user has access to.
	 * 
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 * 
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getMeters() throws OpenemsNamedException {
		return JsonUtils.getAsJsonArray(this.sendGetRequest("/meters"));
	}

	/**
	 * Returns the available measurement field names for the specified meter.
	 * 
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 * 
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getFieldNames(String meterId) throws OpenemsNamedException {
		String endpoint = String.format("/field_names?meterId=%s", meterId);
		return JsonUtils.getAsJsonArray(//
				this.sendGetRequest(endpoint));
	}

	/**
	 * Returns the last measurement for the specified meter.
	 * 
	 * <p>
	 * See https://api.discovergy.com/docs/ for details.
	 * 
	 * @return the Meters as a JsonArray.
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getLastReading(String meterId, Field... fields) throws OpenemsNamedException {
		String endpoint = String.format("/last_reading?meterId=%s&fields=%s", //
				meterId, //
				Arrays.stream(fields) //
						.map(field -> field.getName()) //
						.collect(Collectors.joining(",")));
		return JsonUtils.getAsJsonObject(//
				this.sendGetRequest(endpoint));
	}

	/**
	 * Sends a get request to the Discovergy API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonElement sendGetRequest(String endpoint) throws OpenemsNamedException {
		try {
			OAuthRequest request = this.createRequest(Verb.GET, endpoint);
			Response response = this.executeRequest(request, 200);
			return JsonUtils.parse(response.getBody());
		} catch (InterruptedException | ExecutionException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Discovergy API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private OAuthRequest createRequest(Verb verb, String endpoint)
			throws InterruptedException, ExecutionException, IOException {
		return new OAuthRequest(verb, this.api.getBaseAddress() + endpoint);
	}

	private Response executeRequest(OAuthRequest request, int expectedStatusCode)
			throws InterruptedException, ExecutionException, IOException, OpenemsNamedException {
		Response response = this.executeRequest(request);
		if (response.getCode() != expectedStatusCode) {
			response.getBody();
			throw new RuntimeException("Status code is not " + expectedStatusCode + ": " + response);
		}
		return response;
	}

	private Response executeRequest(OAuthRequest request)
			throws InterruptedException, ExecutionException, IOException, OpenemsNamedException {
		OAuth10aService authenticationService = this.getAuthenticationService();
		OAuth1AccessToken accessToken = this.getAccessToken();
		authenticationService.signRequest(accessToken, request);
		return authenticationService.execute(request);
	}

	private OAuth1AccessToken _accessToken = null;

	private OAuth1AccessToken getAccessToken()
			throws IOException, InterruptedException, ExecutionException, OpenemsNamedException {
		if (this._accessToken != null) {
			return this._accessToken;
		}
		OAuth10aService authenticationService = this.getAuthenticationService();
		OAuth1RequestToken requestToken = authenticationService.getRequestToken();
		String authorizationURL = authenticationService.getAuthorizationUrl(requestToken);
		String verifier = authorize(authorizationURL);
		this._accessToken = authenticationService.getAccessToken(requestToken, verifier);
		return this._accessToken;
	}

	private OAuth10aService _authenticationService = null;

	private OAuth10aService getAuthenticationService() throws IOException, OpenemsNamedException {
		if (this._authenticationService != null) {
			return this._authenticationService;
		}
		JsonObject consumerTokenEntries = this.getConsumerToken();
		String key = JsonUtils.getAsString(consumerTokenEntries, "key");
		String secret = JsonUtils.getAsString(consumerTokenEntries, "secret");
		this._authenticationService = new ServiceBuilder(key).apiSecret(secret).build(this.api);
		return this._authenticationService;
	}

	private JsonObject getConsumerToken() throws IOException, OpenemsNamedException {
		byte[] rawRequest = ("client=" + CLIENT_ID).getBytes(StandardCharsets.UTF_8);
		HttpURLConnection connection = getConnection(this.api.getBaseAddress() + "/oauth1/consumer_token", "POST", true,
				true);
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
		connection.setRequestProperty("Content-Length", Integer.toString(rawRequest.length));
		connection.connect();
		connection.getOutputStream().write(rawRequest);
		connection.getOutputStream().flush();
		String content = StreamUtils.getStreamContents(connection.getInputStream());
		connection.disconnect();
		return JsonUtils.getAsJsonObject(JsonUtils.parse(content));
	}

	private static HttpURLConnection getConnection(String rawURL, String method, boolean doInput, boolean doOutput)
			throws IOException {
		URL url = new URL(rawURL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setDoInput(doInput);
		connection.setDoOutput(doOutput);
		connection.setRequestMethod(method);
		connection.setRequestProperty("Accept", "*");
		connection.setInstanceFollowRedirects(false);
		connection.setRequestProperty("charset", "utf-8");
		connection.setUseCaches(false);
		return connection;
	}

	private static String authorize(String authorizationURL) throws IOException {
		HttpURLConnection connection = getConnection(authorizationURL, "GET", true, false);
		connection.connect();
		String content = StreamUtils.getStreamContents(connection.getInputStream());
		connection.disconnect();
		return content.substring(content.indexOf('=') + 1);
	}
}
