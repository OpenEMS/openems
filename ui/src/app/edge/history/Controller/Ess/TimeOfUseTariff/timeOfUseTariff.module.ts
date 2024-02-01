import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";
import { ChartComponent } from "./chart/chart";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        FlatComponent,
    ],
    declarations: [
        FlatComponent,
        OverviewComponent,
        ChartComponent,
    ],
    exports: [
        FlatComponent,
        OverviewComponent,
        ChartComponent,
    ],
})
export class TimeOfUseTariff { }
