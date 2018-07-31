package market.diagram.load;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class LoadDiagramTest {

	@Test
	public void test() {
		LoadDiagram d = new LoadDiagram();

		d.setValue(new Date(1), new Date(100), new ValueDecimal(1.0));
		d.setValue(new Date(101), new Date(200), new ValueDecimal(1.1));
		d.setValue(new Date(201), new Date(300), new ValueDecimal(1.2));

		Assert.assertEquals(1.0, d.getValue(new Date(1)).doubleValue(), 0);
		Assert.assertEquals(1.0, d.getValue(new Date(100)).doubleValue(), 0);
		Assert.assertEquals(1.1, d.getValue(new Date(101)).doubleValue(), 0);
		Assert.assertEquals(1.1, d.getValue(new Date(200)).doubleValue(), 0);
		Assert.assertEquals(1.2, d.getValue(new Date(201)).doubleValue(), 0);
		Assert.assertEquals(1.2, d.getValue(new Date(300)).doubleValue(), 0);
		Assert.assertEquals(1.0, d.getAvg(new Date(1), new Date(100)).doubleValue(), 0);
		Assert.assertEquals(1.1, d.getAvg(new Date(101), new Date(200)).doubleValue(), 0);
		Assert.assertEquals(1.2, d.getAvg(new Date(201), new Date(300)).doubleValue(), 0);
		Assert.assertEquals(1.1, d.getAvg(new Date(1), new Date(300)).doubleValue(), 0);
		boolean isNull = false;
		try {
			Assert.assertEquals(0.0, d.getAvg(new Date(1000), new Date(2000)).doubleValue(), 0.0);
		} catch (NullPointerException e) {
			isNull = true;
		}
		Assert.assertTrue(isNull);

		d.setValue(new Date(0), new Date(100), new ValueDecimal(2.0));
		d.setValue(new Date(0), new Date(100), new ValueDecimal(2.1));
		d.setValue(new Date(0), new Date(100), new ValueDecimal(2.2));
		d.setValue(new Date(0), new Date(100), new ValueDecimal(2.3));

		d.setValue(new Date(500), new Date(600), new ValueDecimal(3.0));

		d.setValue(new Date(25), new Date(50), new ValueDecimal(4.0));
		d.setValue(new Date(75), new Date(100), new ValueDecimal(4.1));
		d.setValue(new Date(475), new Date(625), new ValueDecimal(4.2));

		isNull = false;
		try {
			d.getAvg(new Date(0), new Date(2000)).doubleValue();
		} catch (NullPointerException e) {
			isNull = true;
		}
		Assert.assertFalse(isNull);

		d.setValue(new Date(10001), new Date(11000), new ValueDecimal(5.0));
		d.setValue(new Date(11001), new Date(12000), new ValueDecimal(5.1));
		d.setValue(new Date(13001), new Date(14000), new ValueDecimal(5.2));

		d.erasePeriod(new Date(10501), new Date(10700));
		isNull = false;
		try {
			d.getAvg(new Date(10501), new Date(10700)).doubleValue();
		} catch (NullPointerException e) {
			isNull = true;
		}
		Assert.assertTrue(isNull);
		d.erasePeriod(new Date(10801), new Date(11700));

		isNull = false;
		try {
			d.getAvg(new Date(10801), new Date(11700)).doubleValue();
		} catch (NullPointerException e) {
			isNull = true;
		}
		Assert.assertTrue(isNull);
		d.erasePeriod(new Date(9000), new Date(20000));

		isNull = false;
		try {
			d.getAvg(new Date(8000), new Date(30000)).doubleValue();
		} catch (NullPointerException e) {
			isNull = true;
		}
		Assert.assertTrue(isNull);
		LoadDiagram c = d.getCopy();
		Assert.assertTrue(d.toString().equals(c.toString()));
		d.setValue(new Date(10001), new Date(11000), new ValueDecimal(6.0));
		Assert.assertFalse(d.toString().equals(c.toString()));
		d.setValue(new Date(11001), new Date(12000), new ValueDecimal(6.0));
		d.setValue(new Date(12002), new Date(13000), new ValueDecimal(6.0));

		d.setIterator(new Date(0));
		Assert.assertEquals(2.3, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(4, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(2.3, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(4.1, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(1.1, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(1.2, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(4.2, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(6, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertEquals(6, d.getNext().getValue().getDecimalDouble(), 0);
		Assert.assertTrue(d.getNext() == null);
	}

}
