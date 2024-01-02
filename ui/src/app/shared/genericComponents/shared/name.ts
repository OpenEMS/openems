import { TranslateService } from "@ngx-translate/core";

import { EdgeConfig } from "../../shared";
import { Converter } from "./converter";

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
