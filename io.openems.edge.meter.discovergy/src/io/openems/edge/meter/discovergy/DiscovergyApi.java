package io.openems.edge.meter.discovergy;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.github.scribejava.core.builder.api.DefaultApi10a;
import com.github.scribejava.core.model.OAuth1RequestToken;

public class DiscovergyApi extends DefaultApi10a {

	private final String baseAddress;
	private final String user;
	private final String password;

	public DiscovergyApi(String user, String password) {
		this("https://api.discovergy.com/public/v1", user, password);
	}

	public DiscovergyApi(String baseAddress, String user, String password) {
		this.baseAddress = baseAddress;
		this.user = user;
		this.password = password;
	}

	public String getBaseAddress() {
		return baseAddress;
	}

	public String getUser() {
		return user;
	}

	@Override
	public String getRequestTokenEndpoint() {
		return baseAddress + "/oauth1/request_token";
	}

	@Override
	public String getAccessTokenEndpoint() {
		return baseAddress + "/oauth1/access_token";
	}

	@Override
	public String getAuthorizationUrl(OAuth1RequestToken requestToken) {
		try {
			return baseAddress + "/oauth1/authorize?oauth_token=" + requestToken.getToken() + "&email="
					+ URLEncoder.encode(user, UTF_8.name()) + "&password=" + URLEncoder.encode(password, UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected String getAuthorizationBaseUrl() {
		return baseAddress + "/oauth1/authorize";
	}
}
