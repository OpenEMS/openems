// @ts-strict-ignore
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { ModalComponent } from "./modal/modal";
import { SchedulePowerAndSocChartComponent } from "./modal/powerSocChart";
import { SchedulePowerAndSocChartReducedComponent } from "./modal/powerSocChartReduced";
import { ScheduleStateAndPriceChartComponent } from "./modal/statePriceChart";
import { ScheduleStateAndPriceChartReducedComponent } from "./modal/statePriceChartReduced";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
    ],
    declarations: [
        ModalComponent,

        FlatComponent,
        ScheduleStateAndPriceChartComponent,
        ScheduleStateAndPriceChartReducedComponent,
        SchedulePowerAndSocChartComponent,
        SchedulePowerAndSocChartReducedComponent,
    ],
    exports: [
        FlatComponent,
    ],
})
export class Controller_Ess_TimeOfUseTariff { }
