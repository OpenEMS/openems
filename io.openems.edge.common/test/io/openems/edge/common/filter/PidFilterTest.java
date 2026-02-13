package io.openems.edge.common.filter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Use this class to test if the PID filter does what it should. Test cases can
 * be generated using the Excel file in the docs directory. Just copy the
 * contents of the Excel sheet "Unit-Test" into the testhis.t() method in this
 * file.
 */
public class PidFilterTest {

	@Before
	public void prepare() {
		System.out.println(String.format("%10s  %10s  %10s", "input", "output", "expected"));
	}

	@Test
	public void test() {
		var p = new PidFilter(0.3, 0.3, 0);
		p.setLimits(-100000, 100000);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 20000, 6000);
		this.t(p, 0, 20000, 12000);
		this.t(p, 3793, 20000, 16862);
		this.t(p, 8981, 20000, 20168);
		this.t(p, 13963, 20000, 21979);
		this.t(p, 17885, 20000, 22613);
		this.t(p, 20473, 20000, 22472);
		this.t(p, 21826, 20000, 21924);
		this.t(p, 22234, 20000, 21254);
		this.t(p, 22038, 20000, 20642);
		this.t(p, 21542, 20000, 20180);
		this.t(p, 20973, 20000, 19888);
		this.t(p, 20472, 20000, 19746);
		this.t(p, 20103, 20000, 19715);
		this.t(p, 19877, 20000, 19752);
		this.t(p, 19775, 20000, 19820);
		this.t(p, 19760, 20000, 19892);
		this.t(p, 19798, 20000, 19952);
		this.t(p, 19857, 20000, 19995);
		this.t(p, 19917, 20000, 20020);
		this.t(p, 19966, 20000, 20030);
		this.t(p, 20000, 20000, 20000);
		this.t(p, 20019, 20000, 20024);
		this.t(p, 20007, 20000, 20022);
		this.t(p, 20018, 20000, 20017);
		this.t(p, 20021, 20000, 20011);
		this.t(p, 20018, 20000, 20005);
		this.t(p, 20014, 20000, 20001);
		this.t(p, 20008, 20000, 19999);
	}

	@Test
	public void testLimits() {
		var p = new PidFilter(0.3, 0.3, 0);
		p.setLimits(-10000, 10000);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 0, 0);
		this.t(p, 0, 10000, 3000);
		this.t(p, 0, 10000, 6000);
		this.t(p, 1896, 10000, 8431);
		this.t(p, 4490, 10000, 10000);
		this.t(p, 6981, 10000, 10000);
		this.t(p, 8889, 10000, 10000);
		this.t(p, 9591, 10000, 10000);
		this.t(p, 9850, 10000, 10000);
		this.t(p, 9945, 10000, 10000);
		this.t(p, 9980, 10000, 10000);
		this.t(p, 9993, 10000, 10000);
		this.t(p, 9997, 10000, 10000);
		this.t(p, 9999, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
		this.t(p, 10000, 10000, 10000);
	}

	/**
	 * This test simulates a PID filter that is executed inside a Peak-Shaving
	 * Controller, but after a FixActivePower-Controller which is set to 1000 W.
	 * Peak-Shaving is expected to require 5000 W discharge.
	 */
	@Test
	public void testPriority() {
		var p = new PidFilter(0.3, 0.3, 0.1);

		// Cycle 1
		p.setLimits(-1000, 1000); // set by FixActivePower
		this.t(p, 1000, 5000, 1000);

		// Cycle 2
		// Limits and input value stay always at 1000 by FixActivePower
		this.t(p, 1000, 5000, 1000);

		// Cycle 3
		this.t(p, 1000, 5000, 1000);

		// Cycle 4
		this.t(p, 1000, 5000, 1000);

		// Cycle 5
		this.t(p, 1000, 5000, 1000);

		// Cycle 6
		this.t(p, 1000, 5000, 1000);

		// Cycle 7
		this.t(p, 1000, 5000, 1000);

		// Cycle 8
		p.setLimits(-9999, 9999); // disable FixActivePower
		this.t(p, 1000, 5000, 1200);

		// Cycle 9
		this.t(p, 1000, 5000, 2400);

		// Cycle 10
		this.t(p, 1000, 5000, 3600);
	}

	@Test
	public void testDirectionChange() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-10000, 10000);
		
		// Discharge
		t(p, 0, 3000, 900);
		t(p, 0, 3000, 1800);
		t(p, 0, 3000, 2700);
		t(p, 569, 3000, 3372);
		t(p, 1347, 3000, 3847);
		t(p, 2202, 3000, 4079);
		t(p, 2942, 3000, 4108);
		t(p, 3514, 3000, 3971);
		t(p, 3871, 3000, 3731);
		t(p, 4021, 3000, 3445);
		t(p, 3989, 3000, 3167);
		t(p, 3826, 3000, 2932);
		t(p, 3585, 3000, 2764);
		t(p, 3321, 3000, 2670);
		t(p, 3075, 3000, 2646);
		t(p, 2878, 3000, 2678);
		t(p, 2747, 3000, 2747);
		t(p, 2683, 3000, 2835);
		t(p, 2680, 3000, 2925);
		t(p, 2722, 3000, 3004);
		t(p, 2793, 3000, 3063);
		t(p, 2876, 3000, 3099);
		t(p, 2957, 3000, 3113);
		t(p, 3024, 3000, 3107);
		t(p, 3071, 3000, 3087);
		t(p, 3098, 3000, 3060);
		t(p, 3104, 3000, 3031);
		t(p, 3093, 3000, 3005);
		t(p, 3072, 3000, 2984);

		// Charge
		t(p, 2900, -3000, 1229);
		t(p, 2600, -3000, -438);
		t(p, 1800, -3000, -1828);
		t(p, 900, -3000, -2988);
		t(p, 10, -3000, -3892);
		t(p, -569, -3000, -4652);
		t(p, -1347, -3000, -5128);
		t(p, -2202, -3000, -5360);
		t(p, -2942, -3000, -5389);
		t(p, -3514, -3000, -5252);
		t(p, -3871, -3000, -5012);
		t(p, -4021, -3000, -4726);
		t(p, -3989, -3000, -4448);
		t(p, -3826, -3000, -4213);
		t(p, -3585, -3000, -4045);
		t(p, -3321, -3000, -3951);
		t(p, -3075, -3000, -3927);
		t(p, -2878, -3000, -3959);
		t(p, -2747, -3000, -4028);
		t(p, -2683, -3000, -4116);
		t(p, -2680, -3000, -4206);
		t(p, -2722, -3000, -4285);
		t(p, -2793, -3000, -4344);
		t(p, -2876, -3000, -4380);
		t(p, -2957, -3000, -4393);
		t(p, -3024, -3000, -4388);
		t(p, -3071, -3000, -4368);
		t(p, -3098, -3000, -4341);
		t(p, -3104, -3000, -4312);
		t(p, -3093, -3000, -4286);
		t(p, -3072, -3000, -4265);
		
		// Zero
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
	}
	
	private void t(PidFilter p, int input, int output, int expectedOutput) {
		System.out.println(String.format("%10d  %10d  %10d", input, output, expectedOutput));
		assertEquals(expectedOutput, p.applyPidFilter(input, output));
	}

}
