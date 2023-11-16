import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';
import { DynamicElectricityTarifAdvertForExistingCustomerComponent } from './dynamic-electricity-tariff/dynamic-electricity-tariff-existing-customer';
import { EoYWinnerAdvertComponent } from './eoy-winner/eoy-winner';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
        EoYWinnerAdvertComponent,
        DynamicElectricityTarifAdvertForExistingCustomerComponent,
    ],
    exports: [
        AdvertisementComponent,
    ],
})
export class AdvertisementModule { }
