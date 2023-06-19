export namespace Name {

  export const SUFFIX_FOR_GRID_SELL_OR_GRID_BUY = (translate, name) => {
    return (value: any): string => {
      if (!value) {
        return name;
      }

      if (value < 0) {
        return name + " " + translate.instant('General.gridSellAdvanced');
      }

      if (value >= 0) {
        return name + " " + translate.instant('General.gridBuyAdvanced');
      }
    };
  };
}