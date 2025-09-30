import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
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
export class ModbusTcpApi { }
