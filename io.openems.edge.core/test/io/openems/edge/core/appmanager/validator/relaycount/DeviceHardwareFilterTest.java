package io.openems.edge.core.appmanager.validator.relaycount;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.app.hardware.IoGpio;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.io.test.DummyInputOutput;

public class DeviceHardwareFilterTest {

	private AppManagerTestBundle test;
	private DeviceHardwareFilter hardwareFilter;

	private OpenemsApp deviceHardwareApp;
	private OpenemsApp deviceHardwareAppWithoutIo;
	private OpenemsApp ioApp;

	@Before
	public void setUp() throws Exception {
		this.test = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.deviceHardwareApp = Apps.techbaseCm3(t), //
					this.deviceHardwareAppWithoutIo = Apps.techbaseCm4Max(t), //
					this.ioApp = Apps.ioGpio(t) //
			);
		});

		this.hardwareFilter = new DeviceHardwareFilter(this.test.appManagerUtil);
	}

	@Test
	public void testApplyWithHardwareInstance() throws Exception {
		this.test.tryInstallWithMinConfig(this.deviceHardwareApp);

		final var ioInstance = this.test.findFirst(this.ioApp.getAppId());
		final var ioId = ioInstance.properties.get(IoGpio.Property.IO_ID.name()).getAsString();

		final var relayFilter = this.hardwareFilter.apply();
		assertFalse(relayFilter.componentFilter().test(new DummyInputOutput(ioId)));
		assertTrue(relayFilter.componentFilter().test(new DummyInputOutput("someOtherIoId0")));
	}

	@Test
	public void testApplyWithoutHardwareInstance() throws Exception {
		final var relayFilter = this.hardwareFilter.apply();
		assertTrue(relayFilter.componentFilter().test(new DummyInputOutput("io0")));
	}

	@Test
	public void testApplyWithoutIoDependecy() throws Exception {
		this.test.tryInstallWithMinConfig(this.deviceHardwareAppWithoutIo);
		assertNull(this.test.findFirst(this.ioApp.getAppId()));

		this.test.tryInstallWithMinConfig(this.ioApp);
		final var ioInstance = this.test.findFirst(this.ioApp.getAppId());
		final var ioId = ioInstance.properties.get(IoGpio.Property.IO_ID.name()).getAsString();

		final var relayFilter = this.hardwareFilter.apply();
		assertTrue(relayFilter.componentFilter().test(new DummyInputOutput("io0")));
		assertTrue(relayFilter.componentFilter().test(new DummyInputOutput(ioId)));
	}

	@Test
	public void testApplyWithOtherIo() throws Exception {
		this.test.tryInstallWithMinConfig(this.ioApp);
		final var ioInstance = this.test.findFirst(this.ioApp.getAppId());
		final var ioId = ioInstance.properties.get(IoGpio.Property.IO_ID.name()).getAsString();

		final var relayFilter = this.hardwareFilter.apply();
		assertTrue(relayFilter.componentFilter().test(new DummyInputOutput(ioId)));
	}

}
