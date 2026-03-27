import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";

import { GridOptimizedChargeChartComponent } from "./chart/chart";
import { SellToGridLimitChartComponent } from "./chart/sellToGridLimitChart.component";
import { FlatComponent } from "./flat/flat";
import { ControllerEssGridOptimizedChargeOverviewComponent as ControllerEssGridoptimizedChargeOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    declarations: [
        FlatComponent,
        ControllerEssGridoptimizedChargeOverviewComponent,
        GridOptimizedChargeChartComponent,
        SellToGridLimitChartComponent,
    ],
    exports: [
        FlatComponent,
        ControllerEssGridoptimizedChargeOverviewComponent,
        GridOptimizedChargeChartComponent,
        SellToGridLimitChartComponent,
    ],
})
export class GridOptimizeCharge { }
