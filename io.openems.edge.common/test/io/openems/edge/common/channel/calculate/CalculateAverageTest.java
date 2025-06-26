package io.openems.edge.common.channel.calculate;

import org.junit.Assert;
import org.junit.Test;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public class CalculateAverageTest {

	private enum TestChannelId implements ChannelId {
		TEST_INTEGER_CHANNEL(Doc.of(OpenemsType.INTEGER)), //
		TEST_DOUBLE_CHANNEL(Doc.of(OpenemsType.DOUBLE)), //
		TEST_FLOAT_CHANNEL(Doc.of(OpenemsType.FLOAT)), //
		TEST_SHORT_CHANNEL(Doc.of(OpenemsType.SHORT)), //
		TEST_LONG_CHANNEL(Doc.of(OpenemsType.LONG)),;

		private final Doc doc;

		TestChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Test
	public void testIntegerAverage() {
		Channel<Integer> channel = createChannel(TestChannelId.TEST_INTEGER_CHANNEL);
		CalculateAverage calculateAverage = new CalculateAverage();

		setNextAndProcessChannelValue(channel, 2);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, 4);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, 6);
		calculateAverage.addValue(channel);

		Integer average = calculateAverage.calculateRounded();

		Assert.assertNotNull(average);
		Assert.assertEquals(4, (int) average);
	}

	@Test
	public void testDoubleAverage() {
		Channel<Double> channel = createChannel(TestChannelId.TEST_DOUBLE_CHANNEL);
		CalculateAverage calculateAverage = new CalculateAverage();

		setNextAndProcessChannelValue(channel, 1.0d);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, 4.0d);
		calculateAverage.addValue(channel);

		Integer averageRounded = calculateAverage.calculateRounded();
		Assert.assertNotNull(averageRounded);
		Assert.assertEquals(3, (int) averageRounded);

		Double average = calculateAverage.calculate();
		Assert.assertNotNull(average);
		Assert.assertEquals(2.5d, average, 0.01);
	}

	@Test
	public void testFloatAverage() {
		Channel<Float> channel = createChannel(TestChannelId.TEST_FLOAT_CHANNEL);
		CalculateAverage calculateAverage = new CalculateAverage();

		setNextAndProcessChannelValue(channel, 1.0f);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, 4.0f);
		calculateAverage.addValue(channel);

		Integer averageRounded = calculateAverage.calculateRounded();
		Assert.assertNotNull(averageRounded);
		Assert.assertEquals(3, (int) averageRounded);

		Double average = calculateAverage.calculate();
		Assert.assertNotNull(average);
		Assert.assertEquals(2.5d, average, 0.01);
	}

	@Test
	public void testShortAverage() {
		Channel<Short> channel = createChannel(TestChannelId.TEST_SHORT_CHANNEL);
		CalculateAverage calculateAverage = new CalculateAverage();

		setNextAndProcessChannelValue(channel, (short) 2);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, (short) 4);
		calculateAverage.addValue(channel);

		Integer averageRounded = calculateAverage.calculateRounded();
		Assert.assertNotNull(averageRounded);
		Assert.assertEquals(3, (int) averageRounded);

		Double average = calculateAverage.calculate();
		Assert.assertNotNull(average);
		Assert.assertEquals(3d, average, 0.01);
	}

	@Test
	public void testLongAverage() {
		Channel<Long> channel = createChannel(TestChannelId.TEST_LONG_CHANNEL);
		CalculateAverage calculateAverage = new CalculateAverage();

		setNextAndProcessChannelValue(channel, 1L);
		calculateAverage.addValue(channel);
		setNextAndProcessChannelValue(channel, 4L);
		calculateAverage.addValue(channel);

		Integer averageRounded = calculateAverage.calculateRounded();
		Assert.assertNotNull(averageRounded);
		Assert.assertEquals(3, (int) averageRounded);

		Double average = calculateAverage.calculate();
		Assert.assertNotNull(average);
		Assert.assertEquals(2.5d, average, 0.01);
	}

	private static <T extends Number> void setNextAndProcessChannelValue(Channel<T> channel, T value) {
		channel.setNextValue(value);
		channel.nextProcessImage();
	}

	private static <T extends Number> Channel<T> createChannel(TestChannelId channelId) {
		return channelId.doc().createChannelInstance(null, channelId);
	}
}