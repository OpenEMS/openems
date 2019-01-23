import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
import { EnergytableComponent } from './energytable/energytable.component';
import { HistoryComponent } from './history/history.component';
import { IndexComponent } from './index.component';
import { WidgetModule } from './widget/widget.module';

@NgModule({
  imports: [
    SharedModule,
    EnergymonitorModule,
    WidgetModule
  ],
  declarations: [
    IndexComponent,
    EnergytableComponent,
    HistoryComponent,
  ]
})
export class IndexModule { }
