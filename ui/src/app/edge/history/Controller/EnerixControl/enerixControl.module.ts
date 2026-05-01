import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { ControllerEnerixOverviewComponent as ControllerEnerixControlOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ControllerEnerixControlOverviewComponent,
        ChartComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEnerixControlOverviewComponent,
        ChartComponent,
    ],
})
export class EnerixControl { }
