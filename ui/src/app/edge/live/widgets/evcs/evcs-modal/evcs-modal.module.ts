import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { SharedModule } from 'src/app/shared/shared.module';
import { EvcsModalPage } from './evcs-modal.page';
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
