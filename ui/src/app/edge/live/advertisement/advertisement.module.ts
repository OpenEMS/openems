import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { FeneconHomeComponent } from './feneconHome/feneconHome';
import { KostalPvInverterComponent } from './kostalpvinverter/kostalpvinverter';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
        FeneconHomeComponent,
        KostalPvInverterComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }