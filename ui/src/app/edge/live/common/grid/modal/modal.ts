import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { transformRcrValues } from "src/app/edge/history/common/grid/shared-grid";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Filter } from "src/app/shared/components/shared/filter";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, CurrentData, EdgeConfig, RippleControlReceiverRestrictionLevel } from "src/app/shared/shared";
import { ChartAnnotationState } from "src/app/shared/type/general";
import { Role } from "src/app/shared/type/role";
import { GridSectionComponent } from "../../../energymonitor/chart/section/grid.component";
import { LiveDataService } from "../../../livedataservice";

@Component({
  templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
  standalone: false,
  providers: [
    { provide: DataService, useClass: LiveDataService },
  ],
})
export class ModalComponent extends AbstractFormlyComponent {

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {

    // Grid-Mode
    const lines: OeFormlyField[] = [{
      type: "channel-line",
      name: translate.instant("GENERAL.OFF_GRID"),
      channel: "_sum/GridMode",
      filter: Filter.GRID_MODE_IS_OFF_GRID,
      converter: Converter.HIDE_VALUE,
    }];

    const gridMeters = Object.values(config.components).filter(component => config?.isTypeGrid(component));

    // Sum Channels (if more than one meter)
    if (gridMeters.length > 1) {
      ModalComponent.getLines(config, translate, lines);

      lines.push(
        {
          type: "channel-line",
          name: translate.instant("GENERAL.GRID_SELL_ADVANCED"),
          channel: "_sum/GridActivePower",
          converter: Converter.GRID_SELL_POWER_OR_ZERO,
        },
        {
          type: "channel-line",
          name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
          channel: "_sum/GridActivePower",
          converter: Converter.GRID_BUY_POWER_OR_ZERO,
        },
        {
          type: "horizontal-line",
        },
      );
    }


    // Individual Meters
    for (const meter of gridMeters) {
      if (gridMeters.length === 1) {
        // Two lines if there is only one meter (= same visualization as with Sum Channels)
        ModalComponent.getLines(config, translate, lines);

        lines.push(
          {
            type: "channel-line",
            name: translate.instant("GENERAL.GRID_SELL_ADVANCED"),
            channel: meter.id + "/ActivePower",
            converter: Converter.GRID_SELL_POWER_OR_ZERO,
          },
          {
            type: "channel-line",
            name: translate.instant("GENERAL.GRID_BUY_ADVANCED"),
            channel: meter.id + "/ActivePower",
            converter: Converter.GRID_BUY_POWER_OR_ZERO,
          },
        );

      } else {
        // More than one meter? Show only one line per meter.
        lines.push({
          type: "channel-line",
          name: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, meter.alias),
          channel: meter.id + "/ActivePower",
          converter: Converter.POWER_IN_WATT,
        });
      }

      lines.push(
        // Individual phases: Voltage, Current and Power
        ...ModalComponent.generatePhasesView(meter, translate, role),
        {
          // Line separator
          type: "horizontal-line",
        },
      );
    }

    if (gridMeters.length > 0) {
      // Technical info
      lines.push({
        type: "info-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.PHASES_INFO"),
      });
    }

    return {
      title: translate.instant("GENERAL.GRID"),
      lines: lines,
    };
  }

  private static generatePhasesView(component: EdgeConfig.Component, translate: TranslateService, role: Role): OeFormlyField[] {
    return Phase.THREE_PHASE
      .map(phase => <OeFormlyField>{
        type: "children-line",
        name: {
          channel: ChannelAddress.fromString(component.id + "/ActivePower" + phase),
          converter: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, translate.instant("GENERAL.PHASE") + " " + phase),
        },

        indentation: TextIndentation.SINGLE,
        children: ModalComponent.generatePhasesLineItems(role, phase, component),
      });
  }

  private static generatePhasesLineItems(role: Role, phase: string, component: EdgeConfig.Component) {
    const children: OeFormlyField[] = [];
    if (Role.isAtLeast(role, Role.INSTALLER)) {
      children.push({
        type: "item",
        channel: component.id + "/Voltage" + phase,
        converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT,
      }, {
        type: "item",
        channel: component.id + "/Current" + phase,
        converter: Converter.CURRENT_IN_MILLIAMPERE_TO_ABSOLUTE_AMPERE,
      });
    }

    children.push({
      type: "item",
      channel: component.id + "/ActivePower" + phase,
      converter: Converter.POSITIVE_POWER,
    });

    return children;
  }

  private static getLines(config: EdgeConfig, translate: TranslateService, lines: OeFormlyField[]) {
    const is14aEnabled = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
    const limiter14aValue = "4,2 kW";
    const isRcrEnabled = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");

    const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")?.[0] ?? null;
    const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")?.[0] ?? null;

    lines.push({
      type: "value-from-channels-line",
      name: translate.instant("GENERAL.STATE"),
      value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
      channelsToSubscribe: [
        ...this.getChannelsFromController(config),
      ],
    });

    if (isRcrEnabled) {
      lines.push({
        type: "value-from-channels-line",
        name: translate.instant("GRID_STATES.FEED_IN_LIMITATION"),
        value: (currentData: CurrentData) => {
          const value = transformRcrValues(currentData.allComponents[controllerRcr + "/RestrictionMode"]) ?? 0;
          return value + " % (" + translate.instant("GRID_STATES.RIPPLE_CONTROL_RECEIVER") + " " + (100 - value) + "%" + ")";
        },
        channelsToSubscribe: [
          new ChannelAddress(controllerRcr, "RestrictionMode"),
        ],
        filter: (currentData: CurrentData) => {
          const restrictionMode = currentData?.allComponents[controllerRcr + "/RestrictionMode"] ?? null;
          if (restrictionMode == null) {
            return true;
          }
          return restrictionMode !== RippleControlReceiverRestrictionLevel.NO_RESTRICTION;
        },
      });
    }
    if (is14aEnabled) {
      lines.push({
        type: "value-from-channels-line",
        name: translate.instant("GRID_STATES.FEED_IN_DESCRIPITON"),
        value: (currentData: CurrentData) => currentData.allComponents[controller14a + "/RestrictionMode"] == ChartAnnotationState.ON ? limiter14aValue : "-",
        channelsToSubscribe: [
          new ChannelAddress(controller14a, "RestrictionMode"),
        ],
        filter: (currentData: CurrentData) => {
          const restrictionMode = currentData?.allComponents[controller14a + "/RestrictionMode"] ?? null;
          if (restrictionMode == null) {
            return true;
          }
          return restrictionMode !== ChartAnnotationState.OFF;
        },
      });
    }

    if (is14aEnabled || isRcrEnabled) {
      lines.push({
        type: "horizontal-line",
      });
    }
  }

  private static getChannelsFromController(config: EdgeConfig): ChannelAddress[] {
    const channelAddresses: ChannelAddress[] = [new ChannelAddress("_sum", "GridMode")];
    const is14aActivated = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.Limiter14a");
    const isRcrActivated = GridSectionComponent.isControllerEnabled(config, "Controller.Ess.RippleControlReceiver");
    const controller14a = config.getComponentIdsByFactory("Controller.Ess.Limiter14a")[0];
    const controllerRcr = config.getComponentIdsByFactory("Controller.Ess.RippleControlReceiver")[0];
    if (is14aActivated) {
      channelAddresses.push(new ChannelAddress(controller14a, "RestrictionMode"));
    }

    if (isRcrActivated) {
      channelAddresses.push(new ChannelAddress(controllerRcr, "RestrictionMode"));
    }
    return channelAddresses;
  }

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return ModalComponent.generateView(config, role, this.translate);
  }

}
