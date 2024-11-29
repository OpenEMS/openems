// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { CurrentData, Utils, ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'savedemissionsWidget',
  templateUrl: './flat.html',
})
export class FlatComponent extends AbstractFlatWidget {

  protected selfconsumptionValue: number | null;
  protected co2EmissionsSaved: number | null;
  protected treesPlanted: number | null;
  protected CO2Factor = 0.4;

  protected override onCurrentData(currentData: CurrentData) {
    // Convert Wh to kWh
    const gridSellActiveEnergy = (currentData.allComponents['_sum/GridSellActiveEnergy'] || 0) / 1000;
    const productionActiveEnergy = (currentData.allComponents['_sum/ProductionActiveEnergy'] || 0) / 1000;

    // Calculate self-consumption rate
    this.selfconsumptionValue = Utils.calculateSelfConsumption(
      gridSellActiveEnergy,
      productionActiveEnergy,
    );

    if (productionActiveEnergy > 0 && this.selfconsumptionValue != null) {
      // Convert self-consumption rate from percentage to fraction for calculation
      const selfConsumptionRate = this.selfconsumptionValue / 100;
      this.co2EmissionsSaved = Utils.calculateCO2EmissionsSaved(
        productionActiveEnergy,
        selfConsumptionRate,
        gridSellActiveEnergy,
        this.CO2Factor, // CO2 factor
      );

      // Calculate the number of trees equivalent to the CO2 emissions saved
      this.treesPlanted = Utils.calculateTreesPlanted(this.co2EmissionsSaved);
    } else {
      this.co2EmissionsSaved = null;
      this.treesPlanted = null;
    }
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    return [
      new ChannelAddress('_sum', 'GridSellActiveEnergy'),
      new ChannelAddress('_sum', 'ProductionActiveEnergy'),
    ];
  }
}
