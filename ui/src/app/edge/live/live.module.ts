import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { WidgetModule } from './widget/widget.module';
import { LiveComponent } from './live.component';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    WidgetModule
  ],
  declarations: [
    LiveComponent,
  ]
})
export class LiveModule { }
