// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { Router, RoutesRecognized } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { filter, map } from "rxjs/operators";

import { environment } from "src/environments";
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
    ).subscribe(async (e: { [key: string]: string }) => {

      // Always use last entry of data object
      const lastData = Object.entries(e).map(([k, v]) => ({ key: k, value: v })).reverse()[0] ?? null;
      if (lastData == null) {
        return;
      }

      const res = lastData.key === "navbarTitle" ? lastData.value :
        (lastData.key === "navbarTitleToBeTranslated"
          ? translate.instant(lastData.value) : null)
        ?? this.service.currentPageTitle
        ?? environment.uiTitle;
      this.service.currentPageTitle = res;
    });
  }
}
