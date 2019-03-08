import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { WidgetModule } from './chart/widget/widget.module';
import { HistoryComponent } from './history.component';
import { EnergyComponent } from './chart/energy/energy.component';

@NgModule({
  imports: [
    SharedModule,
    WidgetModule
  ],
  declarations: [
    HistoryComponent,
    EnergyComponent
  ]
})
export class HistoryModule { }
