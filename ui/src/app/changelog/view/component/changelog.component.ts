import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { environment } from "src/environments";
import { Service } from "../../../shared/shared";
import { Role } from "../../../shared/type/role";
import { Changelog } from "./CHANGELOG.CONSTANTS";

@Component({
  selector: "changelog",
  templateUrl: "./CHANGELOG.COMPONENT.HTML",
  standalone: true,
  imports: [IonicModule, CommonModule, TranslateModule],
})
export class ChangelogComponent {

  public environment = environment;

  public readonly roleIsAtLeast = ROLE.IS_AT_LEAST;
  public readonly changelogs: {
    title?: string,
    version?: string,
    changes: Array<string | { roleIsAtLeast: Role, change: string }>
  }[] = [
      {
        version: "X.Y.Z",
        changes: [
          CHANGELOG.LINK("OpenEMS Releases", "https://GITHUB.COM/OpenEMS/openems/releases"),
        ],
      },
    ];


  protected slice: number = 10;
  protected showAll: boolean = false;
  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
  ) { }

  public numberToRole(role: number): string {
    return Role[role].toLowerCase();
  }
}
