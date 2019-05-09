import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { WidgetsModule } from './widgets/widgets.module';
import { LiveComponent } from './live.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    WidgetsModule
  ],
  declarations: [
    LiveComponent,
  ]
})
export class LiveModule { }
