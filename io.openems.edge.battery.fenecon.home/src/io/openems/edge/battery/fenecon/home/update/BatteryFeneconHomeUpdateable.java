package io.openems.edge.battery.fenecon.home.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.hash.Hashing;

import io.openems.common.session.Language;
import io.openems.common.utils.BehaviorSubject;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.common.type.TextProvider;
import io.openems.edge.common.update.Progress;
import io.openems.edge.common.update.ProgressHistory;
import io.openems.edge.common.update.ProgressPublisher;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;

public class BatteryFeneconHomeUpdateable implements Updateable {
	private static final long MILLIS_BETWEEN_UPDATE_PROGRESS_CHECK = 500L;
	private static final long SLEEP_TIME = 200L;
	private static final int AVG_UPDATE_DURATION_IN_MINUTES = 25;
	private static final int MAX_SLAVE_UPDATE_DURATION_IN_MINUTES = 60;

	private final Logger log;
	private final BatteryFeneconHomeUpdateParams updateParamsProvider;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
	private final BridgeModbus bridgeModbus;
	private final Consumer<FeneconBatteryUpdateEvent> eventCallback;

	private final int modbusId;
	private final BatteryData batteryData;

	private final BehaviorSubject<FeneconBatteryUpdateState> updateState = new BehaviorSubject<>(
			new FeneconBatteryUpdateState.NormalOperation());

	public BatteryFeneconHomeUpdateable(//
			BridgeModbus bridgeModbus, //
			int modbusId, //
			BatteryFeneconHomeUpdateParams updateParamsProvider, //
			BatteryData batteryData, //
			Consumer<FeneconBatteryUpdateEvent> eventCallback, //
			Logger logger) {
		this.log = logger;
		this.bridgeModbus = bridgeModbus;
		this.modbusId = modbusId;
		this.updateParamsProvider = updateParamsProvider;
		this.batteryData = batteryData;
		this.eventCallback = eventCallback;
	}

	@VisibleForTesting
	protected BatteryFeneconHomeUpdateable(//
			BridgeModbus bridgeModbus, //
			int modbusId, //
			BatteryFeneconHomeUpdateParams updateParamsProvider, //
			BatteryData batteryData, //
			Logger logger) {
		this.log = logger;
		this.bridgeModbus = bridgeModbus;
		this.modbusId = modbusId;
		this.updateParamsProvider = updateParamsProvider;
		this.batteryData = batteryData;
		this.eventCallback = null;
	}

	/**
	 * Deactivates this {@link Updateable}.
	 */
	public void deactivate() {
		this.executor.shutdown();
	}

	@Override
	public UpdateableMetaInfo getMetaInfo(Language language) {
		return this.updateParamsProvider.getMetaInfo();
	}

	@Override
	public void executeUpdate() {
		synchronized (this) {
			if (this.updateState.getValue() instanceof FeneconBatteryUpdateState.UpdateRunning) {
				return;
			}
			this.updateState.setValue(new FeneconBatteryUpdateState.UpdateRunning(0, Collections.emptyList()));
		}

		this.triggerEvent(new FeneconBatteryUpdateEvent.UpdateRunning());

		this.executor.execute(() -> {
			try {
				final var progressHistory = new ProgressHistory();
				progressHistory.addOnChangeListener(history -> {
					final var last = history.last();
					this.log.info("Update progress: " + last.toString());
					this.updateState
							.setValue(new FeneconBatteryUpdateState.UpdateRunning(last.percentage(), history.asLog()));
				});

				this.executeUpdateInternal(progressHistory);
				progressHistory.addProgress(new Progress(100, "Finished Fenecon Home Battery update"));

				this.updateState.setValue(new FeneconBatteryUpdateState.NormalOperation());
				this.triggerEvent(new FeneconBatteryUpdateEvent.UpdateSuccess());
			} catch (Exception e) {
				this.log.error("Error while executing home battery firmware update", e);
				var errorText = TextProvider
						.byTranslation(BatteryFeneconHomeUpdateable.class, "BatteryFeneconHome.UpdateFailed")
						.formatWithArguments(e.getMessage());
				this.updateState.setValue(new FeneconBatteryUpdateState.UpdateFailed(errorText));
				this.triggerEvent(new FeneconBatteryUpdateEvent.UpdateFailed(e));
			}
		});
	}

