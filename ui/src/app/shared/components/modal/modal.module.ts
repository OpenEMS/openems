import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { TranslateModule } from '@ngx-translate/core';

import { PipeModule } from '../../pipe/pipe';
import { HelpButtonComponent } from './help-button/help-button';
import { ModalComponent } from './modal';
import { ModalButtonsComponent } from './modal-button/modal-button';
import { ModalInfoLineComponent } from './modal-info-line/modal-info-line';
import { ModalLineComponent } from './modal-line/modal-line';
import { ModalLineItemComponent } from './modal-line/modal-line-item/modal-line-item';
import { ModalPhasesComponent } from './modal-phases/modal-phases';
import { ModalValueLineComponent } from './modal-value-line/modal-value-line';
import { ModalHorizontalLineComponent } from './model-horizontal-line/modal-horizontal-line';

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
    PipeModule,
  ],
  declarations: [
    ModalButtonsComponent,
    ModalInfoLineComponent,
    ModalLineComponent,
    ModalHorizontalLineComponent,
    ModalComponent,
    ModalLineItemComponent,
    ModalPhasesComponent,
    ModalValueLineComponent,
    HelpButtonComponent,
  ],
  exports: [
    ModalButtonsComponent,
    ModalInfoLineComponent,
    ModalLineComponent,
    ModalHorizontalLineComponent,
    ModalComponent,
    ModalLineItemComponent,
    ModalPhasesComponent,
    ModalValueLineComponent,
    HelpButtonComponent,
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],

})
export class ModalModule { }
