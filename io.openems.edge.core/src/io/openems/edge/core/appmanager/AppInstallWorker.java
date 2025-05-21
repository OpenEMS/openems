package io.openems.edge.core.appmanager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.AppCenterGetPossibleAppsResponse.Bundle;
import io.openems.common.session.Language;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class AppInstallWorker extends AbstractWorker {

	private final Object keyForFreeAppsLock = new Object();
	private String keyForFreeApps = null;

	/**
	 * Time to wait before doing the check. This allows the system to completely
	 * boot and read configurations.
	 */
	private static final int INITIAL_WAIT_TIME = 60_000; // in ms
	private static final int INACTIVE_WAIT_TIME = 1_000 * 60 * 60 * 24; // 1 day
	private static final int RELOAD_FREE_APPS_TIME = 10; // every 10 days

	private int reloadFreeApps = 0;
	private List<Bundle> freeApps = null;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final AppManagerImpl parent;

	public AppInstallWorker(AppManagerImpl parent) {
		this.parent = parent;
	}

	private void installFreeApps() {
		final var appsToInstall = this.freeApps;
		if (appsToInstall == null) {
			return;
		}
		for (var bundle : appsToInstall) {
			// only install first successfully installed app
			for (var app : bundle.apps) {
				if (this.parent.getInstantiatedApps().stream().anyMatch(t -> t.appId.equals(app.appId))) {
					break;
				}
				try {
					this.installApp(app.appId);
					break;
				} catch (Throwable e) {
					this.log.info("Unable to install free App[" + app.appId + "]");
					e.printStackTrace();
				}
			}
		}
	}

	private void installApp(String appId) throws OpenemsNamedException {
		final var app = this.parent.findAppByIdOrError(appId);

		JsonObject requestProperties;
		try {
			var properties = app.getProperties();
			requestProperties = Arrays.stream(properties) //
					.map(p -> {
						return Map.<String, JsonElement>entry(p.name, //
								p.getDefaultValue(Language.DEFAULT) //
										.orElse(JsonNull.INSTANCE));
					}) //
					.collect(Collector.<Entry<String, JsonElement>, JsonObject>of(JsonObject::new,
							(t, u) -> t.add(u.getKey(), u.getValue()), (t, u) -> {
								t.entrySet().forEach(e -> {
									if (!e.getValue().equals(JsonNull.INSTANCE)) {
										u.add(e.getKey(), e.getValue());
									}
								});
								return u;
							})) //
			;
		} catch (UnsupportedOperationException e) {
			requestProperties = new JsonObject();
		}

		final var alias = Optional.ofNullable(requestProperties.get("ALIAS")) //
				.map(j -> j.getAsString()) //
				.orElse(app.getName(Language.DEFAULT));

		this.parent.handleAddAppInstanceRequest(null,
				new AddAppInstance.Request(appId, this.keyForFreeApps, alias, requestProperties));
	}

	private void reloadFreeApps() {
		if (!this.parent.backendUtil.isConnected()) {
			this.freeApps = null;
			return;
		}
		final String key = this.keyForFreeApps;
		if (key == null) {
			return;
		}
		this.freeApps = this.parent.backendUtil.getPossibleApps(key);
	}

	@Override
	protected void forever() throws Throwable {
		if (this.reloadFreeApps == RELOAD_FREE_APPS_TIME) {
			this.reloadFreeApps();
		}
		this.installFreeApps();
	}

	@Override
	protected int getCycleTime() {
		if (this.keyForFreeApps == null || this.keyForFreeApps.isBlank()) {
			return ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
		}
		this.reloadFreeApps--;
		if (this.reloadFreeApps < 0) {
			this.reloadFreeApps = RELOAD_FREE_APPS_TIME;
		}
		if (!this.isValidBackendResponse()) {
			return INITIAL_WAIT_TIME;
		}
		return INACTIVE_WAIT_TIME;
	}

	public void setKeyForFreeApps(String key) {
		if (Objects.equals(this.keyForFreeApps, key)) {
			return;
		}
		synchronized (this.keyForFreeAppsLock) {
			this.keyForFreeApps = key;
			if (key != null) {
				this.reloadFreeApps = 0;
				this.triggerNextRun();
			}
		}
	}

	private boolean isValidBackendResponse() {
		return this.freeApps != null;
	}

}
