import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';
import { FlatComponent } from './dynamic-electricity-tariff/flat';
import { ModalComponent } from './dynamic-electricity-tariff/modal';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
        FlatComponent,
        ModalComponent,
    ],
    exports: [
        AdvertisementComponent,
    ],
})
export class AdvertisementModule { }
