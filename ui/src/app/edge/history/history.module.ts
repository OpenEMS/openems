import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { WidgetModule } from './chart/widget/widget.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './chart/energy/energy.component';
import { KwhComponent } from './kwh/kwh.component';

@NgModule({
  imports: [
    SharedModule,
    WidgetModule
  ],
  declarations: [
    HistoryComponent,
    EnergyComponent,
    KwhComponent
  ]
})
export class HistoryModule { }
