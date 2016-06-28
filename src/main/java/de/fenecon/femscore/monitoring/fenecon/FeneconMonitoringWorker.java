package de.fenecon.femscore.monitoring.fenecon;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonStreamParser;

import de.fenecon.femscore.modbus.protocol.interfaces.ElementUpdateListener;
import de.fenecon.femscore.monitoring.MonitoringWorker;

public class FeneconMonitoringWorker extends MonitoringWorker implements ElementUpdateListener {
	private final static String URL = "https://fenecon.de/fems2";
	public final static String CACHE_DB_PATH = "/opt/fems-cache.db";
	public final static int MAX_CACHE_ENTRIES_TO_TRANSFER = 10000;
	private final static int CYCLE = 60000;
	public final static String FEMS_SYSTEMMESSAGE = "FEMS Systemmessage";

	private final static Logger log = LoggerFactory.getLogger(FeneconMonitoringWorker.class);

	private final String devicekey;
	private final FeneconMonitoringCache cache;

	public FeneconMonitoringWorker(String devicekey) {
		this.devicekey = devicekey;
		cache = new FeneconMonitoringCache(this);
	}

	private static ConcurrentLinkedQueue<TimedElementValue> queue = new ConcurrentLinkedQueue<>();

	@Override
	public void elementUpdated(String name, Object value) {
		this.offer(new TimedElementValue(name, value));
	}

	public void offer(TimedElementValue tev) {
		queue.offer(tev);
	}

	@Override
	public void run() {
		log.info("FeneconMonitoringWorker {} started", getName());
		// TODO initialize
		while (!isInterrupted()) {
			try {
				log.info("FeneconMonitoringWorker: " + queue.size() + " elements");

				if (queue.isEmpty()) {
					log.info("FENECON Online Monitoring: No new data to send");
				} else {

					String statsBefore = "Queue:" + queue.size() + "; Cache:" + cache.isEmpty();
					// Get entries from current queue
					ArrayList<TimedElementValue> currentQueue = new ArrayList<>(queue.size());
					for (int i = 0; i < MAX_CACHE_ENTRIES_TO_TRANSFER; i++) {
						TimedElementValue tev = queue.poll();
						if (tev == null)
							break;
						currentQueue.add(tev);
					}
					// send request
					JsonObject resultObj = sendToOnlineMonitoring(currentQueue);
					if (resultObj != null) { // sending was successful
						handleJsonRpcResult(resultObj);
						currentQueue.clear(); // clear currentQueue
						// transfer from cache
						try {
							currentQueue.addAll(cache.pollMany(MAX_CACHE_ENTRIES_TO_TRANSFER));
						} catch (Exception e1) {
							log.error("Error while receiving from cache: " + e1.getMessage());
							e1.printStackTrace();
						}
						if (!currentQueue.isEmpty()) { // if there are elements
														// in
														// the list
							log.info("Send from cache: " + currentQueue.size());
							resultObj = sendToOnlineMonitoring(currentQueue);
							if (resultObj != null) { // sending was successful
								currentQueue.clear();
							} else { // sending was not successful
								try {
									cache.addAll(currentQueue);
								} catch (Exception e) {
									log.error("Error while adding to cache: " + e.getMessage());
									e.printStackTrace();
									queue.addAll(currentQueue);
								}
							}
						}
					} else { // sending was not successful;
						try {
							cache.addAll(currentQueue);
						} catch (Exception e) {
							log.error("Error while adding to cache");
							e.printStackTrace();
							queue.addAll(currentQueue);
						}
					}
					log.info("  Before[" + statsBefore + "]; Now[" + "Queue:" + queue.size() + "; Cache:"
							+ cache.isEmpty() + "]");
				}
			} catch (Throwable t) {
				log.error("Error in FENECON Online-Monitoring: " + t.getMessage());
				t.printStackTrace();
			}

			try {
				Thread.sleep(CYCLE);
			} catch (InterruptedException e) {
				interrupt();
			}
		}
		log.info("FeneconMonitoringWorker {} stopped", getName());
	}

	private JsonObject sendToOnlineMonitoring(ArrayList<TimedElementValue> queue) {
		JsonObject resultObj = null;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			String json = tevListToJson(queue);
			HttpPost post = new HttpPost(URL);
			post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
			HttpResponse response = client.execute(post);

			JsonStreamParser stream = new JsonStreamParser(new InputStreamReader(response.getEntity().getContent()));
			while (stream.hasNext()) {
				JsonElement mainElement = stream.next();
				if (mainElement.isJsonObject()) {
					JsonObject mainObj = mainElement.getAsJsonObject();
					// read result
					JsonElement resultElement = mainObj.get("result");
					if (resultElement != null) {
						if (resultElement.isJsonObject()) {
							resultObj = resultElement.getAsJsonObject();
						}
					}
					// read error
					JsonElement errorElement = mainObj.get("error");
					if (errorElement != null) {
						throw new IOException(errorElement.toString());
					}
				}
			}
			if (resultObj == null) {
				resultObj = new JsonObject();
			}
			log.info("Successfully sent data");
		} catch (IOException | JsonParseException e) {
			log.error("Send error: " + e.getMessage());
		}
		return resultObj;
	}

	private String tevListToJson(List<TimedElementValue> queue) {
		HashMap<String, HashMap<Long, Object>> valuesPerItem = new HashMap<String, HashMap<Long, Object>>();
		for (TimedElementValue entry : queue) {
			HashMap<Long, Object> values = valuesPerItem.get(entry.getName());
			if (values == null) {
				values = new HashMap<Long, Object>();
				valuesPerItem.put(entry.getName(), values);
			}
			values.put(entry.getTime(), entry.getValue());
		}
		Gson gson = new GsonBuilder().create();
		JsonElement jsonValues = gson.toJsonTree(valuesPerItem);

		// create json rpc
		JsonObject json = new JsonObject();
		json.addProperty("jsonrpc", "2.0");
		json.addProperty("method", devicekey);
		json.addProperty("id", 1);
		json.add("params", jsonValues);
		return gson.toJson(json);
	}

	private void handleJsonRpcResult(JsonObject resultObj) {
		JsonElement yalerElement = resultObj.get("yaler");
		if (yalerElement != null) {
			String yalerRelayDomain = yalerElement.getAsString();
			try {
				if (yalerRelayDomain.equals("false")) {
					log.info("Yaler: deactivate Tunnel");
					// TODO FemsYaler.getFemsYaler().deactivateTunnel();
				} else {
					log.info("Yaler: activate Tunnel - " + yalerRelayDomain);
					// TODO
					// FemsYaler.getFemsYaler().activateTunnel(yalerRelayDomain);
				}
			} catch (Exception e) {
				log.error("Error while activating/deactivating yaler: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
