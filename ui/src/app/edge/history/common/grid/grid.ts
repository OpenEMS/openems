import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { SharedModule } from "src/app/shared/shared.module";
import { ChartComponent } from "./chart/chart";
import { ChartComponent as DetailsChartLimitationComponent } from "./details/external-limitation/chart/chart";
import { DetailsOverviewComponent as DetailsOverviewComponentExternalLimitation } from "./details/external-limitation/details.overview";
import { ChartComponent as DetailsChartPhaseAccurateComponent } from "./details/phase-accurate/chart/chart";
import { DetailsOverviewComponent as DetailsOverviewComponentPhaseAccurate } from "./details/phase-accurate/details.overview";

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

    DetailsChartPhaseAccurateComponent,
    DetailsChartLimitationComponent,
    DetailsOverviewComponentExternalLimitation,
    DetailsOverviewComponentPhaseAccurate,
  ],
  exports: [
    FlatComponent,
    ChartComponent,
    OverviewComponent,

    DetailsChartPhaseAccurateComponent,
    DetailsChartLimitationComponent,
    DetailsOverviewComponentExternalLimitation,
    DetailsOverviewComponentPhaseAccurate,
  ],
})
export class Common_Grid { }
