import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';
import { DynamicElectricityTarifAdvertForExistingCustomerComponent } from './dynamic-electricity-tariff/dynamic-electricity-tariff-existing-customer';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
        DynamicElectricityTarifAdvertForExistingCustomerComponent,
    ],
    exports: [
        AdvertisementComponent,
    ],
})
export class AdvertisementModule { }
