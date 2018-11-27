import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';

import { SettingsComponent } from './settings.component';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        SettingsComponent,
    ]
})
export class SettingsModule { }