	private void executeUpdateInternal(ProgressHistory progressHistory) throws Exception {
		if (!(this.bridgeModbus instanceof BridgeModbusSerial serialBridge)) {
			throw new Exception("No serial modbus bridge.");
		}
		if (serialBridge.isStopped()) {
			throw new Exception("Serial modbus bridge is not started.");
		}

		final var updateParams = this.updateParamsProvider.getParams(this.batteryData.getBatteryType());
		if (updateParams == null) {
			throw new Exception("Battery type " + this.batteryData.getBatteryType() + " does not support update.");
		}

		var progress = new ProgressPublisher();
		progress.addListener(progressHistory::addProgress);
		progress.setPercentage(0, "Download battery update ...");

		final var updateFileContent = this.downloadUpdateFile(updateParams);
		progress.setPercentage(2);

		serialBridge.stop();
		try {
			progress.sleep(15000, 3, 5, "Waiting for modbus bridge to stop");

			try (final var updateHandler = new UpdateHandler(serialBridge.getPortName(), serialBridge.getBaudrate(),
					this.modbusId, this.log)) {
				this.updateBattery(updateHandler, updateFileContent, updateParams, progress.subProgress(6, 96));
			}

			progress.sleep(15000, 97, 100, "Waiting for modbus bridge to start");
		} catch (Exception ex) {
			Thread.sleep(2000);
			throw ex;
		} finally {
			serialBridge.start();
		}
	}

	protected void updateBattery(UpdateHandler handler, byte[] updateFileContent,
			BatteryFeneconHomeUpdateParams.UpdateParams updateParams, ProgressPublisher progress) throws Exception {
		progress.setPercentage(1, "Initialize update with battery");
		handler.sendUpdateInitiate(updateFileContent.length);
		this.sleep();

		progress.setPercentage(5, "Erase flash");
		handler.sendEraseFlash();
		this.sleep();

		progress.setPercentage(10, "Send firmware");
		handler.sendFirmwareData(updateFileContent, progress.subProgress(11, 60));
		this.sleep();

		progress.setPercentage(61, "Verify firmware");
		handler.sendVerifyFirmware();
		this.sleep();

		progress.setPercentage(70, "Update slaves");
		this.waitForSlaveUpdate(handler, progress.subProgress(71, 98));
		this.sleep();

		progress.sleep(1000, 98, 99, "Check firmware version");

		var version = handler.readFirmwareVersion();
		if (!version.equals(updateParams.version())) {
			this.log.warn("Incorrect firmware version returned after update. Expected {}, got {}",
					updateParams.version(), version);
		}

		progress.setPercentage(100, "Version updated. " + version);
	}

	private void sleep() throws Exception {
		Thread.sleep(SLEEP_TIME);
	}

	private void waitForSlaveUpdate(UpdateHandler updateHandler, ProgressPublisher progressPublisher) throws Exception {
		var updatePercentage = 0;
		var stopwatch = Stopwatch.createStarted();

		while (true) {
			var status = updateHandler.readSlaveUpdateStatus();
			if (stopwatch.elapsed(TimeUnit.MINUTES) >= MAX_SLAVE_UPDATE_DURATION_IN_MINUTES) {
				throw new Exception(
						"Slave update (current progress: %s) took longer than maximum duration of %d minutes. Is the battery really doing something?"
								.formatted(status, MAX_SLAVE_UPDATE_DURATION_IN_MINUTES));
			}

			switch (status) {
			case UpdateHandler.SlaveUpdateStatus.Done() -> {
				progressPublisher.setPercentage(100);
				return;
			}
			case UpdateHandler.SlaveUpdateStatus.Error(var statusCode) -> {
				throw new Exception("Slave update failed: " + statusCode);
			}
			case UpdateHandler.SlaveUpdateStatus.InProgress(int percentage) -> {
				// Battery reports lower percentages at the end of the update for a few seconds.
				updatePercentage = Math.max(updatePercentage, percentage);
				progressPublisher.setPercentage(updatePercentage);
				Thread.sleep(MILLIS_BETWEEN_UPDATE_PROGRESS_CHECK);
			}
			}

		}
	}

