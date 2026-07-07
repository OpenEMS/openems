import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { CommonAutarchyHistoryComponent } from "./new-navigation/new-navigation";
import { CommonAutarchyOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ChartComponent,
        CommonAutarchyOverviewComponent,
        CommonAutarchyHistoryComponent,
        ChartComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        CommonAutarchyOverviewComponent,
        CommonAutarchyHistoryComponent,
    ],
})
export class CommonAutarchyHistory { }
