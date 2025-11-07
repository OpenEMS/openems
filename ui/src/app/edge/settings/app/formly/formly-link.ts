import { Component, OnInit, ViewEncapsulation } from "@angular/core";
import { ActivatedRoute, ActivatedRouteSnapshot } from "@angular/router";
import { NavController } from "@ionic/angular";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "formly-link",
    templateUrl: "./formly-link.html",
    encapsulation: ViewEncapsulation.None,
    standalone: false,
})
export class FormlyLinkComponent extends FieldType<FieldTypeConfig<FormlyFieldConfig["props"] & {
    link?: { type: "appUpdate", appId: string, instanceId?: string, property?: string }
}>> implements OnInit {

    protected urlToNavigate: string | null = null;

    constructor(
        private router: NavController,
        private route: ActivatedRoute,
    ) {
        super();
    }

    ngOnInit(): void {
        this.urlToNavigate = this.buildUrlToNavigate();
    }

    protected onNavigate() {
        AssertionUtils.assertIsDefined(this.urlToNavigate, "link is undefined");
        this.router.navigateForward(this.urlToNavigate);
    }

    private buildUrlToNavigate(): string | null {
        const link = this.props.link;
        if (link === undefined || link === null) {
            return null;
        }
        if (link.type === "appUpdate") {
            let route: ActivatedRouteSnapshot | null = this.route.snapshot;
            while (route && route?.routeConfig?.path?.indexOf(":edgeId") === -1) {
                route = route?.parent;
            }

            if (route === undefined || route === null) {
                return null;
            }

            const baseRoute = route.url.join("/");
            return baseRoute + "/settings/app/update/" + link.appId;
        }

        return null;
    }

}
