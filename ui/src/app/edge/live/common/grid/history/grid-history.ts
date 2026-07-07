import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ChartBaseModule } from "src/app/shared/chart-base.module";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { ChartComponent } from "./chart/chart";
import { ChartComponent as DetailsChartLimitationComponent } from "./details/external-limitation/chart/chart";

import { CommonGridExternalLimitationOverviewComponent } from "./details/external-limitation/new-navigation/new-navigation";
import { CommonGridDetailsExternalLimitationOverviewComponent } from "./details/external-limitation/overview/details.overview";
import { ChartComponent as DetailsChartPhaseAccurateComponent } from "./details/phase-accurate/chart/chart";
import { CommonGridPhaseAccurateOverviewComponent } from "./details/phase-accurate/new-navigation/new-navigation";
import { CommonGridDetailsPhaseAccurateOverviewComponent } from "./details/phase-accurate/overview/details.overview";
import { FlatComponent } from "./flat/flat";
import { CommonGridHistoryComponent } from "./new-navigation/new-navigation";
import { CommonGridOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        FooterNavigationModule,
        ChartBaseModule,
        CommonUiModule,
    ],
    declarations: [
        FlatComponent,
        ChartComponent,
        CommonGridOverviewComponent,
        CommonGridHistoryComponent,

        DetailsChartPhaseAccurateComponent,
        CommonGridPhaseAccurateOverviewComponent,
        CommonGridExternalLimitationOverviewComponent,
        CommonGridDetailsExternalLimitationOverviewComponent,
        CommonGridDetailsPhaseAccurateOverviewComponent,

        DetailsChartLimitationComponent,
        CommonGridExternalLimitationOverviewComponent,
        CommonGridPhaseAccurateOverviewComponent,
    ],
    exports: [
        FlatComponent,
        ChartComponent,
        CommonGridOverviewComponent,
        CommonGridHistoryComponent,

        DetailsChartPhaseAccurateComponent,
        CommonGridPhaseAccurateOverviewComponent,
        CommonGridExternalLimitationOverviewComponent,
        DetailsChartLimitationComponent,
        CommonGridExternalLimitationOverviewComponent,
        CommonGridPhaseAccurateOverviewComponent,
    ],
})
export class CommonGridHistory { }
