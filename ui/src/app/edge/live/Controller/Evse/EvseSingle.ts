import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { TranslateService } from "@ngx-translate/core";
import { ChartDataset } from "chart.js";
import { TimeOfUseTariffUtils } from "src/app/shared/service/utils";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { SchedulePowerChartComponent } from "./modal/powerChart";
import { ScheduleChartComponent } from "./modal/scheduleChart";

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    ModalComponent,
    ScheduleChartComponent,
    SchedulePowerChartComponent,
  ],
  exports: [
    FlatComponent,
  ],
})
export class Controller_Evse_Single { }

export namespace Controller_Evse_Single {

  export type ScheduleChartData = {
    datasets: ChartDataset[],
    colors: any[],
    labels: Date[]
  };

  export enum Mode {
    ZERO = 0,
    MINIMUM = 1,
    SURPLUS = 2,
    FORCE = 3,
  }

  /**
   * Gets the schedule chart data containing datasets, colors and labels.
   *
   * @param size The length of the dataset
   * @param prices The quarterly price array
   * @param modes The modes array
   * @param timestamps The timestamps array
   * @param translate The Translate service
   * @returns The ScheduleChartData.
   */
  export function getScheduleChartData(size: number, prices: number[], modes: number[], timestamps: string[],
    translate: TranslateService): Controller_Evse_Single.ScheduleChartData {

    const datasets: ChartDataset[] = [];
    const colors: any[] = [];
    const labels: Date[] = [];

    // Initializing States.
    const barZero = Array(size).fill(null);
    const barMinimum = Array(size).fill(null);
    const barSurplus = Array(size).fill(null);
    const barForce = Array(size).fill(null);

    for (let index = 0; index < size; index++) {
      const quarterlyPrice = TimeOfUseTariffUtils.formatPrice(prices[index]);
      const mode = modes[index];
      labels.push(new Date(timestamps[index]));

      if (mode !== null) {
        switch (mode) {
          case Controller_Evse_Single.Mode.ZERO:
            barZero[index] = quarterlyPrice;
            break;
          case Controller_Evse_Single.Mode.MINIMUM:
            barMinimum[index] = quarterlyPrice;
            break;
          case Controller_Evse_Single.Mode.SURPLUS:
            barSurplus[index] = quarterlyPrice;
            break;
          case Controller_Evse_Single.Mode.FORCE:
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
      backgroundColor: "rgba(0,0,0,0.8)",
      borderColor: "rgba(0,0,0,0.9)",
    });

    datasets.push({
      type: "bar",
      label: "Minimum",
      data: barMinimum,
      order: 1,
    });
    colors.push({
      backgroundColor: "rgba(25, 19, 82, 0.8)",
      borderColor: "rgba(51,102,0,1)",
    });

    datasets.push({
      type: "bar",
      label: "Surplus PV",
      data: barSurplus,
      order: 1,
    });
    colors.push({
      backgroundColor: "rgba(51,102,0,0.8)",
      borderColor: "rgba(51,102,0,1)",
    });

    datasets.push({
      type: "bar",
      label: "Force Charge",
      data: barForce,
      order: 1,
    });
    colors.push({
      backgroundColor: "rgba(0, 204, 204,0.5)",
      borderColor: "rgba(0, 204, 204,0.7)",
    });

    const scheduleChartData: Controller_Evse_Single.ScheduleChartData = {
      colors: colors,
      datasets: datasets,
      labels: labels,
    };

    return scheduleChartData;
  }
}
