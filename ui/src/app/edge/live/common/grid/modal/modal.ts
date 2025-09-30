import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Filter } from "src/app/shared/components/shared/filter";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { GridSectionComponent } from "../../../energymonitor/chart/section/GRID.COMPONENT";
import { LiveDataService } from "../../../livedataservice";

@Component({
  templateUrl: "../../../../../shared/components/formly/formly-field-modal/TEMPLATE.HTML",
  standalone: false,
  providers: [
    { provide: DataService, useClass: LiveDataService },
  ],
})
export class ModalComponent extends AbstractFormlyComponent {

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {

    const isActivated = GRID_SECTION_COMPONENT.IS_CONTROLLER_ENABLED(config, "CONTROLLER.ESS.LIMITER14A");
    // Grid-Mode
    const lines: OeFormlyField[] = [{
      type: "channel-line",
      name: TRANSLATE.INSTANT("GENERAL.OFF_GRID"),
      channel: "_sum/GridMode",
      filter: Filter.GRID_MODE_IS_OFF_GRID,
      converter: Converter.HIDE_VALUE,
    }];

    const gridMeters = OBJECT.VALUES(CONFIG.COMPONENTS).filter(component => config?.isTypeGrid(component));

    // Sum Channels (if more than one meter)
    if (GRID_METERS.LENGTH > 1) {
      if (isActivated) {
        LINES.PUSH({
          type: "value-from-channels-line",
          name: TRANSLATE.INSTANT("GENERAL.STATE"),
          value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
          channelsToSubscribe: [
            new ChannelAddress("_sum", "GridMode"),
            new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode"),
          ],
        });
      }

      LINES.PUSH(
        {
          type: "channel-line",
          name: TRANSLATE.INSTANT("GENERAL.GRID_SELL_ADVANCED"),
          channel: "_sum/GridActivePower",
          converter: Converter.GRID_SELL_POWER_OR_ZERO,
        },
        {
          type: "channel-line",
          name: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
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
      if (GRID_METERS.LENGTH === 1) {
        // Two lines if there is only one meter (= same visualization as with Sum Channels)
        if (isActivated) {
          LINES.PUSH({
            type: "value-from-channels-line",
            name: TRANSLATE.INSTANT("GENERAL.STATE"),
            value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
            channelsToSubscribe: [
              new ChannelAddress("_sum", "GridMode"),
              new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode"),
            ],
          });
        }

        LINES.PUSH(
          {
            type: "channel-line",
            name: TRANSLATE.INSTANT("GENERAL.GRID_SELL_ADVANCED"),
            channel: METER.ID + "/ActivePower",
            converter: Converter.GRID_SELL_POWER_OR_ZERO,
          },
          {
            type: "channel-line",
            name: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
            channel: METER.ID + "/ActivePower",
            converter: Converter.GRID_BUY_POWER_OR_ZERO,
          },
        );

      } else {
        // More than one meter? Show only one line per meter.
        LINES.PUSH({
          type: "channel-line",
          name: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, METER.ALIAS),
          channel: METER.ID + "/ActivePower",
          converter: Converter.POWER_IN_WATT,
        });
      }

      LINES.PUSH(
        // Individual phases: Voltage, Current and Power
        ...MODAL_COMPONENT.GENERATE_PHASES_VIEW(meter, translate, role),
        {
          // Line separator
          type: "horizontal-line",
        },
      );
    }

    if (GRID_METERS.LENGTH > 0) {
      // Technical info
      LINES.PUSH({
        type: "info-line",
        name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.PHASES_INFO"),
      });
    }

    return {
      title: TRANSLATE.INSTANT("GENERAL.GRID"),
      lines: lines,
    };
  }

  private static generatePhasesView(component: EDGE_CONFIG.COMPONENT, translate: TranslateService, role: Role): OeFormlyField[] {
    return ["L1", "L2", "L3"]
      .map(phase => <OeFormlyField>{
        type: "children-line",
        name: {
          channel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/ActivePower" + phase),
          converter: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, TRANSLATE.INSTANT("GENERAL.PHASE") + " " + phase),
        },

        indentation: TEXT_INDENTATION.SINGLE,
        children: MODAL_COMPONENT.GENERATE_PHASES_LINE_ITEMS(role, phase, component),
      });
  }

  private static generatePhasesLineItems(role: Role, phase: string, component: EDGE_CONFIG.COMPONENT) {
    const children: OeFormlyField[] = [];
    if (ROLE.IS_AT_LEAST(role, ROLE.INSTALLER)) {
      CHILDREN.PUSH({
        type: "item",
        channel: COMPONENT.ID + "/Voltage" + phase,
        converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT,
      }, {
        type: "item",
        channel: COMPONENT.ID + "/Current" + phase,
        converter: Converter.CURRENT_IN_MILLIAMPERE_TO_ABSOLUTE_AMPERE,
      });
    }

    CHILDREN.PUSH({
      type: "item",
      channel: COMPONENT.ID + "/ActivePower" + phase,
      converter: Converter.POSITIVE_POWER,
    });

    return children;
  }

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return MODAL_COMPONENT.GENERATE_VIEW(config, role, THIS.TRANSLATE);
  }

}
