import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PopoverPage } from './popover.component';
import { SharedModule } from '../shared.module';


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        IonicModule,
        SharedModule
    ],
    declarations: [PopoverPage]
})
export class PopoverPageModule { }
