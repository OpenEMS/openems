package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ActivePowerConstraintWithPidTest {
	
	@Test
	public void testSwitchBetweenChargeDischarge() throws OpenemsNamedException {
		var constraint = new ActivePowerConstraintWithPid();
		var ess = new DummyManagedSymmetricEss("bla");
		ess.setPower(new DummyPower(0.3, 0.3, 0.1));
		var actualPower = this.setAndVerifyPower(constraint, ess, 0, 10_000, 3_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 4_800);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 6_480);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 7_548);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 8_345);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 8_868);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_235);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_481);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_648);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_762);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_839);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_891);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_926);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_950);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_966);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_977);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_984);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_989);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_993);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_995);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_997);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_998);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_998);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9_999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, 4000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, 400);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -2960);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -5096);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -6690);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -7737);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -8471);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -8961);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9297);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9523);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9677);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9781);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9852);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9900);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9932);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9954);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9969);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9979);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9986);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9990);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9993);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9996);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9997);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9998);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -10_000, -10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, -3_999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, -400);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 2960);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 5096);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 6690);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 7737);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 8471);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 8961);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9297);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9523);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9677);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9781);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9852);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9900);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9932);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9954);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9969);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9979);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9985);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9990);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9993);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9996);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9997);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9998);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 9999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 10_000, 10_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 7_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 5_200);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 3_520);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 2_452);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 1655);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 1131);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 765);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 519);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 352);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 238);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 161);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 109);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 74);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 50);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 34);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 23);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 16);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 11);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 7);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 5);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 3);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 2);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 2);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 1);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 1);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
	}
	
	@Test
	public void testBetweenChargeNoCharge() throws OpenemsNamedException {
		var constraint = new ActivePowerConstraintWithPid();
		var ess = new DummyManagedSymmetricEss("bla");
		ess.setPower(new DummyPower(0.3, 0.3, 0.1));
		var actualPower = this.setAndVerifyPower(constraint, ess, 100_000, 100_000, 100_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
	}
	
	@Test
	public void testBetweenDischargeNoCharge() throws OpenemsNamedException {
		var constraint = new ActivePowerConstraintWithPid();
		var ess = new DummyManagedSymmetricEss("bla");
		ess.setPower(new DummyPower(0.3, 0.3, 0.1));
		var actualPower = this.setAndVerifyPower(constraint, ess, -100_000, -100_000, -100_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, 0);
	}
	
	@Test
	public void testMaxLowerThanMin() throws OpenemsNamedException {
		var constraint = new ActivePowerConstraintWithPid();
		var ess = new DummyManagedSymmetricEss("bla");
		int minPower = 5;
		ess.setPower(new DummyPower(0.3, 0.3, 0.1) {
			@Override
			public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return minPower;
			}
			
			@Override
			public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return minPower - 1;
			}
		});
		var actualPower = this.setAndVerifyPower(constraint, ess, 0, -100_000, minPower);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, minPower);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, minPower, minPower);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, minPower);
	}
	
	@Test
	public void testOnlyDischargeAllowed() throws OpenemsNamedException {
		var constraint = new ActivePowerConstraintWithPid();
		var ess = new DummyManagedSymmetricEss("bla");
		ess.setPower(new DummyPower(0.3, 0.3, 0.1) {
			@Override
			public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return -1_000;
			}
			
			@Override
			public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
				return -500;
			}
		});
		var actualPower = this.setAndVerifyPower(constraint, ess, 0, -2_000, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -600);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -710);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -796);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -860);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -904);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -935);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -955);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -970);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -979);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -986);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -991);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -994);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -996);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -997);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -998);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -999);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, -100_000, -1_000);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -849);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -760);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -676);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -622);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -583);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -556);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -538);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -526);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -518);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -512);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -508);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -506);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -504);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -502);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -502);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -501);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -501);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 0, -500);
		actualPower = this.setAndVerifyPower(constraint, ess, actualPower, 100_000, -500);
	}

	private int setAndVerifyPower(ActivePowerConstraintWithPid constraint, DummyManagedSymmetricEss ess,
			int currentActivePower, int targetActivePower, int expectedActivePower) throws OpenemsNamedException {
		ess._setActivePower(currentActivePower);
		ess.getActivePowerChannel().nextProcessImage();
		IntegerWriteChannel activePowerChannel = (IntegerWriteChannel) ess.getSetActivePowerEqualsChannel();
		constraint.accept(ess, targetActivePower);
		int actualPower = activePowerChannel.getNextWriteValue().get().intValue();
		assertEquals(expectedActivePower, actualPower);
		return actualPower;
	}

}
