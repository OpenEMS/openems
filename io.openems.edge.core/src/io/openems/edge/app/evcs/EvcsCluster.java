package io.openems.edge.app.evcs;

import java.util.EnumMap;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evcs.EvcsCluster.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a evcs cluster.
 *
 * <pre>
  {
    "appId":"App.Evcs.Cluster",
    "alias":"Multiladepunkt-Management",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_CLUSTER_ID": "evcsCluster0",
      "EVCS_IDS": [ "evcs0", "evcs1", ...]
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-multiladepunkt-eigenverbrauch-2/">https://fenecon.de/fems-2-2/fems-app-multiladepunkt-eigenverbrauch-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Evcs.Cluster")
public class EvcsCluster extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		ALIAS, //
		EVCS_CLUSTER_ID, //
		EVCS_IDS //
		;
	}

	@Activate
	public EvcsCluster(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var evcsClusterId = this.getId(t, p, Property.EVCS_CLUSTER_ID, "evcsCluster0");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));

			var ids = EnumUtils.getAsJsonArray(p, Property.EVCS_IDS);

			var components = Lists.newArrayList(new EdgeConfig.Component(evcsClusterId, alias,
					"Evcs.Cluster.PeakShaving", JsonUtils.buildJsonObject() //
							.onlyIf(t.isAddOrUpdate(), j -> j.add("evcs.ids", ids)) //
							.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.EVCS_IDS) //
								.setLabel("EVCS-IDs") //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".evcsIds.description")) //
								.setOptions(this.componentUtil.getEnabledComponentsOfStartingId("evcs").stream()
										.filter(t -> !t.id().startsWith("evcsCluster")).collect(Collectors.toList()),
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_LABEL,
										JsonFormlyUtil.SelectBuilder.DEFAULT_COMPONENT_2_VALUE)
								.isRequired(true) //
								.isMulti(true) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-multiladepunkt-eigenverbrauch-2/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8Y"
				+ "QUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAFKqSURBVHhe7Z0HoB1Hdffn7tNT75ZkFfcuy9244F4kGeMAISFAKoQWOshFkg0GU10xB"
				+ "mLAgCEm8BEnhhDiOLElA+4NbFzl3uRuWb3rvXu/89/d896582a23PLevXvPzz7aszNn2s7svJm9uzNGURRFURRFURRFURRFURRFU"
				+ "RRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFU"
				+ "RRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFURRFU"
				+ "RRFURRFURRFURRFURRFURRFURRFUZQmU4qPRSVv+SokaWGkTT16YQlO/Exgukf/I6l/RfIl88df3FF+49nQz0EzrxXiAIjHFbf0B"
				+ "y4bkKbjCJL0WnGlB3w6w2kXinouZKvjq8y0ik6CG4ErPumXVcdRIt1c/gz72fEB1l3x2Dqw/aSNJCmePj2Yv/g4Or2M1IPZjc5/Y"
				+ "cq9i8tLL3ohOk+NR5JmX2s4YOsA5/WEBdKPcbk1E85ToRjMCzjY+BoTU0sDknH5dCDjtnXAYdhd4nMfajhfdnlCPZh/9s50ehGp7"
				+ "yInR/4rG8j9YtPbc3H5xos3wYFExgM4fmCn4bNJ0nEEtg7SbOrRAc59+mDA6RWKwbp4Q4GrbI1sQAgvw8pz228oaGYe+uIO5i0eQ"
				+ "/3TYlJPJxkFNzB+5jQzefedzMv3PWK2rN8Yu4YsN5XKosqKp66u3PcfiGeoabV6A43IRytc24bTCpXTLIa6bHkbnX3jANe5L147D"
				+ "PDZunCl6dNN6ZB3d5Wm7P63pH6dZBbcwMjxY80ORx5oJu66YzjOKvf0mlf+9CjJMhpc9cRWIbdRx7WgvOSCP8TnnIbMh9SBnY+sO"
				+ "o7AZcPY4UCSvUTa23pSHGwLpL2MA9jnQMYn/VkvJPICFA2uvLxltBtCs68Rp9GItOy8AztO2yZLmhxXCE3/3kyHy0gODx2Iru5hZ"
				+ "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"
				+ "XINuS9+n+rm+LxR+OqnUyhkedvuhsmBXbZ6OwjZ6GtpDDI866781JvPrMi8AJluah6CuWdNMkHXuWSGjRzkNmT/FY+UnsBJuDzx0"
				+ "f90KqnYRmsfuMW8Ssmcazau+nH51iuK9pxqsOowCa7fQjHUF7WZNLNsWRpkUoNB2HZqUN6yBvMXUyeEPf0q1Cn1mWEbsu/Q8Roai"
				+ "WFvRXRYDD77+Y7p3fa18o2X1LKNlpIN7bDaDF/ZZGeTpeOx4TDcIFx6ljhrSbtWZB6bQjBv8anUOdmjKJvfxL8uPhmfDxZNL38LU"
				+ "sjyDtYNMxT4yiY7ino7jbyNIkueBgs7zbx5kPahHhz1oW4zdsrHSbWfUz1EsqBy2xU3VjZ4f1BEHMDOg8+9UQwoR6RW4XNPw44bN"
				+ "KscNpxeoRisizcUtFrZXI2e3eyGnaTjyGQNA2w9DxxOAjdnPMHJZ00xXV1fIpO/IIuvmA2v/6B8+49Sf0HsALzXrAkMVjqKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKohQIY/4/dhlY2A/x5QwAAAAASUVORK5CYII=";
	}

}
