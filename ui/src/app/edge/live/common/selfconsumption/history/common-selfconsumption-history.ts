import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ModalModule } from "src/app/shared/components/modal/modal.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart.component";
import { FlatComponent } from "./flat/flat";
import { CommonSelfConsumptionHistoryComponent } from "./new-navigation/new-navigation";
import { OverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ModalModule,
    ],
    declarations: [
        FlatComponent,
        ChartComponent,
        OverviewComponent,
        CommonSelfConsumptionHistoryComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        OverviewComponent,
        CommonSelfConsumptionHistoryComponent,
    ],
})
export class CommonSelfConsumptionHistory { }
