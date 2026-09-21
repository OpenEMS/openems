import { Component, OnInit, ViewEncapsulation } from "@angular/core";
import { ActivatedRoute, ActivatedRouteSnapshot, Router } from "@angular/router";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "formly-link",
    templateUrl: "./formly-link.html",
    encapsulation: ViewEncapsulation.None,
    standalone: false,
})
export class FormlyLinkComponent
    extends FieldType<
        FieldTypeConfig<
            FormlyFieldConfig["props"] & {
                link?:
                    | {
                          type: "appUpdate";
                          appId: string;
                          instanceId?: string;
                          property?: string;
                      }
                    | { type: "appInstall"; appId: string; name: string };
            }
        >
    >
    implements OnInit
{
    protected urlToNavigate: {
        queryParams: Record<string, string>;
        baseUrl: string;
    } | null = null;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
    ) {
        super();
    }

    ngOnInit(): void {
        this.urlToNavigate = this.buildUrlToNavigate();
    }

    protected onNavigate() {
        AssertionUtils.assertIsDefined(this.urlToNavigate, "link is undefined");
        this.router.navigate([this.urlToNavigate.baseUrl], {
            relativeTo: this.route,
            queryParams: this.urlToNavigate.queryParams,
            replaceUrl: true,
        });
    }

    private buildUrlToNavigate(): {
        queryParams: Record<string, string>;
        baseUrl: string;
    } | null {
        const link = this.props.link;
        if (link === undefined || link === null) {
            return null;
        }
        let route: ActivatedRouteSnapshot | null = this.route.snapshot;
        while (route && route?.routeConfig?.path?.indexOf(":edgeId") === -1) {
            route = route?.parent;
        }

        if (route === undefined || route === null) {
            return null;
        }

        if (link.type === "appUpdate") {
            return {
                baseUrl: "../update",
                queryParams: { appId: link.appId },
            };
        }

        if (link.type === "appInstall") {
            return {
                baseUrl: "../install",
                queryParams: {
                    appId: link.appId,
                    name: link.name,
                    callback: "true",
                },
            };
        }

        return null;
    }
}
