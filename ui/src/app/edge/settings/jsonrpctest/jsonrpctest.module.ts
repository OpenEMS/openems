import { NgModule } from "@angular/core";
import { SharedModule } from "src/app/shared/shared.module";
import { JsonrpcTestComponent } from "./jsonrpctest";

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    JsonrpcTestComponent,
  ],
  exports: [
    JsonrpcTestComponent,
  ],
})
export class JsonrpcTestModule { }