	private byte[] downloadUpdateFile(BatteryFeneconHomeUpdateParams.UpdateParams updateParams) throws Exception {
		var url = this.updateParamsProvider.getArmDownloadLocation(updateParams);
		var firmwareUpdateData = downloadFile(url);

		var hash = Hashing.sha256().hashBytes(firmwareUpdateData);
		if (!hash.equals(updateParams.sha256())) {
			throw new RuntimeException(
					"Checksum verification of downloaded update file failed. File: '%s', Expected hash: '%s', calculated hash: '%s'"
							.formatted(url, updateParams.sha256(), hash));
		}
		return firmwareUpdateData;
	}

	private static byte[] downloadFile(String url) throws Exception {
		try (var client = HttpClient.newBuilder().build()) {
			var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200) {
				throw new Exception("Download of firmware update file failed. Expected status code 200, got "
						+ response.statusCode() + ": '"
						+ StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response.body())) + "'");
			}

			return response.body();
		}
	}

	private void triggerEvent(FeneconBatteryUpdateEvent event) {
		if (this.eventCallback != null) {
			this.eventCallback.accept(event);
		}
	}

	@Override
	public GetUpdateState.UpdateState getUpdateState(Language lang) {
		return switch (this.updateState.getValue()) {
		case FeneconBatteryUpdateState.NormalOperation() -> //
			this.calculateAvailableUpdatesStatus();
		case FeneconBatteryUpdateState.UpdateRunning(var percentCompleted, var logs) -> //
			new GetUpdateState.UpdateState.Running(percentCompleted, AVG_UPDATE_DURATION_IN_MINUTES, logs);
		case FeneconBatteryUpdateState.UpdateFailed(var errorText) -> //
			new GetUpdateState.UpdateState.Error(errorText.getText(lang));
		};

	}

	private GetUpdateState.UpdateState calculateAvailableUpdatesStatus() {
		if (!this.batteryData.isBatteryRunning()) {
			return new GetUpdateState.UpdateState.Unknown();
		}

		final var currentVersion = this.batteryData.getVersion();
		final var currentBatteryType = this.batteryData.getBatteryType();
		if (currentVersion == null || currentBatteryType == null) {
			return new GetUpdateState.UpdateState.Unknown();
		}

		final var updateParams = this.updateParamsProvider.getParams(currentBatteryType);
		if (updateParams == null) {
			return new GetUpdateState.UpdateState.Updated(currentVersion.toString());
		}

		if (currentVersion.isAtLeast(updateParams.version())) {
			return new GetUpdateState.UpdateState.Updated(currentVersion.toString());
		}

		return new GetUpdateState.UpdateState.Available(currentVersion.toString(), updateParams.version().toString());
	}

	private sealed interface FeneconBatteryUpdateState {
		record NormalOperation() implements FeneconBatteryUpdateState {
		}

		record UpdateRunning(int percentCompleted, List<String> logs) implements FeneconBatteryUpdateState {
		}

		record UpdateFailed(TextProvider errorText) implements FeneconBatteryUpdateState {
		}
	}

	public sealed interface FeneconBatteryUpdateEvent {
		record UpdateRunning() implements FeneconBatteryUpdateEvent {
		}

		record UpdateSuccess() implements FeneconBatteryUpdateEvent {
		}

		record UpdateFailed(Exception exception) implements FeneconBatteryUpdateEvent {
		}
	}
}
