import { Component } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { environment } from "src/environments";
import { Service } from "../../../shared/shared";
import { Role } from "../../../shared/type/role";
import { Changelog } from "./changelog.constants";

@Component({
  selector: "changelog",
  templateUrl: "./changelog.component.html",
})
export class ChangelogComponent {

  public environment = environment;

  public readonly roleIsAtLeast = Role.isAtLeast;
  public readonly changelogs: {
    title?: string,
    version?: string,
    changes: Array<string | { roleIsAtLeast: Role, change: string }>
  }[] = [
      {
        version: "x.y.z",
        changes: [
          Changelog.link("OpenEMS Releases", "https://github.com/OpenEMS/openems/releases"),
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
