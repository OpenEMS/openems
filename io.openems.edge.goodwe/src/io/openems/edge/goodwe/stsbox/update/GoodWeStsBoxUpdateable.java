package io.openems.edge.goodwe.stsbox.update;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.session.Language;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.update.ProgressHistory;
import io.openems.edge.common.update.ProgressPublisher;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;
import io.openems.edge.goodwe.common.enums.GoodWeType;
import io.openems.edge.goodwe.update.UpdateHandler;

public class GoodWeStsBoxUpdateable implements Updateable {

	private static final int ESTIMATED_UPDATE_MINUTES = 10;

	private final Logger log = LoggerFactory.getLogger(GoodWeStsBoxUpdateable.class);

	private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
	private final BridgeModbus bridgeModbus;
	private final GoodWeStsBoxUpdateParams updateParamsProvider;
	private final Runnable closeChannelListener;

	private final BehaviorSubject<GoodWeStsBoxVersion> version = new BehaviorSubject<>(
			new GoodWeStsBoxVersion(null, null));

	private final AtomicReference<GoodWeType> goodWeType = new AtomicReference<>();
	private final AtomicReference<GetUpdateState.UpdateState> runningUpdateState = new AtomicReference<>(
			new GetUpdateState.UpdateState.Unknown());

	public GoodWeStsBoxUpdateable(//
			BridgeModbus bridgeModbus, //
			GoodWeStsBoxUpdateParams updateParamsProvider, //
			Channel<GoodWeType> goodWeType, //
			Channel<Integer> firmwareVersion, //
			Channel<Integer> firmwareSubVersion //
	) {
		this.bridgeModbus = bridgeModbus;
		this.updateParamsProvider = updateParamsProvider;

		final var goodWeTypeListener = goodWeType.onChange(this::updateGoodWeType);

		final var firmwareVersionListener = firmwareVersion.onChange(this::updateVersion);
		final var firmwareSubVersionListener = firmwareSubVersion.onChange(this::updateSubVersion);

		this.closeChannelListener = () -> {
			goodWeType.removeOnChangeCallback(goodWeTypeListener);
			firmwareVersion.removeOnChangeCallback(firmwareVersionListener);
			firmwareSubVersion.removeOnChangeCallback(firmwareSubVersionListener);
		};
	}

	/**
	 * Deactivates this {@link Updateable}.
	 */
	public void deactivate() {
		this.executor.shutdown();
		this.closeChannelListener.run();
	}

	@Override
	public UpdateableMetaInfo getMetaInfo(Language language) {
		return this.updateParamsProvider.getMetaInfo();
	}

	@Override
	public void executeUpdate() {
		synchronized (this) {
			if (this.runningUpdateState.get() instanceof GetUpdateState.UpdateState.Running) {
				return;
			}
			this.runningUpdateState
					.set(new GetUpdateState.UpdateState.Running(0, ESTIMATED_UPDATE_MINUTES, Collections.emptyList()));
		}

		this.executor.execute(() -> {
			try {
				final var progressHistory = new ProgressHistory();
				progressHistory.addOnChangeListener(history -> {
					final var last = history.last();
					this.log.info(last.toString());
					this.runningUpdateState.set(new GetUpdateState.UpdateState.Running(last.percentage(),
							ESTIMATED_UPDATE_MINUTES, history.asLog()));
				});

				this.executeUpdateInternal(progressHistory);

				// reset so that "getUpdateState" returns the current version
				this.runningUpdateState.set(new GetUpdateState.UpdateState.Unknown());
			} catch (Exception e) {
				this.runningUpdateState.set(new GetUpdateState.UpdateState.Error(e.getMessage()));
				this.log.error("Error while executing Firmware Update: ", e);
			}
		});
	}

	@Override
	public GetUpdateState.UpdateState getUpdateState(Language language) {
		final var currentUpdateState = this.runningUpdateState.get();
		if (currentUpdateState instanceof GetUpdateState.UpdateState.Running
				|| currentUpdateState instanceof GetUpdateState.UpdateState.Error) {
			return currentUpdateState;
		}

		final var currentVersion = this.version.getValue();
		if (!currentVersion.isDefined()) {
			return new GetUpdateState.UpdateState.Unknown();
		}

		final var updateParams = this.updateParamsProvider.getParams(this.goodWeType.get());
		if (updateParams == null) {
			return new GetUpdateState.UpdateState.Updated(currentVersion.toString());
		}

		if (!currentVersion.equals(updateParams.latestVersion())) {
			return new GetUpdateState.UpdateState.Available(currentVersion.toString(),
					updateParams.latestVersion().toString());
		}

		return new GetUpdateState.UpdateState.Updated(currentVersion.toString());
	}

