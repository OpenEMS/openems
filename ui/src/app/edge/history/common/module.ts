import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { Common_Autarchy } from './autarchy/Autarchy';
import { Common_Grid } from './grid/module';
import { Common_Production } from './production/production';

@NgModule({
  imports: [
    Common_Autarchy,
    Common_Production,
    Common_Grid
  ],
  exports: [
    Common_Autarchy,
    Common_Production,
    Common_Grid
  ],
})
export class Common_Module { }
