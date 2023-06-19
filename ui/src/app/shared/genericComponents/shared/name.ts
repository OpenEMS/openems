import { TranslateService } from "@ngx-translate/core";
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
}