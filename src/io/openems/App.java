/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.core.databus.Databus;
import io.openems.core.utilities.ThingFactory;
import io.openems.demo.Demo;
import io.openems.demo.DemoFems7WithMeter;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS started");

		Demo demo = new DemoFems7WithMeter();
		// Demo demo = new DemoJanitza();

		// Demo demo = new DemoFems7();

		JsonObject config = demo.getConfig();
		log.info("OpenEMS config loaded");

		Databus databus = ThingFactory.getFromConfig(config);
		log.info("OpenEMS Databus initialized");
		// databus.printAll();

		Thread.sleep(3000);

		log.info("ess0/soc: " + databus.getValue("ess0", "Soc"));
	}
}
