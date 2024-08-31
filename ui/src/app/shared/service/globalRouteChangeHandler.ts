// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { Router, RoutesRecognized } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { filter, map } from "rxjs/operators";

import { Service } from "./service";

@Injectable({
  providedIn: "root",
})
export class GlobalRouteChangeHandler {

  constructor(
    public service: Service,
    private router: Router,
    private translate: TranslateService,
  ) {

    this.router.events.pipe(
      filter(event => event instanceof RoutesRecognized),
      map(event => {
        let data = null;
        let route = event["state"].root;

        while (route) {
          data = route.data || data;
          route = route.firstChild;
        }
        return data;
      }),
    ).subscribe(e => {

      if (e.navbarTitle != null && e.navbarTitleToBeTranslated != null) {
        throw new Error("Either use navbarTitle or navbarTitleToBeTranslated");
      }

      this.service.currentPageTitle = e.navbarTitle ?? (e.navbarTitleToBeTranslated ? translate.instant(e.navbarTitleToBeTranslated) : null) ?? this.service.currentPageTitle;
    });
  }
}
