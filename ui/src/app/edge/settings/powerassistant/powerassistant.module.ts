import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";
import { PowerAssistantComponent } from "./powerassistant";

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    PowerAssistantComponent,
  ],
  exports: [
    PowerAssistantComponent,
  ],
})
export class PowerAssistantModule { }
