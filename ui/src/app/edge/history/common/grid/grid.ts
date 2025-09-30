import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/FOOTER_NAVIGATION.MODULE";
import { SharedModule } from "src/app/shared/SHARED.MODULE";
import { ChartComponent } from "./chart/chart";
import { ChartComponent as DetailsChartComponent } from "./details/chart/chart";
import { DetailsOverviewComponent } from "./details/DETAILS.OVERVIEW";
import { FlatComponent } from "./flat/flat";
import { OverviewComponent } from "./overview/overview";

@NgModule({
  imports: [
    BrowserModule,
    FooterNavigationModule,
    SharedModule,
  ],
  declarations: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    DetailsChartComponent,
    DetailsOverviewComponent,
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    DetailsChartComponent,
    DetailsOverviewComponent,
  ],
})
export class Common_Grid { }
