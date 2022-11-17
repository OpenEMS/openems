import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { PickDateComponent } from '../pickdate/pickdate.component';
import { PipeModule } from '../pipe/pipe';
import { ChartComponent } from './chart/chart';
import { FlatWidgetComponent } from './flat/flat';
import { FlatWidgetHorizontalLineComponent } from './flat/flat-widget-horizontal-line/flat-widget-horizontal-line';
import { FlatWidgetLineComponent } from './flat/flat-widget-line/flat-widget-line';
import { FlatWidgetPercentagebarComponent } from './flat/flat-widget-percentagebar/flat-widget-percentagebar';
import { HelpButtonComponent } from './modal/help-button/help-button';
import { ModalComponent } from './modal/modal';
import { ModalButtonsComponent } from './modal/modal-button/modal-button';
import { ModalInfoLineComponent } from './modal/modal-info-line/modal-info-line';
import { ModalLineComponent } from './modal/modal-line/modal-line';
import { ModalHorizontalLineComponent } from './modal/model-horizontal-line/modal-horizontal-line';
import { ChartComponent } from './chart/chart'
import { RouterModule } from '@angular/router';
import { ChartOptionsComponent } from '../chartoptions/chartoptions.component';
import { ChartOptionsPopoverComponent } from './chart/chartoptions/popover/popover.component';
import { TranslateModule } from '@ngx-translate/core';

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        ReactiveFormsModule,
        RouterModule,
	TranslateModule.forRoot()
    ],
    entryComponents: [
        PickDateComponent,
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        ModalButtonsComponent,
        ModalInfoLineComponent,
        ModalLineComponent,
        ModalHorizontalLineComponent,
        ModalComponent,
	ChartOptionsPopoverComponent
    ],
    declarations: [
        PickDateComponent,
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        HelpButtonComponent,
        ModalButtonsComponent,
        ModalInfoLineComponent,
        ModalLineComponent,
        ModalHorizontalLineComponent,
        ModalComponent,
        ChartComponent,
	ChartOptionsPopoverComponent
    ],
    exports: [
        FlatWidgetComponent,
        FlatWidgetLineComponent,
        FlatWidgetHorizontalLineComponent,
        FlatWidgetPercentagebarComponent,
        HelpButtonComponent,
        ModalButtonsComponent,
        ModalInfoLineComponent,
        ModalLineComponent,
        ModalHorizontalLineComponent,
        ModalComponent,
        ChartComponent,
        PickDateComponent,
	ChartOptionsPopoverComponent    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]

})
export class Generic_ComponentsModule { }
