import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
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
}
