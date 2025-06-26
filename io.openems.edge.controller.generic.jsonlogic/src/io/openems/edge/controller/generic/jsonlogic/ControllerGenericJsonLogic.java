package io.openems.edge.controller.generic.jsonlogic;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerGenericJsonLogic extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}

// TODO: once gson version 2.8.6 or higher is compatible with OSGi on Java 8: use json-logic library
// from maven instead of local file. Json-logic library on maven requires Gson 2.8.6; but Gson 2.8.6
// is not compatible with OSGi on Java 8 as it has the wrong manifest headers.
// See -> https://github.com/google/gson/issues/1601
// This is why we are using a manually compiled jar here, based on the fork at
// https://github.com/sfeilmeier/json-logic-java
//
// To revert back to official version, add to pom.xml:
//<dependency>
//	<groupId>io.github.meiskalt7</groupId>
//	<artifactId>json-logic-java</artifactId>
//	<version>1.0.0</version>
//</dependency>
