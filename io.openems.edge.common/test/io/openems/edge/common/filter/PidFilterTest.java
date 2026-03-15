package io.openems.edge.common.filter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Use this class to test if the PID filter does what it should. Test cases can
 * be generated using the Excel file in the docs directory. Just copy the
 * contents of the Excel sheet "Unit-Test" into the test() method in this file.
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
		t(p, 0, 0, 0);
		t(p, 0, 20000, 6000);
		t(p, 0, 20000, 12000);
		t(p, 3793, 20000, 15724);
		t(p, 8981, 20000, 17474);
		t(p, 13963, 20000, 17790);
		t(p, 17885, 20000, 17248);
		t(p, 20473, 20000, 16330);
		t(p, 21826, 20000, 15376);
		t(p, 22234, 20000, 14583);
		t(p, 22038, 20000, 14031);
		t(p, 21542, 20000, 13717);
		t(p, 20973, 20000, 13596);
		t(p, 20472, 20000, 13604);
		t(p, 20103, 20000, 13684);
		t(p, 19877, 20000, 13789);
		t(p, 19775, 20000, 13887);
		t(p, 19760, 20000, 13964);
		t(p, 19798, 20000, 14013);
		t(p, 19857, 20000, 14038);
		t(p, 19917, 20000, 14045);
		t(p, 19966, 20000, 14040);
	}

	@Test
	public void testLimits() {
		var p = new PidFilter(0.3, 0.3, 0);
		p.setLimits(-10000, 10000);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 0, 0);
		t(p, 0, 10000, 3000);
		t(p, 0, 10000, 6000);
		t(p, 1896, 10000, 7862);
		t(p, 4490, 10000, 8737);
		t(p, 6981, 10000, 8896);
		t(p, 8889, 10000, 8656);
		t(p, 9591, 10000, 8569);
		t(p, 9850, 10000, 8536);
		t(p, 9945, 10000, 8524);
		t(p, 9980, 10000, 8519);
		t(p, 9993, 10000, 8518);
		t(p, 9997, 10000, 8517);
		t(p, 9999, 10000, 8517);
		t(p, 10000, 10000, 10000);
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
		t(p, 1000, 5000, 1000);

		// Cycle 2
		// Limits and input value stay always at 1000 by FixActivePower
		t(p, 1000, 5000, 1000);

		// Cycle 3
		t(p, 1000, 5000, 1000);

		// Cycle 4
		t(p, 1000, 5000, 1000);

		// Cycle 5
		t(p, 1000, 5000, 1000);

		// Cycle 6
		t(p, 1000, 5000, 1000);

		// Cycle 7
		t(p, 1000, 5000, 1000);

		// Cycle 8
		p.setLimits(-9999, 9999); // disable FixActivePower
		t(p, 1000, 5000, 2400);

		// Cycle 9
		t(p, 1000, 5000, 3600);

		// Cycle 10
		t(p, 1000, 5000, 4800);
	}

	@Test
	public void testDirectionChange() {
		var p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-10000, 10000);

		// Discharge
		t(p, 0, 3000, 900);
		t(p, 0, 3000, 1800);
		t(p, 0, 3000, 2700);
		t(p, 569, 3000, 3202);
		t(p, 569, 3000, 3988);
		t(p, 1347, 3000, 4173);
		t(p, 2942, 3000, 3630);
		t(p, 3514, 3000, 3406);
		t(p, 3871, 3000, 3059);
		t(p, 4021, 3000, 2729);
		t(p, 3989, 3000, 2460);
		t(p, 3826, 3000, 2274);
		t(p, 3585, 3000, 2179);
		t(p, 3321, 3000, 2164);
		t(p, 3075, 3000, 2213);
		t(p, 2878, 3000, 2304);
		t(p, 2747, 3000, 2413);
		t(p, 2683, 3000, 2520);
		t(p, 2680, 3000, 2611);
		t(p, 2722, 3000, 2678);
		t(p, 2793, 3000, 2715);
		t(p, 2876, 3000, 2726);
		t(p, 2957, 3000, 2715);
		t(p, 3024, 3000, 2689);
		t(p, 3071, 3000, 2656);
		t(p, 3098, 3000, 2621);
		t(p, 3104, 3000, 2590);
		t(p, 3093, 3000, 2567);
		t(p, 3072, 3000, 2552);

		// Charge
		t(p, 2900, -3000, -1753);
		t(p, 2600, -3000, -3330);
		t(p, 900, -3000, -3850);
		t(p, -1347, -3000, -3617);
		t(p, -2202, -3000, -3739);
		t(p, -3321, -3000, -3281);
		t(p, -3075, -3000, -3469);
		t(p, -2878, -3000, -3559);
		t(p, -2876, -3000, -3578);
		t(p, -2957, -3000, -3558);
		t(p, -3024, -3000, -3532);
		t(p, -3071, -3000, -3499);
		t(p, -3098, -3000, -3463);
		t(p, -3104, -3000, -3432);
		t(p, -3093, -3000, -3409);
		t(p, -3072, -3000, -3395);

		// Zero
		t(p, -2500, 0, 0);
		t(p, -1000, 0, 0);
		t(p, 0, 0, 0);
	}

	private static void t(PidFilter p, int input, int output, int expectedOutput) {
		System.out.println(String.format("%10d  %10d  %10d", input, output, expectedOutput));
		assertEquals(expectedOutput, p.applyPidFilter(input, output));
	}

}
