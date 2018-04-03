package io.openems.backend.metadata.odoo;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

/**
 * This worker combines writes to lastMessage and lastUpdate fields, to avoid
 * DDOSing Odoo by writing too often.
 * 
 * @author stefan.feilmeier
 *
 */
public class OdooWriteWorker {

	private final Logger log = LoggerFactory.getLogger(OdooWriteWorker.class);

	protected final static int UPDATE_INTERVAL_IN_SECONDS = 60;

	public OdooWriteWorker(Odoo parent) {
		this.future = this.executor.scheduleWithFixedDelay(() -> {
			/*
			 * This task is executed regularly. Sends data to websocket.
			 */
			String time = OdooUtils.DATETIME_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
			{
				Integer[] ids;
				synchronized (this.lastMessageIds) {
					ids = this.lastMessageIds.toArray(new Integer[this.lastMessageIds.size()]);
					this.lastMessageIds.clear();
				}
				if (ids.length > 0) {
					try {
						OdooUtils.write(parent.url, parent.database, parent.uid, parent.password, "fems.device", ids,
								new FieldValue(Field.FemsDevice.LAST_MESSAGE, time));
					} catch (OpenemsException e) {
						log.error("Unable to write lastMessage to ids [" + ids + "]: " + e.getMessage());
					}
				}
			}
			{
				Integer[] ids;
				synchronized (this.lastUpdateIds) {
					ids = this.lastUpdateIds.toArray(new Integer[this.lastUpdateIds.size()]);
					this.lastUpdateIds.clear();
				}
				if (ids.length > 0) {
					try {
						OdooUtils.write(parent.url, parent.database, parent.uid, parent.password, "fems.device", ids,
								new FieldValue(Field.FemsDevice.LAST_UPDATE, time));
					} catch (OpenemsException e) {
						log.error("Unable to write lastUpdate to ids [" + ids + "]: " + e.getMessage());
					}
				}
			}
		}, 0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private final Set<Integer> lastMessageIds = new HashSet<>();
	private final Set<Integer> lastUpdateIds = new HashSet<>();

	/**
	 * Holds the scheduled task
	 */
	private ScheduledFuture<?> future;

	public void onLastMessage(int edgeId) {
		synchronized (this.lastMessageIds) {
			this.lastMessageIds.add(edgeId);
		}
	}

	public void onLastUpdate(int edgeId) {
		synchronized (this.lastUpdateIds) {
			this.lastUpdateIds.add(edgeId);
		}
	}

	public void dispose() {
		// unsubscribe regular task
		this.future.cancel(true);
	}
}
