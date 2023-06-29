import { NgModule } from '@angular/core';
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
  ]
})
export class Common_Module { }
