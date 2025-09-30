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

    THIS.ROUTER.EVENTS.PIPE(
      filter(event => event instanceof RoutesRecognized),
      map(event => {
        let data = null;
        let route = event["state"].root;

        while (route) {
          data = ROUTE.DATA || data;
          route = ROUTE.FIRST_CHILD;
        }

        return data;
      }),
    ).subscribe(async (e: { [key: string]: string }) => {

      // Always use last entry of data object
      const lastData = OBJECT.ENTRIES(e).map(([k, v]) => ({ key: k, value: v })).reverse()[0] ?? null;
      if (lastData == null) {
        return;
      }

      const res = LAST_DATA.KEY === "navbarTitle" ? LAST_DATA.VALUE :
        (LAST_DATA.KEY === "navbarTitleToBeTranslated"
          ? TRANSLATE.INSTANT(LAST_DATA.VALUE) : null)
        ?? THIS.SERVICE.CURRENT_PAGE_TITLE
        ?? ENVIRONMENT.UI_TITLE;
      THIS.SERVICE.CURRENT_PAGE_TITLE = res;
    });
  }
}
