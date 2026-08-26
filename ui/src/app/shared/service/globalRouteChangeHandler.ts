// @ts-strict-ignore
import { inject, Injectable } from "@angular/core";
import { Router, RoutesRecognized } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { filter, map } from "rxjs/operators";

import { PlatFormService } from "src/app/platform.service";
import { environment } from "src/environments";
import { Service } from "./service";

@Injectable({
    providedIn: "root",
})
export class GlobalRouteChangeHandler {

    private platformService = inject(PlatFormService);

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
                    data = (route.data && Object.keys(route.data).length > 0) ? route.data : data;
                    route = route.firstChild;
                }

                return data;
            }),
        ).subscribe(async (e: { [key: string]: string }) => {

            if (e == null) {
                return;
            }

            // Always use last entry of data object
            const lastData = Object?.entries(e ?? {})?.map(([k, v]) => ({ key: k, value: v }))?.reverse()?.[0] ?? null;
            if (lastData == null) {
                return;
            }

            this.service.currentPageTitle = this.resolvePageTitle(
                lastData,
                this.service.currentPageTitle,
                this.translate
            );

            const device = this.platformService.getDevice();
            if (device?.isSmartphone()) {
                this.service.currentPageTitle = environment.uiTitleShort;
            }
        });
    }

    private resolvePageTitle(
        lastData: { key: string; value: string },
        currentTitle: string | null,
        translate: TranslateService
    ): string {
        if (lastData.key === "navbarTitle") {
            return lastData.value;
        }

        if (lastData.key === "navbarTitleToBeTranslated") {
            return translate.instant(lastData.value);
        }

        return currentTitle ?? environment.uiTitle;
    }

}
