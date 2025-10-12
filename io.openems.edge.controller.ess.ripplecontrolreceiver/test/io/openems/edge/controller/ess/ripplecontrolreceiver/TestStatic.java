package io.openems.edge.controller.ess.ripplecontrolreceiver;

import static io.openems.edge.controller.ess.ripplecontrolreceiver.ControllerEssRippleControlReceiverImpl.feedInLimitFromMetaLimits;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.NO_RESTRICTION;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.SIXTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.THIRTY_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.ZERO_PERCENT;
import static io.openems.edge.controller.ess.ripplecontrolreceiver.EssRestrictionLevel.getRestrictionLevelByPriority;
import static org.junit.Assert.assertEquals;

import java.util.OptionalInt;

import org.junit.Test;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.meta.GridFeedInLimitationType;

public class TestStatic {

	@Test
	public void testFeedInLimitFromMetaLimits() {
		assertEquals(OptionalInt.empty(),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.DYNAMIC_LIMITATION, new Value<Integer>(null, null)));
		assertEquals(OptionalInt.empty(),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.NO_LIMITATION, new Value<Integer>(null, 0)));
		assertEquals(OptionalInt.empty(),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.UNDEFINED, new Value<Integer>(null, 0)));
		assertEquals(OptionalInt.empty(),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.NO_LIMITATION, new Value<Integer>(null, 6000)));
		assertEquals(OptionalInt.empty(),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.UNDEFINED, new Value<Integer>(null, 6000)));

		assertEquals(OptionalInt.of(6000),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.DYNAMIC_LIMITATION, new Value<Integer>(null, 6000)));
		assertEquals(OptionalInt.of(0),
				feedInLimitFromMetaLimits(GridFeedInLimitationType.DYNAMIC_LIMITATION, new Value<Integer>(null, 0)));

	}

	@Test
	public void testGetRestrictionLevelByPriority() {

		assertEquals(NO_RESTRICTION, getRestrictionLevelByPriority(false, false, false));

		assertEquals(ZERO_PERCENT, getRestrictionLevelByPriority(true, true, true));
		assertEquals(ZERO_PERCENT, getRestrictionLevelByPriority(true, true, false));
		assertEquals(ZERO_PERCENT, getRestrictionLevelByPriority(true, false, true));

		assertEquals(THIRTY_PERCENT, getRestrictionLevelByPriority(false, true, true));
		assertEquals(THIRTY_PERCENT, getRestrictionLevelByPriority(false, true, false));

		assertEquals(SIXTY_PERCENT, getRestrictionLevelByPriority(false, false, true));
	}
}