	private void executeUpdateInternal(ProgressHistory progressHistory) throws Exception {
		if (!(this.bridgeModbus instanceof BridgeModbusSerial serialBridge)) {
			throw new OpenemsRuntimeException("no serial modbus bridge.");
		}

		final var updateParams = this.updateParamsProvider.getParams(this.goodWeType.get());
		if (updateParams == null) {
			throw new OpenemsRuntimeException(
					"No update params available for sts box with type " + this.goodWeType + ".");
		}

		final var progress = new ProgressPublisher();
		progress.addListener(progressHistory::addProgress);
		progress.setPercentage(0, "Start Sts Box Update ...");

		progress.setPercentage(1, "Downloading sts update file");
		final var updateFileBytes = this.downloadUpdateFileAndCheckSum(updateParams);
		progress.setPercentage(10);

		final var portName = serialBridge.getPortName();
		final var baudrate = serialBridge.getBaudrate();

		this.log.info("Settings - Port: {} | Baud: {}", portName, baudrate);

		serialBridge.stop();
		progress.sleep(15000, 10, 15, "Waiting for modbus bridge to stop");

		try (final var updateHandler = new UpdateHandler(portName, baudrate, this.log)) {
			updateHandler.updateStsVersion(progress.subProgress(15, 99), updateFileBytes);
		} finally {
			serialBridge.start();
		}

		this.waitForVersionUpdateBlocking(progress.subProgress(99, 100), updateParams.latestVersion());
		progress.setPercentage(100, "Version updated. " + updateParams.latestVersion());
	}

	private void waitForVersionUpdateBlocking(ProgressPublisher progress, GoodWeStsBoxVersion expectedVersion) {
		try {
			this.waitForVersionUpdate(progress, expectedVersion) //
					.get(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new OpenemsRuntimeException("Failed to wait for version update", e);
		}
	}

	@VisibleForTesting
	CompletableFuture<Void> waitForVersionUpdate(ProgressPublisher progress, GoodWeStsBoxVersion expectedVersion) {
		progress.setPercentage(0, "Wait for version update...");
		final var future = new CompletableFuture<Void>();
		final Consumer<GoodWeStsBoxVersion> listener = goodWeVersion -> {
			if (!goodWeVersion.isDefined()) {
				return;
			}
			if (!expectedVersion.equals(goodWeVersion)) {
				return;
			}
			future.complete(null);
		};
		this.version.subscribe(listener);
		if (this.version.getValue().equals(expectedVersion)) {
			future.complete(null);
		}

		return future.whenComplete((unused, throwable) -> {
			this.version.unsubscribe(listener);
			if (throwable == null) {
				progress.setPercentage(100, "Finished waiting");
			}
		});
	}

	private byte[] downloadUpdateFileAndCheckSum(GoodWeStsBoxParams updateParams) throws Exception {
		var url = this.updateParamsProvider.getDownloadLocation(updateParams);
		var firmwareUpdateData = downloadFile(url);

		var hash = Hashing.sha256().hashBytes(firmwareUpdateData);
		if (!hash.equals(updateParams.checksum())) {
			throw new OpenemsRuntimeException(
					"Checksum verification of downloaded update file failed. File: '%s', Expected hash: '%s', calculated hash: '%s'"
							.formatted(url, updateParams.checksum(), hash));
		}
		return firmwareUpdateData;
	}

	private static byte[] downloadFile(String url) throws URISyntaxException, IOException, InterruptedException {
		try (var client = HttpClient.newBuilder().build()) {
			var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				throw new OpenemsRuntimeException(
						"Download of firmware update file failed. Expected status code 200, got "
								+ response.statusCode() + ": '"
								+ StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response.body())) + "'");
			}

			return response.body();
		}
	}

	private void updateGoodWeType(Value<GoodWeType> oldValue, Value<GoodWeType> newValue) {
		this.goodWeType.set(newValue.asEnum());
	}

	private void updateSubVersion(Value<Integer> oldValue, Value<Integer> newValue) {
		this.version.updateValue(goodWeVersion -> goodWeVersion.withFirmwareSubVersion(newValue.get()));
	}

	private void updateVersion(Value<Integer> oldValue, Value<Integer> newValue) {
		this.version.updateValue(goodWeVersion -> goodWeVersion.withFirmwareVersion(newValue.get()));
	}

}
