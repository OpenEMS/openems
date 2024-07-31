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
import { HelpButtonComponent } from './modal/help-button/help-button';
import { ModalComponent } from './modal/modal';
import { ModalButtonsComponent } from './modal/modal-button/modal-button';
import { ModalInfoLineComponent } from './modal/modal-info-line/modal-info-line';
import { ModalLineComponent } from './modal/modal-line/modal-line';
import { ModalLineItemComponent } from './modal/modal-line/modal-line-item/modal-line-item';
import { ModalPhasesComponent } from './modal/modal-phases/modal-phases';
import { ModalValueLineComponent } from './modal/modal-value-line/modal-value-line';
import { ModalHorizontalLineComponent } from './modal/model-horizontal-line/modal-horizontal-line';
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
    ],
    declarations: [

        // Flat
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        FlatWidgetLineItemComponent,

        // Modal
        ModalButtonsComponent,
        ModalInfoLineComponent,
        ModalLineComponent,
        ModalHorizontalLineComponent,
        ModalComponent,
        ModalLineItemComponent,
        ModalPhasesComponent,
        ModalValueLineComponent,

        // Others
        HelpButtonComponent,
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

        // Modal
        ModalButtonsComponent,
        ModalInfoLineComponent,
        ModalLineComponent,
        ModalHorizontalLineComponent,
        ModalComponent,
        ModalLineItemComponent,
        ModalPhasesComponent,
        ModalValueLineComponent,

        // Others
        HelpButtonComponent,
        NotificationComponent,
        FooterComponent,

        FooterNavigationModule,
        ChartModule,
        PickdateModule,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],

})
export class ComponentsModule { }
