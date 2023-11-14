import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';

import { AdvertisementComponent } from './advertisement.component';

@NgModule({
    imports: [
        SharedModule,
    ],
    declarations: [
        AdvertisementComponent,
    ],
    exports: [
        AdvertisementComponent,
    ],
})
export class AdvertisementModule { }
