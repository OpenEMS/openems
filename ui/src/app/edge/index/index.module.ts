import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { EnergymonitorModule } from './energymonitor/energymonitor.module';
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
  ]
})
export class IndexModule { }
