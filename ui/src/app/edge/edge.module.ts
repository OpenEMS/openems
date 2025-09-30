import { NgModule } from "@angular/core";
import { SharedModule } from "./../shared/SHARED.MODULE";
import { EdgeComponent } from "./EDGE.COMPONENT";
import { HistoryModule } from "./history/HISTORY.MODULE";
import { LiveModule } from "./live/LIVE.MODULE";

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
