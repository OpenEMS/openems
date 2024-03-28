import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { compareVersions } from "compare-versions";
import { isBefore } from "date-fns";
import { Edge } from "src/app/shared/shared";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { SchedulePowerAndSocChartComponent } from "./modal/powerSocChart";
import { ScheduleStateAndPriceChartComponent } from "./modal/statePriceChart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        FlatComponent,
        ModalComponent,
        ScheduleStateAndPriceChartComponent,
        SchedulePowerAndSocChartComponent,
    ],
    declarations: [
        FlatComponent,
        ModalComponent,
        ScheduleStateAndPriceChartComponent,
        SchedulePowerAndSocChartComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Ess_TimeOfUseTariff {

    // TODO remove once there is no 2024.3.1 version anymore with Time-of-Use-Tariff
    public static filterScheduleData(edge: Edge, schedule: {
        timestamp: string;
        price: number;
        state: number;
        grid: number;
        production: number,
        consumption: number,
        ess: number,
        soc: number,
    }[]) {
        if(compareVersions(edge.version, "2024.3.1") == 0) {
            var lastTimestamp = new Date(schedule[0].timestamp);
            for(var i = 1; i<schedule.length; i++) {
                var timestamp = new Date(schedule[i].timestamp);
                if(isBefore(timestamp, lastTimestamp)) {
                    const ref = i - 4;
                    for(var j of [
                        ref + 1, ref + 2, ref + 3, ref + 4,
                        ref + 6, ref + 7, ref + 8, ref + 9,
                        ref + 11, ref + 12, ref + 13, ref + 14,
                    ]) {
                        schedule[j] = null;
                    }
                    i = ref + 15;
                }
                lastTimestamp = timestamp;
            }
        }
        return schedule = schedule.filter(e => e !== null);
    }

}
