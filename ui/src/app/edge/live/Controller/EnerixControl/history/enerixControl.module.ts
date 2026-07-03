import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { EnerixControlChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerEnerixControlHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerEnerixOverviewComponent as ControllerEnerixControlOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        EnerixControlChartComponent,
        ControllerEnerixControlHistoryComponent,
    ],
    declarations: [FlatComponent, ControllerEnerixControlOverviewComponent],
    exports: [
        FlatComponent,
        ControllerEnerixControlOverviewComponent,
        EnerixControlChartComponent,
        ControllerEnerixControlHistoryComponent,
    ],
})
export class ControllerEnerixControlControlHistory {}
