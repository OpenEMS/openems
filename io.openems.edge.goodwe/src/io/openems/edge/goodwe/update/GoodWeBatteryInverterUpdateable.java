package io.openems.edge.goodwe.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.session.Language;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.update.Progress;
import io.openems.edge.common.update.ProgressHistory;
import io.openems.edge.common.update.ProgressPublisher;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;
import io.openems.edge.goodwe.common.enums.GoodWeType;

public class GoodWeBatteryInverterUpdateable implements Updateable {

	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterUpdateable.class);

	private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
	private final BridgeModbus bridgeModbus;
	private final GoodWeBatteryInverterUpdateParams updateParamsProvider;
	private final Runnable closeChannelListener;

	private final AtomicReference<GoodWeVersion> version = new AtomicReference<>(
			new GoodWeVersion(null, null, null, null));
	private final List<Consumer<GoodWeVersion>> versionListener = new CopyOnWriteArrayList<>();

	private volatile GoodWeType goodWeType = GoodWeType.UNDEFINED;
	private volatile GetUpdateState.UpdateState updateState = new GetUpdateState.UpdateState.Unknown();

	public GoodWeBatteryInverterUpdateable(//
			BridgeModbus bridgeModbus, //
			GoodWeBatteryInverterUpdateParams updateParamsProvider, //
			Channel<GoodWeType> goodWeType, //
			Channel<Integer> dspFirmwareVersionMaster, //
			Channel<Integer> dspFirmwareBetaVersionMaster, //
			Channel<Integer> armFirmwareVersion, //
			Channel<Integer> armFirmwareBetaVersion //
	) {
		this.bridgeModbus = bridgeModbus;
		this.updateParamsProvider = updateParamsProvider;

		var goodWeTypeListener = goodWeType.onChange((oldValue, newValue) -> {
			this.goodWeType = newValue.asEnum();
		});

		var dspFirmwareVersionListener = dspFirmwareVersionMaster.onChange((oldValue, newValue) -> {
			this.updateVersion(goodWeVersion -> goodWeVersion.withDspFirmwareVersion(newValue.get()));
		});
		var dspFirmwareBetaVersionListener = dspFirmwareBetaVersionMaster.onChange((oldValue, newValue) -> {
			this.updateVersion(goodWeVersion -> goodWeVersion.withDspFirmwareVersionBeta(newValue.get()));
		});
		var armFirmwareVersionListener = armFirmwareVersion.onChange((oldValue, newValue) -> {
			this.updateVersion(goodWeVersion -> goodWeVersion.withArmFirmwareVersion(newValue.get()));
		});
		var armFirmwareBetaVersionListener = armFirmwareBetaVersion.onChange((oldValue, newValue) -> {
			this.updateVersion(goodWeVersion -> goodWeVersion.withArmFirmwareVersionBeta(newValue.get()));
		});

		this.closeChannelListener = () -> {
			goodWeType.removeOnChangeCallback(goodWeTypeListener);
			dspFirmwareVersionMaster.removeOnChangeCallback(dspFirmwareVersionListener);
			dspFirmwareBetaVersionMaster.removeOnChangeCallback(dspFirmwareBetaVersionListener);
			armFirmwareVersion.removeOnChangeCallback(armFirmwareVersionListener);
			armFirmwareBetaVersion.removeOnChangeCallback(armFirmwareBetaVersionListener);
		};
	}

	private void updateVersion(UnaryOperator<GoodWeVersion> updateFunction) {
		final var newVersion = this.version.updateAndGet(updateFunction);
		for (var listener : this.versionListener) {
			listener.accept(newVersion);
		}
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
			if (this.updateState instanceof GetUpdateState.UpdateState.Running) {
				return;
			}
			this.updateState = new GetUpdateState.UpdateState.Running(0, Collections.emptyList());
		}

		this.executor.execute(() -> {
			try {
				final var progressHistory = new ProgressHistory();
				progressHistory.addOnChangeListener(history -> {
					final var last = history.last();
					this.log.info(last.toString());
					this.updateState = new GetUpdateState.UpdateState.Running(last.percentage(), history.asLog());
				});

				this.executeUpdateInternal(progressHistory);
				progressHistory.addProgress(new Progress(100, "Finished GoodWe update"));
			} catch (Exception e) {
				// TODO update state failed?
				this.log.error("Error while executing Firmware Update: ", e);
			} finally {
				this.updateState = new GetUpdateState.UpdateState.Unknown();
			}
		});
	}

	@Override
	public GetUpdateState.UpdateState getUpdateState() {
		final var currentUpdateState = this.updateState;
		if (currentUpdateState instanceof GetUpdateState.UpdateState.Running) {
			return currentUpdateState;
		}

		final var currentVersion = this.version.get();
		if (!currentVersion.isDefined()) {
			return new GetUpdateState.UpdateState.Unknown();
		}

		final var updateParams = this.updateParamsProvider.getParams(this.goodWeType);
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
			throw new Exception("no serial modbus bridge.");
		}

		final var goodWeType = this.goodWeType;
		if (goodWeType == GoodWeType.UNDEFINED) {
			throw new Exception("GoodWeType not defined.");
		}

		final var updateParams = this.updateParamsProvider.getParams(goodWeType);
		if (updateParams == null) {
			throw new Exception("GoodWeType " + goodWeType + " not supported for update.");
		}

		final var progress = new ProgressPublisher();
		progress.addListener(progressHistory::addProgress);
		progress.setPercentage(0, "Start GoodWe Update ...");

		final var tempDirectory = new File(System.getProperty("java.io.tmpdir"));

		final var fileNameArm = new File(tempDirectory, "arm.bin");
		final var fileNameDsp = new File(tempDirectory, "dsp.bin");
		final var armUrl = new URI(this.updateParamsProvider.getArmDownloadLocation(updateParams)).toURL();
		final var dspUrl = new URI(this.updateParamsProvider.getDspDownloadLocation(updateParams)).toURL();

		progress.setPercentage(1, String.format("Downloading arm file from %s to %s", armUrl, fileNameArm));
		downloadFile(armUrl, fileNameArm);
		progress.setPercentage(6, String.format("Downloading dsp file from %s to %s", dspUrl, fileNameDsp));
		downloadFile(dspUrl, fileNameDsp);
		progress.setPercentage(10);

		final var portName = serialBridge.getPortName();
		final var baudrate = serialBridge.getBaudrate();

		this.log.info("Settings - Port: {} | Baud: {}", portName, baudrate);

		serialBridge.stop();
		progress.sleep(15000, 10, 15, "Waiting for modbus bridge to stop");

		try (final var updateHandler = new UpdateHandler(portName, baudrate)) {
			updateHandler.updateArmVersion(progress.subProgress(15, 50), fileNameArm.toString());

			updateHandler.closePort();
			progress.sleep(1_000 * 60 * 3, 50, 60, "Waiting for serial connection restart");
			updateHandler.openPort();

			updateHandler.updateDspVersion(progress.subProgress(60, 98), fileNameDsp.toString());
		} finally {
			if (!fileNameArm.delete()) {
				this.log.info("Unable to delete arm file");
			}
			if (!fileNameDsp.delete()) {
				this.log.info("Unable to delete dsp file");
			}
			serialBridge.start();
		}

		this.waitForVersionUpdate(progress.subProgress(99, 100), updateParams.latestVersion());
		progress.setPercentage(100, "Version updated. " + updateParams.latestVersion());
	}

	private void waitForVersionUpdate(ProgressPublisher progress, GoodWeVersion expectedVersion) {
		progress.setPercentage(0, "Wait for version update...");
		final var future = new CompletableFuture<Void>();
		final Consumer<GoodWeVersion> listener = goodWeVersion -> {
			if (!goodWeVersion.isDefined()) {
				return;
			}
			if (!expectedVersion.equals(goodWeVersion)) {
				return;
			}
			future.complete(null);
		};
		try {
			this.versionListener.add(listener);
			if (this.version.get().equals(expectedVersion)) {
				return;
			}

			future.get(5, TimeUnit.MINUTES);
			progress.setPercentage(100, "Finished waiting");
		} catch (Exception e) {
			throw new RuntimeException("Failed to wait for version update", e);
		} finally {
			this.versionListener.remove(listener);
		}

	}

	private static void downloadFile(URL url, File destination) throws IOException {
		try (var readableByteChannel = Channels.newChannel(url.openStream());
				var fileOutputStream = new FileOutputStream(destination)) {
			var fileChannel = fileOutputStream.getChannel();
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		}
	}

}
