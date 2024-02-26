package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;
import static io.openems.edge.controller.ess.timeofusetariff.RiskLevel.MEDIUM;

import java.time.Clock;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Optimizer;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		create();
	}

	/**
	 * Creates a {@link TimeOfUseTariffControllerImplTest} instance.
	 * 
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create() throws Exception {
		return create(Clock.systemDefaultZone());
	}

	/**
	 * Creates a {@link TimeOfUseTariffControllerImplTest} instance.
	 * 
	 * @param clock a {@link Clock}
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create(Clock clock) throws Exception {
		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("predictorManager", new DummyPredictorManager()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("timeOfUseTariff", DummyTimeOfUseTariffProvider.empty(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withSoc(60) //
						.withCapacity(10000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setEssMaxChargePower(5000) //
						.setRiskLevel(MEDIUM) //
						.build());
		return sut;
	}

	/**
	 * Gets the {@link Optimizer} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	public static Optimizer getOptimizer(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var field = TimeOfUseTariffControllerImpl.class.getDeclaredField("optimizer");
		field.setAccessible(true);
		return (Optimizer) field.get(ctrl);
	}

	/**
	 * Gets the {@link DummyPredictorManager} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	public static DummyPredictorManager getPredictorManager(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var field = TimeOfUseTariffControllerImpl.class.getDeclaredField("predictorManager");
		field.setAccessible(true);
		return (DummyPredictorManager) field.get(ctrl);
	}

	/**
	 * Gets the {@link DummyComponentManager} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	public static DummyComponentManager getComponentManager(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var field = TimeOfUseTariffControllerImpl.class.getDeclaredField("componentManager");
		field.setAccessible(true);
		return (DummyComponentManager) field.get(ctrl);
	}

	/**
	 * Gets the {@link DummyTimeOfUseTariffProvider} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	public static DummyTimeOfUseTariffProvider getTimeOfUseTariff(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var field = TimeOfUseTariffControllerImpl.class.getDeclaredField("timeOfUseTariff");
		field.setAccessible(true);
		return (DummyTimeOfUseTariffProvider) field.get(ctrl);
	}

}
