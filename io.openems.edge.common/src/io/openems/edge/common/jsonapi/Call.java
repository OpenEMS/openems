package io.openems.edge.common.jsonapi;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Call<REQUEST, RESPONSE> {

	private final Logger log = LoggerFactory.getLogger(Call.class);

	private final REQUEST request;
	private RESPONSE response;
	private Map<String, Object> properties;

	private Call(REQUEST request, Map<String, Object> properties) {
		super();
		this.request = request;
		this.properties = properties;
	}

	public Call(REQUEST request) {
		this(request, new TreeMap<>());
	}

	public void setResponse(RESPONSE response) {
		if (this.response != null) {
			this.log.info("Request[" + this.request + "] was already fulfilled!");
		}
		this.response = response;
	}

	/**
	 * Gets the value to which the specific key is mapped, or null if this map
	 * contains no mapping for the key.
	 * 
	 * <p>
	 * The properties may contain special information about the current {@link Call}
	 * e. g. which user did trigger this call.
	 * 
	 * @param <T> the type of value
	 * @param key the key whose associated value is to be returned
	 * @return the value to which the specified key is mapped, or null if this map
	 *         contains no mapping for the key
	 * @see Call#put(Key, Object)
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Key<T> key) {
		return (T) this.properties.get(key.identifier());
	}

	/**
	 * Associates the specified value with the specified key in this map. If the map
	 * previously contained a mapping for the key, the old value is replaced by the
	 * specified value.
	 * 
	 * <p>
	 * With this values can be associated to the current call e. g. the user who
	 * triggered this call.
	 * 
	 * @param <T>   the type of the value
	 * @param key   key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 */
	public <T> void put(Key<T> key, T value) {
		this.properties.put(key.identifier(), value);
	}

	/**
	 * Creates a new {@link Call} with the given request and applies all properties
	 * to the new {@link Call}.
	 * 
	 * @param <REQ>   the type of the new request
	 * @param request the new request
	 * @return the new {@link Call}
	 */
	public <REQ> Call<REQ, RESPONSE> mapRequest(REQ request) {
		return new Call<>(request, this.properties);
	}

	/**
	 * Creates a new {@link Call} with the type of the response mapped to a new
	 * type. The current request and the properties are applied to the new
	 * {@link Call}.
	 * 
	 * @param <RES> the new type of the response
	 * @return the new {@link Call}
	 */
	public <RES> Call<REQUEST, RES> mapResponse() {
		return new Call<>(this.request, this.properties);
	}

	public REQUEST getRequest() {
		return this.request;
	}

	public RESPONSE getResponse() {
		return this.response;
	}

}