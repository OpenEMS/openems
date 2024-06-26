package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

    private static final String CTRL_ID = "ctrl0";
    private static final Logger log = LoggerFactory.getLogger(MyControllerTest.class);

    @Test
    public void test() throws Exception {
        try {
            ControllerTest test = new ControllerTest(new ReversePowerRelayImpl());
            log.info("ControllerTest initialized.");

            MyConfig config = MyConfig.create()
                .setId(CTRL_ID)
                .setPvInverterId("pvInverter0")
                .setInputChannelAddress0Percent("inputChannel0Percent")
                .setInputChannelAddress30Percent("inputChannel30Percent")
                .setInputChannelAddress60Percent("inputChannel60Percent")
                .setInputChannelAddress100Percent("inputChannel100Percent")
                .setPowerLimit30(300)
                .setPowerLimit60(600)
                .build();
            log.info("Configuration created: {}", config);

            test.activate(config);
            log.info("ControllerTest activated with config.");

            test.next(new TestCase());
            log.info("Test case executed.");
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException occurred: ", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }
}
