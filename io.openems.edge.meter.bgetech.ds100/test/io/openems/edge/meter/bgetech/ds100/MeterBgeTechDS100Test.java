/*
 *   OpenEMS Meter B+G E-Tech DS100 bundle
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program and the accompanying materials are made available under
 *   the terms of the Eclipse Public License v2.0 which accompanies this
 *   distribution, and is available at
 *
 *   https://www.eclipse.org/legal/epl-2.0
 *
 *   SPDX-License-Identifier: EPL-2.0
 *
 */

package io.openems.edge.meter.bgetech.ds100;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterBgeTechDS100Test {

    @Test
    public void test() throws Exception {
    	new ComponentTest(new MeterBgeTechDS100Impl())
            .addReference("cm", new DummyConfigurationAdmin())
            .addReference("setModbus", new DummyModbusBridge("modbus0"))
            .activate(MeterBgeTechDS100TestConfig.create()
                .setId("component0")
                .setModbusId("modbus0")
                .setType(MeterType.GRID)
                .setInvert(false)
                .build())
            .next(new TestCase())
            .deactivate();
    }

}
