import { CommonModule } from "@angular/common";
import { Component, inject, Input } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, ChartOptions, ChartType } from "chart.js";
import { isEqual, startOfDay } from "date-fns";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { v4 as uuidv4 } from "uuid";

import { UserService } from "src/app/shared/service/user.service";
import { Language } from "src/app/shared/type/language";
import { DateTimeFormats, DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { GetSchedule } from "../../edge/config-components/energy/getSchedule";
import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { ChartConstants } from "../chart.constants";
import { ChartComponentsModule } from "../chart.module";

@Component({
    selector: "oe-components-chart-single-xaxis",
    templateUrl: "./single-xaxis.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
        CommonModule,
    ],
})
export class SingleXAxisComponent {

    protected readonly userService: UserService = inject(UserService);

    protected _data: GetSchedule.Response | null = null;
    protected datasets: ChartDataset[] = [];
    protected labels: Date[] = [];
    protected options: ChartOptions = ONLY_X_AXIS();
    protected loading = false;
    protected chartType: ChartType = "line";
    protected spinnerId: string = uuidv4();

    @Input() public set data(value: GetSchedule.Response) {
        this._data = value;
        this.labels = this._data.getLabels();
        this.options = ONLY_X_AXIS();

        Chart.register(ChartConstants.Plugins.SYNC_CHARTS());
    }
}

export const ONLY_X_AXIS = (): ChartOptions<any> => {
    return {
        responsive: true,
        maintainAspectRatio: false,

        plugins: {
            legend: {
                display: false,
            },
            tooltip: {
                enabled: false,
            },
            ["syncChart"]: {
                group: 1,
            },
        },

        scales: {
            y: {
                display: false, // hide entire Y axis
                grid: {
                    display: false,
                    drawTicks: false,
                },
                ticks: {
                    display: false,
                },

            },
            x: {
                stacked: true,
                offset: false,
                type: "time",
                bounds: "data",
                display: true,
                ticks: {
                    source: "auto",
                    minRotation: 0,
                    maxRotation: 0,
                },
                afterUpdate: (scale: ChartOptions<any>["scales"][number]) => {
                    if (scale == null || scale.ticks == null) {
                        return;
                    }
                    // Replace midght label with Weekday
                    for (let i = 1; i < scale.ticks.length; i++) {
                        const tick = scale.ticks[i];
                        const timestamp = new Date(tick.value);
                        if (isEqual(startOfDay(timestamp), timestamp)) { // midnight
                            tick.label = DateTimeUtils.formatWithLocale(timestamp, DateTimeFormats.WEEKDAY);
                        }
                    }
                },
                adapters: {
                    date: {
                        locale: Language.getCurrentLanguage().dateFnsLocale,
                    },
                },
                time: {
                    round: false,
                    unit: "hour",
                    displayFormats: {
                        hour: "HH", // 17
                    },
                },
                grid: {
                    display: false, // removes chart area/grid lines
                    drawTicks: false,
                    offset: false,
                },
                border: {
                    display: false,
                },
            },
        },

        elements: {
            point: {
                radius: 0,
            },
        },
        layout: {
            padding: {
                left: 0,
            },
        },
    };
};
