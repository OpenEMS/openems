import { Component, Input } from "@angular/core";
import { ActivatedRoute, Params, Router, RouterModule } from "@angular/router";
import { IonButton, IonicModule } from "@ionic/angular";
import { Icon } from "src/app/shared/type/widget";

@Component({
    selector: "oe-flat-button",
    templateUrl: "./flat-widget-button.html",
    standalone: true,
    imports: [
        IonicModule,
        RouterModule,
    ],
})
export class FlatWidgetButtonComponent {

    @Input() public link: { routeRelative?: ActivatedRoute, text: string, queryParams?: Params } | null = null;
    @Input() public color: "light" | "medium" | "primary" = "primary";
    @Input() public disabled: IonButton["disabled"] = false;
    @Input() public fill: IonButton["fill"] = "solid";

    constructor(
        private router: Router,
    ) { }

    protected navigateTo() {
        if (this.link == null) {
            return;
        }

        if (this.link.routeRelative != null) {
            this.router.navigate([this.link.text], { queryParams: this.link.queryParams ? this.link.queryParams : null, relativeTo: this.link.routeRelative },);
            return;
        }

        this.router.navigate([this.link.text],
            this.link.queryParams ? { queryParams: this.link.queryParams } : {});
    }
}

export type ButtonLabel = {
    /** Name of Label, displayed below the icon */
    name: string;
    value: string | number | boolean;
    /** Icons for Button, displayed above the corresponding name */
    icon?: Icon;
    callback?: () => void;
    style?: { [key: string]: string };
};
