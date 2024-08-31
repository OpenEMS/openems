import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { SharedModule } from "src/app/shared/shared.module";
import { TotalChartComponent } from "./chart/chart";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";
import { ChartComponent } from "./details/chart/chart";
import { DetailsOverviewComponent } from "./details/details.overview";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";

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
