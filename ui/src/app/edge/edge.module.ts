import { HistoryModule } from './history/history.module';
import { LiveModule } from './live/live.module';
import { NgModule } from '@angular/core';
import { SharedModule } from './../shared/shared.module';
import { EdgeComponent } from './edge.component';
import { ExampleSystemsModule } from './exampleSystems/exampleSystems.module';

@NgModule({
  declarations: [
    EdgeComponent
  ],
  imports: [
    HistoryModule,
    LiveModule,
    SharedModule,
    ExampleSystemsModule
  ],
  exports: [
    EdgeComponent
  ]
})
export class EdgeModule { }
