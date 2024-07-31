import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { IonicModule } from '@ionic/angular';
import { TranslateModule } from '@ngx-translate/core';

import { PickDateComponent } from './pickdate.component';
import { PickDatePopoverComponent } from './popover/popover.component';

@NgModule({
  imports: [
    BrowserModule,
    IonicModule,
    ReactiveFormsModule,
    TranslateModule,
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
