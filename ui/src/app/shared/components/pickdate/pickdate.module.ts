import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { IonicModule } from '@ionic/angular';
import { TranslateModule } from '@ngx-translate/core';

import { PickDateComponent } from './pickdate.component';
import { PickDatePopoverComponent } from './popover/popover.component';
import { AngularMyDatePickerModule } from '@nodro7/angular-mydatepicker';

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    ReactiveFormsModule,
    TranslateModule,
    AngularMyDatePickerModule,
  ],
  declarations: [
    PickDateComponent,
    PickDatePopoverComponent,
  ],
  exports: [
    PickDateComponent,
  ],

})
export class PickdateModule { }
