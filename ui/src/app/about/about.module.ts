import { AboutComponent } from './about.component';

import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';


@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    AboutComponent,
  ]
})
export class AboutModule { }
