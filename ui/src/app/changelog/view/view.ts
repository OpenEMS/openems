import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { ChangelogComponent } from "./component/CHANGELOG.COMPONENT";

@Component({
  selector: "changelogViewComponent",
  templateUrl: "./VIEW.HTML",
  standalone: true,
  imports: [IonicModule, CommonModule, ChangelogComponent],
})
export class ChangelogViewComponent { }
