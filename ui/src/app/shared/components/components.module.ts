import { CommonModule } from "@angular/common";
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { BrowserModule } from "@angular/platform-browser";
import { RouterModule } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { PipeComponentsModule, PipeModule } from "src/app/shared/pipe/pipe.module";
import { CommonUiModule } from "../common-ui.module";
import { DomChangeDirective } from "../directive/oe-dom-change";
import { ChartComponentsModule, ChartModule } from "./chart/chart.module";
import { FlatWidgetComponent } from "./flat/flat";
import { FlatWidgetHorizontalLineComponent } from "./flat/flat-widget-horizontal-line/flat-widget-horizontal-line";
import { FlatWidgetLineComponent } from "./flat/flat-widget-line/flat-widget-line";
import { FlatWidgetLineItemComponent } from "./flat/flat-widget-line/flat-widget-line-item/flat-widget-line-item";
import { FlatWidgetLineDividerComponent } from "./flat/flat-widget-line-divider/flat-widget-line-divider";
import { FlatWidgetPercentagebarComponent } from "./flat/flat-widget-percentagebar/flat-widget-percentagebar";
import { FooterComponent } from "./footer/footer";
import { FooterNavigationComponentsModule, FooterNavigationModule } from "./footer/subnavigation/footerNavigation.module";
import { HistoryDataErrorModule } from "./history-data-error/history-data-error.module";
import { ModalComponentsModule, ModalModule } from "./modal/modal.module";
import { NavigationBreadCrumbsComponent } from "./navigation/bread-crumbs/breadcrumbs";
import { NavigationChipsComponent } from "./navigation/chips/chips";
import { NavigationPageComponent as NavigationViewComponent } from "./navigation/view/view";
import { PickdateComponentModule, PickdateModule } from "./pickdate/pickdate.module";
import { NotificationComponent } from "./shared/notification/notification";

@NgModule({
    imports: [
        CommonModule,
        IonicModule,
        PipeComponentsModule,
        ReactiveFormsModule,
        DomChangeDirective,
        RouterModule,
        ModalComponentsModule,
        PickdateComponentModule,
        ChartComponentsModule,
    ],
    declarations: [

        // Flat
        FlatWidgetComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetLineComponent,
        FlatWidgetLineDividerComponent,
        FlatWidgetLineItemComponent,
        FlatWidgetPercentagebarComponent,

        // Others
        NotificationComponent,
        FooterComponent,
        NavigationViewComponent,
        NavigationChipsComponent,
        NavigationBreadCrumbsComponent,
    ],
    exports: [
        // Flat
        FlatWidgetComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetLineComponent,
        FlatWidgetLineDividerComponent,
        FlatWidgetLineItemComponent,
        FlatWidgetPercentagebarComponent,

        // Others
        NotificationComponent,
        FooterComponent,
        NavigationViewComponent,
        NavigationChipsComponent,
        NavigationBreadCrumbsComponent,
        ModalModule,
        FooterNavigationComponentsModule,
        PickdateComponentModule,
        ChartComponentsModule,
        PipeComponentsModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ComponentsBaseModule { }
@NgModule({
    imports: [
        ComponentsBaseModule,
        BrowserModule,
        CommonUiModule,
        PipeModule,
        HistoryDataErrorModule,
        FooterNavigationModule,
        ChartModule,
        DomChangeDirective,
        PickdateModule,
        ModalModule,
        ReactiveFormsModule,
        RouterModule,
    ],
    exports: [
        ComponentsBaseModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ComponentsModule { }
