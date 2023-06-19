package io.openems.edge.bridge.http;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.base.Optional;
import com.google.gson.Gson;

import io.openems.edge.bridge.http.OsDetector.Plattform;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import rocks.kavin.reqwest4j.ReqwestUtils;
import rocks.kavin.reqwest4j.Response;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import kotlin.Pair;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.HTTP", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({
	EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
	EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class HttpBridgeImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, HttpBridge {
	
	private Logger log = Logger.getLogger(getClass());
	private Gson gson = new Gson();
	private Config config = null;
	private AtomicInteger requestCounter = new AtomicInteger(0);
	
	public HttpBridgeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				HttpBridge.ChannelId.values()
		);

	}
	
	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public HttpResponse get(URL url) {
		this.requestCounter.incrementAndGet();
		var r = this.fetch(url.toString(), "GET", null, Map.of("Accept", "application/json"));
		return new HttpResponse(r);
	}
	
	@Override
	public HttpResponse post(URL url, Object body) {
		this.requestCounter.incrementAndGet();
		var headers = Map.of("Accept", "application/json", "Content-Type", "application/json");
		var r = this.fetch(url.toString(), "POST", this.gson.toJson(body).getBytes(StandardCharsets.UTF_8), headers);
		return new HttpResponse(r);
	}

	@Override
	public void handleEvent(Event event) {
		this.updateRequestPerCycle();
		if (this.config.test_connection()) {
			this.log.info("Sending test request to https://fenecon.de/");
			var response = this.fetch("https://fenecon.de", "GET", null, null);
			this.log.info("Response status code: " + response.status());
			this.log.info("Response headers: " + response.headers());
		}
	}
	
	private Response fetch(String url, String method, byte[] bodyContent, Map<String, String> headers) {
		if (this.config != null && this.config.verbose()) {
			method = Optional.fromNullable(method).or("");
			bodyContent = Optional.fromNullable(bodyContent).or(new byte[] {});
			headers = Optional.fromNullable(headers).or(Map.of());
			
			this.log.debug(method + " " + url);
			for (String h : headers.keySet()) {
				this.log.debug(h + ": " + headers.get(h));
			}
			this.log.debug("Body: \n" + new String(bodyContent));
		}
		
		if (OsDetector.findOs() == Plattform.Windows) {
			return this.mockNativeApi(url, method, bodyContent, headers);
		} else {
			return ReqwestUtils.fetch(url, method, bodyContent, headers);
		}
	}
	

	
	private Response mockNativeApi(String url, String method, byte[] bodyContent, Map<String, String> headers) {
		OkHttpClient client = new OkHttpClient();
		var req = new Request.Builder()
				.url(url);
		if (!method.equals("GET")) {
			req.method(method, RequestBody.create(bodyContent));
		} else {
			req.method(method, null);
		}
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			req.addHeader(key, val);
		}
		try (var rsp = client.newCall(req.build()).execute()) {
			int statusCode = rsp.code();
			byte[] body = rsp.body().bytes();
			String finalUrl = rsp.request().url().toString();
			Map<String, String> returnHeaders = new HashMap<>();
			for (Iterator<Pair<String, String>> it = rsp.headers().iterator(); it.hasNext();) {
				var h = it.next();
				returnHeaders.put(h.component1(), h.component2());
			}
			return new Response(statusCode, returnHeaders, body, finalUrl);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void updateRequestPerCycle() {
		this.channel(HttpBridge.ChannelId.REQUEST_PER_CYCLE).setNextValue(this.requestCounter.get());
	}
}
