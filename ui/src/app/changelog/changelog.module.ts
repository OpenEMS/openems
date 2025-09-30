import { CommonModule } from "@angular/common";
import { NgModule } from "@angular/core";
import { ChangelogRoutingModule } from "./changelog-ROUTING.MODULE";
import { ChangelogComponent } from "./view/component/CHANGELOG.COMPONENT";
import { ChangelogViewComponent } from "./view/view";

@NgModule({
  imports: [
    CommonModule,
    ChangelogComponent,
    ChangelogViewComponent,
    ChangelogRoutingModule,
  ],
  declarations: [
  ],
  exports: [
    ChangelogComponent,
  ],
})
export class ChangelogModule { }
