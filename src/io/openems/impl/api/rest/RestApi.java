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
package io.openems.impl.api.rest;

import io.vertx.core.AbstractVerticle;

public class RestApi extends AbstractVerticle {

	// private static Logger log = LoggerFactory.getLogger(RestApi.class);
	//
	// @Override public void start(Future<Void> fut) throws Exception {
	// // Create a router object.
	// Router router = Router.router(vertx);
	//
	// router.get("/rest/thing/:thingId/channel/:channelId/current").handler(this::getThingChannelValue);
	//
	// // Bind "/" to our hello message - so we are still compatible.
	// router.route("/").handler(routingContext -> {
	// HttpServerResponse response = routingContext.response();
	// response.putHeader("content-type", "text/html").end(
	// "<h1>Welcome to OpenEMS REST-Api</h1><p>Why don't you try reading <a
	// href='/rest/thing/ess0/channel/Soc/current'>current SOC of ess0</a>?");
	// });
	//
	// // Create the HTTP server and pass the "accept" method to the request handler.
	// vertx.createHttpServer().requestHandler(router::accept).listen(
	// // Retrieve the port from the configuration,
	// // default to 8080.
	// config().getInteger("http.port", 8081), result -> {
	// if (result.succeeded()) {
	// fut.complete();
	// } else {
	// fut.fail(result.cause());
	// }
	// });
	// }

	// private void getThingChannelValue(RoutingContext routingContext) {
	// String thingId = routingContext.request().getParam("thingId");
	// String channelId = routingContext.request().getParam("channelId");
	// if (thingId == null || channelId == null) {
	// routingContext.response().setStatusCode(400).end();
	// } else {
	// log.info("Thing: " + thingId + ", Channel: " + channelId);
	// try {
	// Object value = databus.getValue(thingId, channelId);
	// routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
	// .end(Json.encodePrettily(value));
	// } catch (InvalidValueException | NullPointerException e) {
	// routingContext.response().setStatusCode(404).end();
	// }
	// }
	// routingContext.response().setStatusCode(204).end();
	// }
}
