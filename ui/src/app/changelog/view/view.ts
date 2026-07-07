import { Component } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { ChangelogComponent } from "./component/changelog.component";

@Component({
    selector: "changelogViewComponent",
    templateUrl: "./view.html",
    standalone: true,
    imports: [IonicModule, ChangelogComponent],
})
export class ChangelogViewComponent { }
