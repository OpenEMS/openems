import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';
import { EOYAdvertComponent } from './eoy-award/eoy';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        EOYAdvertComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }