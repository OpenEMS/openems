import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Routes, RouterModule } from '@angular/router';

import { IonicModule } from '@ionic/angular';

import { EvcsModalPage } from './evcs-modal.page';

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
    RouterModule.forChild(routes)
  ],
  declarations: [EvcsModalPage]
})
export class EvcsModalPageModule {}
