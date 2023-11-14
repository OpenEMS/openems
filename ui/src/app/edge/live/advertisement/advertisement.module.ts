import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';
import { EoYWinnerAdvertComponent } from './eoy-winner/eoy-winner';
import { DynamicElectricityTarifAdvertForExistingCustomerComponent } from './dynamic-electricity-tariff/dynamic-electricity-tariff-existing-customer';
import { DynamicElectricityTarifAdvertForNewCustomerComponent } from './dynamic-electricity-tariff/dynamic-electricity-tariff-new-customer';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
        EoYWinnerAdvertComponent,
        DynamicElectricityTarifAdvertForExistingCustomerComponent,
        DynamicElectricityTarifAdvertForNewCustomerComponent,
    ],
    exports: [
        AdvertisementComponent,
    ],
})
export class AdvertisementModule { }
