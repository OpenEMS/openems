package io.openems.edge.app.hardware;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.hardware.KMtronic8Channel.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a App for KMtronic 8-Channel Relay.
 *
 * <pre>
  {
    "appId":"App.Hardware.KMtronic8Channel",
    "alias":"FEMS Relais 8-Kanal",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"IO_ID": "io0",
    	"MODBUS_ID": "modbus10",
    	"IP": "192.168.1.199"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@Component(name = "App.Hardware.KMtronic8Channel")
public class KMtronic8Channel extends AbstractOpenemsAppWithProps<KMtronic8Channel, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, KMtronic8Channel, Parameter.BundleParameter> {
		// Component-IDs
		IO_ID(AppDef.componentId("io1")), //
		MODBUS_ID(AppDef.componentId("modbus10")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		IP(AppDef.copyOfGeneric(CommunicationProps.ip(), //
				def -> def.setTranslatedDescriptionWithAppPrefix(".ip.description") //
						.setDefaultValue("192.168.1.199") //
						.setRequired(true))), //
		CHECK(AppDef.copyOfGeneric(CommonProps.installationHint(//
				(app, property, l, parameter) -> TranslationUtil.getTranslation(parameter.bundle, //
						"App.Hardware.KMtronic8Channel.installationHint")))), //
		;

		private final AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KMtronic8Channel, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KMtronic8Channel, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KMtronic8Channel>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public KMtronic8Channel(//
			@Reference ComponentManager componentManager, //
			ComponentContext context, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);

			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var ioId = this.getId(t, p, Property.IO_ID);

			final var comp = Lists.newArrayList(//
					new EdgeConfig.Component(ioId, alias, "IO.KMtronic", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.build()), //
					new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(comp)) //
					.throwingOnlyIf(ip.startsWith("192.168.1."),
							b -> b.addTask(Tasks.staticIp(new InterfaceConfiguration("eth0") //
									.addIp("Relay", "192.168.1.198/28")))) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-relais/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HARDWARE };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected KMtronic8Channel getApp() {
		return this;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8Y"
				+ "QUAAAAJcEhZcwAACxMAAAsTAQCanBgAAAY9aVRYdFhNTDpjb20uYWRvYmUueG1wAAAAAAA8P3hwYWNrZXQgYmVnaW49Iu+7vyIga"
				+ "WQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pg0KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0a"
				+ "z0iQWRvYmUgWE1QIENvcmUgNi4wLWMwMDIgMTE2LjE2NDc2NiwgMjAyMS8wMi8xOS0yMzoxMDowNyAgICAgICAgIj4NCiAgPHJkZ"
				+ "jpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4NCiAgICA8cmRmOkRlc"
				+ "2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJod"
				+ "HRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc"
				+ "2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6L"
				+ "y9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob"
				+ "3AgMjEuMiAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDIwLTAxLTMxVDEwOjQzOjU2KzAxOjAwIiB4bXA6TW9kaWZ5RGF0Z"
				+ "T0iMjAyMS0wNi0xNFQxMTowNDozMSswMjowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMS0wNi0xNFQxMTowNDozMSswMjowMCIgZ"
				+ "GM6Zm9ybWF0PSJpbWFnZS9wbmciIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiIHBob3Rvc2hvcDpJQ0NQcm9maWxlPSJzUkdCIElFQ"
				+ "zYxOTY2LTIuMSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDozZGMxOGQyMS0yMGI1LTg4NDMtOTYyYS0yZDU5Y2MwODcyYjgiI"
				+ "HhtcE1NOkRvY3VtZW50SUQ9ImFkb2JlOmRvY2lkOnBob3Rvc2hvcDo2ZDE3NzZlZC1iZDU3LWIwNDUtOGNkYi1jMmQ1NGQzYTE4Y"
				+ "zEiIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDplOTg1N2Q4Zi04Yjk2LTUxNDktYjYyZC0xMTZhNGIyZTBkZDMiP"
				+ "g0KICAgICAgPHhtcE1NOkhpc3Rvcnk+DQogICAgICAgIDxyZGY6U2VxPg0KICAgICAgICAgIDxyZGY6bGkgc3RFdnQ6YWN0aW9uP"
				+ "SJjcmVhdGVkIiBzdEV2dDppbnN0YW5jZUlEPSJ4bXAuaWlkOmU5ODU3ZDhmLThiOTYtNTE0OS1iNjJkLTExNmE0YjJlMGRkMyIgc"
				+ "3RFdnQ6d2hlbj0iMjAyMC0wMS0zMVQxMDo0Mzo1NiswMTowMCIgc3RFdnQ6c29mdHdhcmVBZ2VudD0iQWRvYmUgUGhvdG9zaG9wI"
				+ "DIxLjIgKFdpbmRvd3MpIiAvPg0KICAgICAgICAgIDxyZGY6bGkgc3RFdnQ6YWN0aW9uPSJzYXZlZCIgc3RFdnQ6aW5zdGFuY2VJR"
				+ "D0ieG1wLmlpZDozZGMxOGQyMS0yMGI1LTg4NDMtOTYyYS0yZDU5Y2MwODcyYjgiIHN0RXZ0OndoZW49IjIwMjEtMDYtMTRUMTE6M"
				+ "DQ6MzErMDI6MDAiIHN0RXZ0OnNvZnR3YXJlQWdlbnQ9IkFkb2JlIFBob3Rvc2hvcCAyMS4yIChXaW5kb3dzKSIgc3RFdnQ6Y2hhb"
				+ "mdlZD0iLyIgLz4NCiAgICAgICAgPC9yZGY6U2VxPg0KICAgICAgPC94bXBNTTpIaXN0b3J5Pg0KICAgIDwvcmRmOkRlc2NyaXB0a"
				+ "W9uPg0KICA8L3JkZjpSREY+DQo8L3g6eG1wbWV0YT4NCjw/eHBhY2tldCBlbmQ9InIiPz4YiCvaAAA7rElEQVR4Xu3dB2Cb1b028"
				+ "EdbsuU94njb2ctZkAFZzLBXWlbZ3BbovG1v6e36Cvd2fJe2l1IulJavBTqBsCl7JAQCWWSRPe3E8d6yLMla3/kfvbLlkWkn9LbPD"
				+ "xRLR++W3r/OOe95zwERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERE"
				+ "RERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERE"
				+ "RERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERH9r2Ay/hKRM"
				+ "n7M2EVBf7fxanBFpcVY/v6K5cZLOoUYsIgSTPv8wiiSzQgc8sBkNSNtXqE6S0xofXM/TDaLephRkDsSb/3yBZ47nwKz8ZeIFKvbh"
				+ "qRxmUgak4nkKbmwZbnUwwn3tBEqLQPuihxYXDZjajrVGLCI+rFmOJE2MRfJ5emxhCjgHp+FtPG5cJalxdLoU8GARdRPxuYIzj4wA"
				+ "Sl/6wTaQzC1BjFlczbO2FUK17sqjT41DFhECUzN3bik7GxkpKXjonmLkbo3irzqJCyYfiZKSkowe8RUWLwRY2o61RiwiBJE1BmxZ"
				+ "esWdHd3Izs7GxaHFd6gD/v27UMwGET56HJ11rC+/dPCI0+U4LSvnxt1pDoxvqsAzRYP2qaYEA1HYVrRglll07E5uBcIRPH6T5fy3"
				+ "PkUMIdFlMDeoX7FIyas9e9AXaQFvj1t6N7dDm9qGK/XfYi2gAehZl+bMTmdYgxYRAnyZo+CNScJ5tJkOLKTEXab4UuOIi0nHZZRb"
				+ "nS5o4iMSt5kTE6nGAMWUQJTFIiqsyKl0wqT+i/iDQHhCCxBleaxwmazwWa1GlPTqcZy+Eny5FNPpX/04YfTZp42c5GRRIcRMVnRZ"
				+ "C1Uz1S0+JStq3v7h80Ob8+ZEQ1FYDKpF5Z4ApDjT6ucMXLRE7GEuKhPzeRUT/SEfr8f1dUHMWf2nMpbzpv8uKTR0DFgDbM///nPp"
				+ "cnJyfcUFhbdXFZWpn+R9ReeDi/66QequOgxBk3JfR2JLKfL24Xdu3e1NTQ23rtkyZJfGm/REPBMGkYvvPBCelZ29oapFVNLjSQie"
				+ "L1evPraqw/cfttt/2ok0QliwBpGf/nrXx+/+KKLb07MUcnzSCQCs9msMhJRrPxwJdrb22Pv6X8/Lf9IH/2nk0NLXKu02Zp1+iz9e"
				+ "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"
				+ "y8u1BrQ+9+titSa3l7juHXEtkNvr3GMEreXTj4WCU8xq9VSml4x8pbaP3yCrp0t+mQ0qVxQ+8pqtL1/EO1ra2DNcCLsC6Ll7Sq0r"
				+ "66BZ2M9HAUp6FivimfdYbS8U4mO1bVwlKTo+Ztf24eOtbX64SiWtABq/7gF3u3NOihIO6NQZzda3zuAzs0NCFR7dA4m2B5Aw9Ltq"
				+ "viocmBqufLz5dnciGaVM+r8ROVy3DZ4dzbrdksNz+1Uy2tSJ20AJrva3g/U9qpHbJ2pOrjKa3n4KtthTrLCp4qW3Sq9ZVkVunY0w"
				+ "5br0ttW/+R2dG5phHeXSst2qe3pQN2T2+Dd2qQCRFRPE/EH0fDsTpXraUFABSlLqk3tiw+1T3wCz4Z6vSwJ9p6NDWh+az861tTCN"
				+ "jJZbXcjzC6r3l4pFst2yL41vbIXHR/X6YejSB0jtV1NL++BRx1Tf5XaXjVPQAV82S455r79bbBmOvW21P0ltm2SCzOrY+I/2IH6p"
				+ "1TaFrW96gzyN3ZWtWyrY5HwFDB+a+hUuf7669JdDuc0XWsrJFMQK7P0fhrxtPg0QnIP8lreiycbOYoB04nE5cclTDbodAO2w3gt7"
				+ "/WkyaPfdPFl9d8HeZ447+G2V17KvMKYREuYbOD2qteSdKzbe6RjJIykI69THuqffvsZDofaHn/iD+yFgf4+ReXmNyI65ViHdWJmG"
				+ "3+JiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIi"
				+ "IiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIi"
				+ "IiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIi"
				+ "IiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIj+0QD/H+UUXKsCJe1HAAAAAElFTkSuQmCC";
	}

}
