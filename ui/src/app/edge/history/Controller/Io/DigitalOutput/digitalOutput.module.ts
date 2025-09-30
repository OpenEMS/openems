import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/FOOTER_NAVIGATION.MODULE";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { TotalChartComponent } from "./chart/chart";
import { ChartComponent } from "./details/chart/chart";
import { DetailsOverviewComponent } from "./details/DETAILS.OVERVIEW";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        FooterNavigationModule,
    ],
    declarations: [
        FlatComponent,
        OverviewComponent,
        TotalChartComponent,
        DetailsOverviewComponent,
        ChartComponent,
    ],
    exports: [
        FlatComponent,
        OverviewComponent,
        TotalChartComponent,
        DetailsOverviewComponent,
        ChartComponent,
    ],
})
export class DigitalOutput { }
