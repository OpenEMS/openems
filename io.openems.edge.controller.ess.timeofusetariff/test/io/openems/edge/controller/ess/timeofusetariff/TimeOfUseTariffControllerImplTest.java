package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;

import java.time.Clock;

import org.junit.Test;

import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.Call;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	private static class DummyEnergyScheduler extends AbstractDummyOpenemsComponent<DummyEnergyScheduler>
			implements EnergyScheduler {

		private final Version version;

		public DummyEnergyScheduler(Version version) {
			super("_energy", "_energy", //
					OpenemsComponent.ChannelId.values(), //
					Controller.ChannelId.values());
			this.version = version;
		}

		@Override
		public JsonrpcResponse handleGetScheduleRequestV1(Call<JsonrpcRequest, JsonrpcResponse> call, String id) {
			return null;
		}

		@Override
		public Version getImplementationVersion() {
			return this.version;
		}

		@Override
		protected DummyEnergyScheduler self() {
			return this;
		}

	}

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		create(clock, Version.V2_ENERGY_SCHEDULABLE, //
				new DummyManagedSymmetricEss("ess0") //
						.withSoc(60) //
						.withCapacity(10000), //
				new DummyTimedata("timedata0")) //
				.deactivate();
	}

	/**
	 * Creates a {@link TimeOfUseTariffControllerImpl} instance.
	 * 
	 * @param clock    a {@link Clock}
	 * @param version  the {@link EnergyScheduler} implementation {@link Version}
	 * @param ess      the {@link SymmetricEss}
	 * @param timedata the {@link Timedata}
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create(Clock clock, Version version, SymmetricEss ess,
			Timedata timedata) throws Exception {
		var componentManager = new DummyComponentManager(clock);
		var sum = new DummySum();
		var timeOfUseTariff = DummyTimeOfUseTariffProvider.empty(clock);
		var energyScheduler = new DummyEnergyScheduler(version);

		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("energyScheduler", energyScheduler) //
				.addReference("timedata", timedata) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("sum", sum) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEnabled(false) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setEssMaxChargePower(5000) //
						.setMaxChargePowerFromGrid(10000) //
						.build()) //
				.next(new TestCase());
		return sut;
	}
}
