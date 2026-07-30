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

Chart.register(ChartConstants.Plugins.SYNC_CHARTS());

@Component({
    selector: "oe-components-chart-single-xaxis",
    templateUrl: "./timeline-chart.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class TimeLineChartComponent {
    protected readonly userService: UserService = inject(UserService);

    protected _data: GetSchedule.Response | null = null;
    protected datasets: ChartDataset[] = [];
    protected labels: Date[] = [];
    protected options: ChartOptions = ONLY_X_AXIS();
    protected chartType: ChartType = "line";
    protected spinnerId: string = uuidv4();

    @Input() public set data(value: GetSchedule.Response) {
        this._data = value;
        this.labels = this._data.getLabels24h();
        this.options = ONLY_X_AXIS();
        this.datasets = [
            {
                data: this.labels.map((el) => ({ x: el.getTime(), y: 0 })),
                borderWidth: 0,
                pointRadius: 0,
                backgroundColor: "transparent",
                borderColor: "transparent",
                showLine: false,
                fill: false,
            },
        ];

        /** Tooltips */
        const tooltipCallbacks = this.options.plugins?.tooltip?.callbacks;
        if (tooltipCallbacks != null) {
            tooltipCallbacks.title = (tooltipItems) => {
                const label = this.labels[tooltipItems[0]?.dataIndex];
                return DateTimeUtils.format(label, DateTimeFormats.HOUR_MINUTE) ?? "";
            };
            tooltipCallbacks.label = () => "";
        }

        if (this.options.plugins?.tooltip != null) {
            this.options.plugins.tooltip.position = "bottom";
        }
    }
}

export const ONLY_X_AXIS = (): ChartOptions<any> => {
    return {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
            mode: "index",
            intersect: false,
        },

        plugins: {
            legend: {
                display: false,
            },
            datalabels: {
                display: false,
            },
            tooltip: {
                callbacks: {},
                caretPadding: 0,
                position: "average",
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
                    color: getComputedStyle(
                        document.documentElement,
                    ).getPropertyValue("--ion-color-chart-xAxis-ticks"),
                },
                afterUpdate: (scale: ChartOptions<any>["scales"][number]) => {
                    if (scale == null || scale.ticks == null) {
                        return;
                    }
                    // Replace midght label with Weekday
                    for (let i = 1; i < scale.ticks.length; i++) {
                        const tick = scale.ticks[i];
                        const timestamp = new Date(tick.value);
                        if (isEqual(startOfDay(timestamp), timestamp)) {
                            // midnight
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
