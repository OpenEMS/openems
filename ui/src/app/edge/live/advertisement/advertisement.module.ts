import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { ExtendedWarrantyComponent } from './extended-warranty/extended-warranty';
import { FemsAppCenterComponent } from './fems-app-center/fems-app-center';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        FemsAppCenterComponent,
        ExtendedWarrantyComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }