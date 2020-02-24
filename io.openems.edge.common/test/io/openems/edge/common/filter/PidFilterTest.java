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
		PidFilter p = new PidFilter(0.3, 0.3, 0.1);
		p.setLimits(-100, 100);
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
		t(p, 0, 100, 30);
		t(p, 0, 100, 60);
		t(p, 18.9636167648567, 100, 82.4145532940573);
		t(p, 44.9035582677583, 100, 98.2458533399253);
		t(p, 68.6150294059977, 100, 100);
		t(p, 87.3452823896626, 100, 100);
		t(p, 95.3445895573267, 100, 100);
		t(p, 98.2873702079256, 100, 100);
		t(p, 99.3699587091581, 100, 100);
		t(p, 99.7682207620101, 100, 100);
		t(p, 99.9147331834531, 100, 100);
		t(p, 99.9686320911783, 100, 100);
		t(p, 99.9884603912319, 100, 100);
		t(p, 99.9957548151751, 100, 100);
		t(p, 99.9984382837789, 100, 100);
		t(p, 99.9994254767093, 100, 100);
		t(p, 99.9997886446929, 100, 100);
		t(p, 99.9999222467277, 100, 100);
		t(p, 99.9999713961696, 100, 100);
		t(p, 99.9999894772389, 100, 100);
		t(p, 99.9999961288925, 100, 100);
		t(p, 99.9999985758991, 100, 100);
		t(p, 99.9999994761026, 100, 100);
		t(p, 99.9999998072689, 100, 100);
		t(p, 99.9999999290982, 100, 100);
		t(p, 99.9999999739167, 100, 100);
		t(p, 99.9999999904045, 100, 100);
		t(p, 99.99999999647, 100, 100);
		t(p, 99.9999999987014, 100, 100);

	}

	private void t(PidFilter p, double input, double output, double expectedOutput) {
		System.out.println(String.format("%10.3f  %10.3f  %10.3f", input, output, expectedOutput));
		assertEquals(expectedOutput, p.applyPidFilter(input, output), 0.001);
	}

}
