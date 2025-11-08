import { TranslateService } from "@ngx-translate/core";
import { EvcsComponent } from "src/app/shared/components/edge/components/evcsComponent";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

export namespace SharedConsumption {

  export function getNavigationTree(edge: Edge, config: EdgeConfig, translate: TranslateService): ConstructorParameters<typeof NavigationTree> | null {
    const evcss: EvcsComponent[] = EvcsComponent.getComponents(config, edge);
    const consumptionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));
    const heatComponents = config?.getComponentsImplementingNature("io.openems.edge.heat.api.Heat")
      .filter(component =>
        !(component.factoryId === "Controller.Heat.Heatingelement") &&
        !component.isEnabled === false);
    const sum: EdgeConfig.Component = config.getComponent("_sum");
    sum.alias = translate.instant("Edge.History.PHASE_ACCURATE");

    return new NavigationTree("consumption", { baseString: "common/consumption" }, { name: "oe-consumption", color: "warning" }, translate.instant("General.consumption"), "label", [
      new NavigationTree("details", { baseString: "details" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("Edge.History.PHASE_ACCURATE"), "label", [], null),
      getHistoryNavigationTree(edge, sum, evcss, heatComponents, consumptionMeters, translate),
    ], null).toConstructorParams();
  }

  function getHistoryNavigationTree(edge: Edge, sum: EdgeConfig.Component, evcsComponents: EdgeConfig.Component[], heatComponents: EdgeConfig.Component[], consumptionMeterComponents: EdgeConfig.Component[], translate: TranslateService): NavigationTree {
    return new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("General.HISTORY"), "label", [
      ...getHistorySingleComponentNavigationTree(edge, sum, evcsComponents, heatComponents, consumptionMeterComponents, translate),
    ], null);
  }

  function getHistorySingleComponentNavigationTree(edge: Edge, sum: EdgeConfig.Component, evcsComponents: EdgeConfig.Component[], heatComponents: EdgeConfig.Component[], consumptionMeterComponents: EdgeConfig.Component[], translate: TranslateService): NavigationTree[] {
    return [
      new NavigationTree(sum.id + "/details", { baseString: sum.id + "/details" }, { name: "stats-chart-outline", color: "warning" }, sum.alias, "label", [], null),
      ...[...evcsComponents, ...heatComponents, ...consumptionMeterComponents].map(el => (
        new NavigationTree(el.id + "/details", { baseString: el.id + "/details" }, { name: "stats-chart-outline", color: "warning" }, el.alias, "label", [
          ...(edge.roleIsAtLeast(Role.INSTALLER) ?
            [new NavigationTree(el.id + "/current-voltage", { baseString: "current-voltage" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("Edge.History.CURRENT_AND_VOLTAGE"), "label", [], null)]
            : []
          ),
        ], null))),
    ];
  }

  export function getFormlyGeneralView(config: EdgeConfig, translate: TranslateService): OeFormlyView {

    const evcss: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component =>
        !(component.factoryId == "Evcs.Cluster.SelfConsumption") &&
        !(component.factoryId == "Evcs.Cluster.PeakShaving") &&
        !(config.factories[component.factoryId].natureIds.includes("io.openems.edge.meter.api.ElectricityMeter")) &&
        !component.isEnabled == false);

    const consumptionMeters: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

    const lines: OeFormlyField[] = [];

    // Total
    lines.push({
      type: "channel-line",
      name: translate.instant("General.TOTAL"),
      channel: "_sum/ConsumptionActivePower",
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
    });

    Phase.THREE_PHASE.forEach(phase => {
      lines.push({
        type: "channel-line",
        name: translate.instant("General.phase") + " " + phase,
        indentation: TextIndentation.SINGLE,
        channel: "_sum/ConsumptionActivePower" + phase,
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
    });

    if (evcss.length > 0) {
      lines.push({
        type: "horizontal-line",
      });
    }

    // Evcss
    evcss.forEach((evcs, index) => {
      lines.push({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(evcs),
        channel: evcs.id + "/ChargePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });

      if (index < (evcss.length - 1)) {
        lines.push({ type: "horizontal-line" });
      }
    });

    if (consumptionMeters.length > 0) {
      lines.push({ type: "horizontal-line" });
    }

    // Consumptionmeters
    consumptionMeters.forEach((meter, index) => {
      lines.push({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(meter),
        channel: meter.id + "/ActivePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
      Phase.THREE_PHASE.forEach(phase => {
        lines.push({
          type: "channel-line",
          name: "Phase " + phase,
          channel: meter.id + "/ActivePower" + phase,
          indentation: TextIndentation.SINGLE,
          converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        });
      });

      if (index < (consumptionMeters.length - 1)) {
        lines.push({
          type: "horizontal-line",
        });
      }
    });

    lines.push({ type: "horizontal-line" });

    // OtherPower
    const channelsToSubscribe: ChannelAddress[] = [new ChannelAddress("_sum", "ConsumptionActivePower")];

    evcss.forEach(evcs => channelsToSubscribe.push(new ChannelAddress(evcs.id, "ChargePower")));
    consumptionMeters.forEach(meter => {
      channelsToSubscribe.push(...[new ChannelAddress(meter.id, "ActivePower")]);
    });

    lines.push({
      type: "value-from-channels-line",
      name: translate.instant("General.otherConsumption"),
      value: (currentData: CurrentData) => Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO(Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
      channelsToSubscribe: channelsToSubscribe,
    });

    lines.push({
      type: "info-line",
      name: translate.instant("Edge.Index.Widgets.phasesInfo"),
    });

    return {
      title: translate.instant("General.consumption"),
      helpKey: "REDIRECT.COMMON_CONSUMPTION",
      lines: lines,
      component: new EdgeConfig.Component(),
    };
  }

  export function getFormlyDetailsView(config: EdgeConfig, translate: TranslateService): OeFormlyView {

    const evcss: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component =>
        !(component.factoryId == "Evcs.Cluster.SelfConsumption") &&
        !(component.factoryId == "Evcs.Cluster.PeakShaving") &&
        !(config.factories[component.factoryId].natureIds.includes("io.openems.edge.meter.api.ElectricityMeter")) &&
        !component.isEnabled == false);

    const consumptionMeters: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

    const lines: OeFormlyField[] = [];

    // Total
    lines.push({
      type: "channel-line",
      name: translate.instant("General.TOTAL"),
      channel: "_sum/ConsumptionActivePower",
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
    });

    Phase.THREE_PHASE.forEach(phase => {
      lines.push({
        type: "channel-line",
        name: translate.instant("General.phase") + " " + phase,
        indentation: TextIndentation.SINGLE,
        channel: "_sum/ConsumptionActivePower" + phase,
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
    });

    if (evcss.length > 0) {
      lines.push({
        type: "horizontal-line",
      });
    }

    // Evcss
    evcss.forEach((evcs, index) => {
      lines.push({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(evcs),
        channel: evcs.id + "/ChargePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });

      if (index < (evcss.length - 1)) {
        lines.push({ type: "horizontal-line" });
      }
    });

    if (consumptionMeters.length > 0) {
      lines.push({ type: "horizontal-line" });
    }

    // Consumptionmeters
    consumptionMeters.forEach((meter, index) => {
      lines.push({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(meter),
        channel: meter.id + "/ActivePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
      Phase.THREE_PHASE.forEach(phase => {
        lines.push({
          type: "channel-line",
          name: "Phase " + phase,
          channel: meter.id + "/ActivePower" + phase,
          indentation: TextIndentation.SINGLE,
          converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        });
      });

      if (index < (consumptionMeters.length - 1)) {
        lines.push({
          type: "horizontal-line",
        });
      }
    });

    lines.push({ type: "horizontal-line" });

    // OtherPower
    const channelsToSubscribe: ChannelAddress[] = [new ChannelAddress("_sum", "ConsumptionActivePower")];

    evcss.forEach(evcs => channelsToSubscribe.push(new ChannelAddress(evcs.id, "ChargePower")));
    consumptionMeters.forEach(meter => {
      channelsToSubscribe.push(...[new ChannelAddress(meter.id, "ActivePower")]);
    });

    lines.push({
      type: "value-from-channels-line",
      name: translate.instant("General.otherConsumption"),
      value: (currentData: CurrentData) => Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO(Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
      channelsToSubscribe: channelsToSubscribe,
    });

    lines.push({
      type: "info-line",
      name: translate.instant("Edge.Index.Widgets.phasesInfo"),
    });

    return {
      title: translate.instant("General.consumption"),
      helpKey: "REDIRECT.COMMON_CONSUMPTION",
      lines: lines,
      component: new EdgeConfig.Component(),
    };
  }
}

