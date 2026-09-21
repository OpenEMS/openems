import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FooterNavigationModule } from "src/app/shared/components/footer/subnavigation/footerNavigation.module";
import { SharedModule } from "src/app/shared/shared.module";
import { TotalChartComponent } from "./chart/chart";
import { ChartComponent } from "./details/chart/chart";
import { ControllerIoFixDigitalOutputDetailsOverviewComponent } from "./details/details.overview";
import { FixDigitalDetailsComponent } from "./details/new-navigation/details";
import { FlatComponent } from "./flat/flat";
import { FixDigitalInputHistoryComponent } from "./new-navigation/new-navigation";
import { ControllerIoDigitalOutputOverviewComponent } from "./overview/overview";

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
        FooterNavigationModule,
        ChartComponent,
        TotalChartComponent,
        FixDigitalDetailsComponent,
        ControllerIoFixDigitalOutputDetailsOverviewComponent,
        FixDigitalInputHistoryComponent,
    ],
    declarations: [
        FlatComponent,
        ControllerIoDigitalOutputOverviewComponent,
    ],
    exports: [
        FlatComponent,
        ControllerIoDigitalOutputOverviewComponent,
        TotalChartComponent,
        ControllerIoFixDigitalOutputDetailsOverviewComponent,
        ChartComponent,
        FixDigitalInputHistoryComponent,
        FixDigitalDetailsComponent,
    ],
})
export class FixDigitalOutputHistory { }
