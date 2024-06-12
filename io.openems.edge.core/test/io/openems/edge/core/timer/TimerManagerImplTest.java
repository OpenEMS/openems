package io.openems.edge.core.timer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.timer.Timer;

public class TimerManagerImplTest {

    private TimerManagerImpl tm;

    @Before
    public void setup() throws Exception {
	TimerManagerTestBundle tmTestBundle = new TimerManagerTestBundle();
	this.tm = tmTestBundle.getTimerManager();
    }

    @Test
    public void testTimerByCount() throws OpenemsException, Exception {
	Timer t = this.tm.getTimerByCount(3);
	assertFalse(t.check());
	assertFalse(t.check());
	assertFalse(t.check());
	assertTrue(t.check());
	t.reset();
	assertFalse(t.check());
    }

    @Test
    public void testTimerByCoreCycles() throws OpenemsException, Exception {
	Timer t = this.tm.getTimerByCoreCycles(3);

	assertFalse(t.check());
	this.tm.handleEvent(null);
	assertFalse(t.check());
	this.tm.handleEvent(null);
	assertFalse(t.check());
	this.tm.handleEvent(null);

	assertTrue(t.check());
	t.reset();

	this.tm.handleEvent(null);
	assertFalse(t.check());
	this.tm.handleEvent(null);
	assertFalse(t.check());
	this.tm.handleEvent(null);
	assertTrue(t.check());
	this.tm.handleEvent(null);
	assertTrue(t.check());
    }

    // TODO add test for time timer and test delay and channels also
    // Note that time timer tests would slow down build process
}
