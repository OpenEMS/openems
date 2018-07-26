package market.diagram.load;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class LoadDiagramTest {

	@Test
	public void test() {
		LoadDiagram d = new LoadDiagram();
		
		d.setValue(new Date(1), new Date(100), 1.0);
		d.setValue(new Date(101), new Date(200), 1.1);
		d.setValue(new Date(201), new Date(300), 1.2);
		
		Assert.assertEquals(1.0, d.getValue(new Date(1)), 0);
		Assert.assertEquals(1.0, d.getValue(new Date(100)), 0);
		Assert.assertEquals(1.1, d.getValue(new Date(101)), 0);
		Assert.assertEquals(1.1, d.getValue(new Date(200)), 0);
		Assert.assertEquals(1.2, d.getValue(new Date(201)), 0);
		Assert.assertEquals(1.2, d.getValue(new Date(300)), 0);
		Assert.assertEquals(1.0, d.getAvg(new Date(1), new Date(100)), 0);
		Assert.assertEquals(1.1, d.getAvg(new Date(101), new Date(200)), 0);
		Assert.assertEquals(1.2, d.getAvg(new Date(201), new Date(300)), 0);
		Assert.assertEquals(1.1, d.getAvg(new Date(1), new Date(300)), 0);
		Assert.assertEquals(0.0,  d.getAvg(new Date(1000),  new Date(2000)), 0.0);
		
		d.setValue(new Date(0), new Date(100), 2.0);
		d.setValue(new Date(0), new Date(100), 2.1);
		d.setValue(new Date(0), new Date(100), 2.2);
		d.setValue(new Date(0), new Date(100), 2.3);
		
		d.setValue(new Date(500), new Date(600), 3.0);
		
		d.setValue(new Date(25), new Date(50),4.0);
		d.setValue(new Date(75), new Date(100), 4.1);
		d.setValue(new Date(475), new Date(625), 4.2);
	}

}
