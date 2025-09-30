import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { Phase } from "src/app/shared/components/shared/phase";

import { ChannelAddress, CurrentData, EdgeConfig } from "../../../../../shared/shared";
import { LiveDataService } from "../../../livedataservice";

@Component({
  templateUrl: "../../../../../shared/components/formly/formly-field-modal/TEMPLATE.HTML",
  standalone: false,
  providers: [
    { provide: DataService, useClass: LiveDataService },
  ],
})
export class ModalComponent extends AbstractFormlyComponent {

  public static generateView(config: EdgeConfig, translate: TranslateService): OeFormlyView {

    const evcss: EDGE_CONFIG.COMPONENT[] | null = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.EVCS.API.EVCS")
      .filter(component =>
        !(COMPONENT.FACTORY_ID == "EVCS.CLUSTER.SELF_CONSUMPTION") &&
        !(COMPONENT.FACTORY_ID == "EVCS.CLUSTER.PEAK_SHAVING") &&
        !(CONFIG.FACTORIES[COMPONENT.FACTORY_ID].NATURE_IDS.INCLUDES("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")) &&
        !COMPONENT.IS_ENABLED == false);

    const consumptionMeters: EDGE_CONFIG.COMPONENT[] | null = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.METER.API.ELECTRICITY_METER")
      .filter(component => COMPONENT.IS_ENABLED && CONFIG.IS_TYPE_CONSUMPTION_METERED(component));

    const lines: OeFormlyField[] = [];

    // Total
    LINES.PUSH({
      type: "channel-line",
      name: TRANSLATE.INSTANT("GENERAL.TOTAL"),
      channel: "_sum/ConsumptionActivePower",
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
    });

    Phase.THREE_PHASE.forEach(phase => {
      LINES.PUSH({
        type: "channel-line",
        name: TRANSLATE.INSTANT("GENERAL.PHASE") + " " + phase,
        indentation: TEXT_INDENTATION.SINGLE,
        channel: "_sum/ConsumptionActivePower" + phase,
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
    });

    if (EVCSS.LENGTH > 0) {
      LINES.PUSH({
        type: "horizontal-line",
      });
    }

    // Evcss
    EVCSS.FOR_EACH((evcs, index) => {
      LINES.PUSH({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(evcs),
        channel: EVCS.ID + "/ChargePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });

      if (index < (EVCSS.LENGTH - 1)) {
        LINES.PUSH({ type: "horizontal-line" });
      }
    });

    if (CONSUMPTION_METERS.LENGTH > 0) {
      LINES.PUSH({ type: "horizontal-line" });
    }

    // Consumptionmeters
    CONSUMPTION_METERS.FOR_EACH((meter, index) => {
      LINES.PUSH({
        type: "channel-line",
        name: Name.METER_ALIAS_OR_ID(meter),
        channel: METER.ID + "/ActivePower",
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
      Phase.THREE_PHASE.forEach(phase => {
        LINES.PUSH({
          type: "channel-line",
          name: "Phase " + phase,
          channel: METER.ID + "/ActivePower" + phase,
          indentation: TEXT_INDENTATION.SINGLE,
          converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        });
      });

      if (index < (CONSUMPTION_METERS.LENGTH - 1)) {
        LINES.PUSH({
          type: "horizontal-line",
        });
      }
    });

    LINES.PUSH({ type: "horizontal-line" });

    // OtherPower
    const channelsToSubscribe: ChannelAddress[] = [new ChannelAddress("_sum", "ConsumptionActivePower")];

    EVCSS.FOR_EACH(evcs => CHANNELS_TO_SUBSCRIBE.PUSH(new ChannelAddress(EVCS.ID, "ChargePower")));
    CONSUMPTION_METERS.FOR_EACH(meter => {
      CHANNELS_TO_SUBSCRIBE.PUSH(...[new ChannelAddress(METER.ID, "ActivePower")]);
    });

    LINES.PUSH({
      type: "value-from-channels-line",
      name: TRANSLATE.INSTANT("GENERAL.OTHER_CONSUMPTION"),
      value: (currentData: CurrentData) => Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO(Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
      channelsToSubscribe: channelsToSubscribe,
    });

    LINES.PUSH({
      type: "info-line",
      name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.PHASES_INFO"),
    });

    return {
      title: TRANSLATE.INSTANT("GENERAL.CONSUMPTION"),
      lines: lines,
    };
  }

  protected override generateView(config: EdgeConfig): OeFormlyView {
    return MODAL_COMPONENT.GENERATE_VIEW(config, THIS.TRANSLATE);
  }

}
