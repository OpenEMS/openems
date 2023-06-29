import { TranslateService } from "@ngx-translate/core";

import { EdgeConfig } from "../../shared";
import { Converter } from "./converter";
import { Role } from "../../type/role";
import { OeFormlyField } from "./oe-formly-component";
import { TextIndentation } from "../modal/modal-line/modal-line";

export namespace Name {

  export const SUFFIX_FOR_GRID_SELL_OR_GRID_BUY = (translate: TranslateService, name: string): Converter =>
    (value): string => {
      if (typeof value === "number") {
        if (value < 0) {
          return name + " " + translate.instant('General.gridSellAdvanced');
        } else {
          return name + " " + translate.instant('General.gridBuyAdvanced');
        }
      }
      return name;
    };

  /**
   * Even though every meter should have set the alias, it still occurrs, that it is not set
   * 
   * @param meter the meter: {@link EdgeConfig.Component}
   * @returns the meter alias if existing, else meter id 
   */
  export const METER_ALIAS_OR_ID = (meter: EdgeConfig.Component): string => meter.alias ?? meter.id;
}

export namespace Meter {

  export const METER_PHASES = (meter: EdgeConfig.Component, translate: TranslateService, role: Role): OeFormlyField[] => {
    return ['L1', 'L2', 'L3']
      .map(phase => <OeFormlyField>{
        type: 'children-line',
        name: translate.instant('General.phase') + " " + phase,
        indentation: TextIndentation.SINGLE,
        children: PHASE_LINE_ITEMS(role, phase, meter)
      });
  };

  export const PHASE_LINE_ITEMS = (role: Role, phase: string, meter: EdgeConfig.Component): OeFormlyField[] => {
    let children: OeFormlyField[] = [];
    if (Role.isAtLeast(role, Role.INSTALLER)) {
      children.push({
        type: 'item',
        channel: meter.id + '/Voltage' + phase,
        converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT
      }, {
        type: 'item',
        channel: meter.id + '/Current' + phase,
        converter: Converter.CURRENT_IN_MILLIAMPERE_TO_AMPERE
      });
    }

    children.push({
      type: 'item',
      channel: meter.id + '/ActivePower' + phase,
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
    });

    return children;
  };
}
