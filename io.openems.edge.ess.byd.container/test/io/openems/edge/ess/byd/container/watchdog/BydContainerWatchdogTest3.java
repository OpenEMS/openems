package io.openems.edge.ess.byd.container.watchdog;

import java.util.Collection;

import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.byd.container.EssFeneconBydContainer;

public class BydContainerWatchdogTest3 {

	@SuppressWarnings("all")
	private static class MyEssConfig extends AbstractComponentConfig
			implements io.openems.edge.ess.byd.container.Config {

		private final String id;
		// private final String ess_id;
		private final Boolean readonly;

		public MyEssConfig(String id, boolean readonly) {
			super(Config.class, id);
			this.id = id;
			this.readonly = readonly;

		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public boolean readonly() {
			return this.readonly;
		}

		@Override
		public String modbus_id0() {
			// TODO Auto-generated method stub
			return "modbus1";
		}

		@Override
		public String modbus_id1() {
			// TODO Auto-generated method stub
			return "modbus1";
		}

		@Override
		public String modbus_id2() {
			// TODO Auto-generated method stub
			return "modbus1";
		}

		@Override
		public String Modbus_target() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String modbus1_target() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String modbus2_target() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@SuppressWarnings("all")
	private static class MyCtrlConfig extends AbstractComponentConfig
			implements io.openems.edge.ess.byd.container.watchdog.Config {

		private final String id;
		private final String ess_id;

		public MyCtrlConfig(String id, String ess_id) {
			super(Config.class, ess_id);
			this.id = id;
			this.ess_id = ess_id;
		}

		@Override
		public String ess_id() {
			// TODO Auto-generated method stub
			return this.ess_id;
		}

	}

	private static class SimulatedComp implements OpenemsComponent {
		protected final IntegerReadChannel watchDog;

		public IntegerReadChannel getWatchDog() {
			return watchDog;
		}

		public SimulatedComp() {
			this.watchDog = BydContainerWatchdog.ChannelId.WATCHDOG.doc().createChannelInstance(this,
					BydContainerWatchdog.ChannelId.WATCHDOG);
		}

		@Override
		public String id() {
			return "ctrlBydContainerWatchdog0";
		}

		@Override
		public String alias() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isEnabled() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public ComponentContext getComponentContext() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Channel<?> _channel(String channelName) {
			// TODO Auto-generated method stub
			return this.getWatchDog();
		}

		@Override
		public Collection<Channel<?>> channels() {
			// TODO Auto-generated method stub
			return null;
		}

	}

	@Test
	public void test() throws Exception {

		// SimulatedComp , MyCtrlConfig, MyEssConfig
		// -------------------------------------------------------------

		// initialize the controller
		BydContainerWatchdog ctrl = new BydContainerWatchdog();

		// Adding the referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		ctrl.componentManager = componentManager;

		// Controller config
		MyCtrlConfig myCtrlconfig = new MyCtrlConfig("ctrlBydContainerWatchdog0", "ess0");

		// Simulated controller channel address
		SimulatedComp wd = new SimulatedComp();
		componentManager.addComponent(wd);
		wd.watchDog.setNextValue(1);

		EssFeneconBydContainer ess = new EssFeneconBydContainer();
		//DummyConfigurationAdmin cm = new DummyConfigurationAdmin();
		//ess.cm = cm;
		// Ess config
//		MyEssConfig myEssConfig = new MyEssConfig("ess0", false);
//		ess.config = myEssConfig;
//		ess.activate(null, myEssConfig);
//		ess.activate(null, myEssConfig);
//		componentManager.addComponent(ess);

		// Set the conrtoller config
		ctrl.config = (myCtrlconfig);
		ChannelAddress watchDog = new ChannelAddress("ctrlBydContainerWatchdog0", "Watchdog");
		ChannelAddress isTimeout = new ChannelAddress("ctrlBydContainerWatchdog0", "istimeout");

		new ControllerTest(ctrl, componentManager, wd, ess).next(new TestCase()//
				
		        .output(isTimeout, 0))
		.next(new TestCase() //
				.input(watchDog, 0)
				.output(isTimeout, 1))
		.next(new TestCase() //
				.input(watchDog, 0)
				.output(isTimeout, 1))
		.next(new TestCase() //
				.input(watchDog, 0)
				.output(isTimeout, 1))
		.next(new TestCase() //
				.input(watchDog, 0)
				.output(isTimeout, 1))
		.next(new TestCase() //
				.input(watchDog, 1)
				.output(isTimeout, 0))
		.next(new TestCase() //
				.output(isTimeout, 0))
		.next(new TestCase() //
				.input(watchDog, 0)
				.output(isTimeout, 1))
				.run();

	}

}
