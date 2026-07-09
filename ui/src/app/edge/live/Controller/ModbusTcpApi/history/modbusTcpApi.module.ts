import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ControllerModbusTcpApiChartComponent } from "./chart/chart";
import { ModbusTcpApiHistoryFlatComponent } from "./flat/flat";
import { ControllerModbusTcpApiOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        ControllerModbusTcpApiChartComponent,
        ModbusTcpApiHistoryFlatComponent,
        ControllerModbusTcpApiOverviewComponent,
    ],
    exports: [
        ControllerModbusTcpApiOverviewComponent,
        ControllerModbusTcpApiChartComponent,
        ModbusTcpApiHistoryFlatComponent,
    ],
})
export class ControllerModbusTcpApi {}
