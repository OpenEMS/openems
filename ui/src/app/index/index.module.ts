import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';

import { IndexComponent } from './index.component';


@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    IndexComponent
  ]
})
export class IndexModule { }
