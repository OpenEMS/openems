import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { IonicModule } from '@ionic/angular';
import { PipeModule } from '../pipe/pipe';
import { FlatWidgetComponent } from './flat/flat';
import { FlatWidgetHorizontalLine } from './flat/flat-widget-horizontal-line/flat-widget-horizontal-line';
import { FlatWidgetLine } from './flat/flat-widget-line/flat-widget-line';
import { FlatWidgetPercentagebar } from './flat/flat-widget-percentagebar/flat-widget-percentagebar';
import { ModalComponent } from './modal/modal';
import { ModalButtons } from './modal/modal-button/modal-button';
import { ModalInfoLine } from './modal/modal-info-line/modal-info-line';
import { ModalLine } from './modal/modal-line/modal-line';
import { ModalHorizontalLine } from './modal/model-horizontal-line/modal-horizontal-line';

@NgModule({
    imports: [
        BrowserModule,
        IonicModule,
        PipeModule,
        ReactiveFormsModule,
    ],
    entryComponents: [
        FlatWidgetComponent,
        FlatWidgetLine,
        FlatWidgetHorizontalLine,
        FlatWidgetPercentagebar,
        ModalButtons,
        ModalInfoLine,
        ModalLine,
        ModalHorizontalLine,
        ModalComponent
    ],
    declarations: [
        FlatWidgetComponent,
        FlatWidgetLine,
        FlatWidgetHorizontalLine,
        FlatWidgetPercentagebar,
        ModalButtons,
        ModalInfoLine,
        ModalLine,
        ModalHorizontalLine,
        ModalComponent,
    ],
    exports: [
        FlatWidgetComponent,
        FlatWidgetLine,
        FlatWidgetHorizontalLine,
        FlatWidgetPercentagebar,
        ModalButtons,
        ModalInfoLine,
        ModalLine,
        ModalHorizontalLine,
        ModalComponent,
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]

})
export class Generic_ComponentsModule { }
