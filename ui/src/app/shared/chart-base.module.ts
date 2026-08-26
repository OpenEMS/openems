import { NgModule } from "@angular/core";
import { ChartComponentsModule } from "./components/chart/chart.module";
import { ComponentsBaseModule } from "./components/components.module";
import { HistoryDataErrorModule } from "./components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "./components/pickdate/pickdate.module";

@NgModule({
    imports: [
        ComponentsBaseModule,
        PickdateComponentModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
    ],
    exports: [
        ComponentsBaseModule,
        PickdateComponentModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
    ],
})
export class ChartBaseModule { }

