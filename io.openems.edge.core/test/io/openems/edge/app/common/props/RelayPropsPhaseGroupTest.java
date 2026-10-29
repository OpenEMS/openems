package io.openems.edge.app.common.props;

import static io.openems.edge.app.common.props.RelayProps.phaseGroup;
import static io.openems.edge.app.common.props.RelayProps.relayContactDef;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.edge.app.TestC;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ComponentUtil.RelayContactInfo;
import io.openems.edge.core.appmanager.ComponentUtil.RelayInfo;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;

class RelayPropsPhaseGroupTest {

	private TestC testCApp;

	@BeforeEach
	void setUp() throws Exception {
		new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(this.testCApp = Apps.testC(t));
		});
	}

	@Test
	void conditionalContactsIgnoreUnsetValuesAndShowErrorsOnActiveContact() {
		final var parameter = new BundleParameter(AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));
		final var field = Property.PHASE_GROUP.def().getField()
				.get(this.testCApp, Property.PHASE_GROUP, Language.DEFAULT, parameter).build();
		final var group = field.getAsJsonArray("fieldGroup").get(0).getAsJsonObject();
		final var validators = group.getAsJsonObject("validators");
		final var firstValidation = validators.getAsJsonObject("RELAY_1_VALIDATION");
		final var fields = group.getAsJsonArray("fieldGroup");

		assertEquals("RELAY_1", firstValidation.get("errorPath").getAsString());
		assertTrue(firstValidation.get("expressionString").getAsString().contains(".filter(i => !!i)"));
		assertTrue(fields.get(0).getAsJsonObject().getAsJsonObject("validation").get("show").getAsBoolean());
	}

	@Test
	void relayContactFieldIgnoresShortPreferredRelayDefinitions() {
		final var relay = new RelayInfo("relay0", "", 8, List.of(//
				new RelayContactInfo("relay0/InputOutput0", "", 0, List.of(), List.of()), //
				new RelayContactInfo("relay0/InputOutput1", "", 1, List.of(), List.of()), //
				new RelayContactInfo("relay0/InputOutput2", "", 2, List.of(), List.of()) //
		));
		final var parameter = new TestRelayParameter(new String[] { null, null, null }, List.of(relay),
				List.of(PreferredRelay.of(8, new int[] { 1, 2 })));

		assertDoesNotThrow(() -> RelayProperty.CONTACT.def() //
				.getField() //
				.get(this.testCApp, RelayProperty.CONTACT, Language.DEFAULT, parameter) //
				.build());
	}

	private record TestRelayParameter(//
			String[] preferredRelays, //
			List<RelayInfo> allRelays, //
			List<PreferredRelay> defaultRelays //
	) implements BundleProvider, RelayProps.RelayContactInformationProvider {

		@Override
		public ResourceBundle bundle() {
			return AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT);
		}

		@Override
		public RelayProps.RelayContactInformation relayContactInformation() {
			return new RelayProps.RelayContactInformation(this.preferredRelays, this.allRelays, this.defaultRelays);
		}
	}

	private enum RelayProperty implements Type<RelayProperty, TestC, TestRelayParameter> {
		CONTACT(relayContactDef(3));

		private final AppDef<? super TestC, ? super RelayProperty, ? super TestRelayParameter> def;

		private RelayProperty(AppDef<? super TestC, ? super RelayProperty, ? super TestRelayParameter> def) {
			this.def = def;
		}

		@Override
		public Type<RelayProperty, TestC, TestRelayParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestC, ? super RelayProperty, ? super TestRelayParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestC>, TestRelayParameter> getParamter() {
			return t -> new TestRelayParameter(new String[] { null, null, null }, List.of(), List.of());
		}
	}

	private enum Property implements Type<Property, TestC, BundleParameter> {
		RELAY_1(AppDef.of().setField(JsonFormlyUtil::buildInputFromNameable)), //
		RELAY_2(AppDef.of().setField(JsonFormlyUtil::buildInputFromNameable)), //
		PHASE_GROUP(phaseGroup(Map.of(RELAY_2, new BooleanExpression("model.ACTIVE")), RELAY_1, RELAY_2));

		private final AppDef<? super TestC, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super TestC, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TestC, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestC, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestC>, BundleParameter> getParamter() {
			return t -> new BundleParameter(AbstractOpenemsApp.getTranslationBundle(t.language));
		}
	}
}
