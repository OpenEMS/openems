import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { TranslateModule } from '@ngx-translate/core';

import { PipeModule } from '../pipe/pipe';
import { ChartModule } from './chart/chart.module';
import { FlatWidgetComponent } from './flat/flat';
import { FlatWidgetHorizontalLineComponent } from './flat/flat-widget-horizontal-line/flat-widget-horizontal-line';
import { FlatWidgetLineComponent } from './flat/flat-widget-line/flat-widget-line';
import { FlatWidgetLineItemComponent } from './flat/flat-widget-line/flat-widget-line-item/flat-widget-line-item';
import { FlatWidgetPercentagebarComponent } from './flat/flat-widget-percentagebar/flat-widget-percentagebar';
import { FooterComponent } from './footer/footer';
import { FooterNavigationModule } from './footer/subnavigation/footerNavigation.module';
import { HistoryDataErrorModule } from './history-data-error/history-data-error.module';
import { ModalModule } from './modal/modal.module';
import { PickdateModule } from './pickdate/pickdate.module';
import { NotificationComponent } from './shared/notification/notification';

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        ReactiveFormsModule,
        RouterModule,
        TranslateModule,
        HistoryDataErrorModule,
        FooterNavigationModule,
        ChartModule,
        PickdateModule,
        ModalModule,
    ],
    declarations: [

        // Flat
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        FlatWidgetLineItemComponent,

        // Others
        NotificationComponent,
        FooterComponent,
    ],
    exports: [
        // Flat
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        FlatWidgetLineItemComponent,

        // Others
        NotificationComponent,
        FooterComponent,

        FooterNavigationModule,
        ChartModule,
        PickdateModule,
        ModalModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],

})
export class ComponentsModule { }
