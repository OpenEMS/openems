import { NgModule } from "@angular/core";
import { SharedModule } from "./../shared/shared.module";
import { EdgeComponent } from "./edge.component";
import { HistoryModule } from "./history/history.module";
import { LiveModule } from "./live/live.module";

@NgModule({
  declarations: [
    EdgeComponent,
  ],
  imports: [
    HistoryModule,
    LiveModule,
    SharedModule,
  ],
  exports: [
    EdgeComponent,
  ],
})
export class EdgeModule { }
