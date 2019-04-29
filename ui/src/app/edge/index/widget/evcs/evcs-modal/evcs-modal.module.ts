import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule, ModalController } from '@ionic/angular';

import { EvcsModalPage } from './evcs-modal.page';
import { SharedModule } from 'src/app/shared/shared.module';
import { InfoPopoverComponent } from './info-popover/info-popover.component';

const routes: Routes = [
  {
    path: '',
    component: EvcsModalPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    SharedModule,
    RouterModule.forChild(routes)
  ],
  entryComponents: [InfoPopoverComponent],
  declarations: [EvcsModalPage, InfoPopoverComponent]
})
export class EvcsModalPageModule {

}
