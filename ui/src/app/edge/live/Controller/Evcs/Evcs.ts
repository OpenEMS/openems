import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { TimeOfUseTariffUtils } from "src/app/shared/service/utils";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { ScheduleChartComponent } from "./modal/scheduleChart";
import { PopoverComponent } from "./popover/popover";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    ModalComponent,
    PopoverComponent,
    ScheduleChartComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class Controller_Evcs { }

export namespace Controller_Evcs {

  export type ScheduleChartData = {
    datasets: ChartDataset[],
    colors: any[],
    labels: Date[]
  };

  export enum SmartMode {
    ZERO = 0,
    SURPLUS_PV = 1,
    FORCE = 3,
  }

  /**
   * Gets the schedule chart data containing datasets, colors and labels.
   *
   * @param size The length of the dataset
   * @param prices The Time-of-Use-Tariff quarterly price array
   * @param states The Time-of-Use-Tariff state array
   * @param timestamps The Time-of-Use-Tariff timestamps array
   * @param translate The Translate service
   * @returns The ScheduleChartData.
   */
  export function getScheduleChartData(size: number, prices: number[], states: number[], timestamps: string[], translate: TranslateService): Controller_Evcs.ScheduleChartData {

    const datasets: ChartDataset[] = [];
    const colors: any[] = [];
    const labels: Date[] = [];

    // Initializing States.
    const barZero = Array(size).fill(null);
    const barSurplusPv = Array(size).fill(null);
    const barForce = Array(size).fill(null);

    for (let index = 0; index < size; index++) {
      const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(prices[index]);
      const state = states[index];
      labels.push(new Date(timestamps[index]));

      if (state !== null) {
        switch (state) {
          case Controller_Evcs.SmartMode.ZERO:
            barZero[index] = quarterlyPrice;
            break;
          case Controller_Evcs.SmartMode.SURPLUS_PV:
            barSurplusPv[index] = quarterlyPrice;
            break;
          case Controller_Evcs.SmartMode.FORCE:
            barForce[index] = quarterlyPrice;
            break;
        }
      }
    }

    // Set datasets
    datasets.push({
      type: "bar",
      label: "No Charge",
      data: barZero,
      order: 1,
    });
    colors.push({
      // Dark Green
      backgroundColor: "rgba(51,102,0,0.8)",
      borderColor: "rgba(51,102,0,1)",
    });

    // Set dataset for ChargeGrid.
    datasets.push({
      type: "bar",
      label: "Surplus PV",
      data: barSurplusPv,
      order: 1,
    });
    colors.push({
      // Sky blue
      backgroundColor: "rgba(0, 204, 204,0.5)",
      borderColor: "rgba(0, 204, 204,0.7)",
    });

    // Set dataset for buy from grid
    datasets.push({
      type: "bar",
      label: "Force Charge",
      data: barForce,
      order: 1,
    });
    colors.push({
      // Black
      backgroundColor: "rgba(0,0,0,0.8)",
      borderColor: "rgba(0,0,0,0.9)",
    });

    const scheduleChartData: Controller_Evcs.ScheduleChartData = {
      colors: colors,
      datasets: datasets,
      labels: labels,
    };

    return scheduleChartData;
  }
}
