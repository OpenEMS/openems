import { Component, effect } from "@angular/core";
import { NavigationService } from "./navigation.service";
import { NavigationTree } from "./shared";

@Component({
    selector: "oe-navigation",
    templateUrl: "./navigation.component.html",
    standalone: false,
})
export class NavigationComponent {
    protected children: NavigationTree[] = [];
    protected parents: NavigationTree[] = [];

    protected isAllowed = true;

    constructor(
        public navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = navigationService.currentNode();
            this.children = currentNode?.getChildren() ?? [];
            this.parents = currentNode?.getParents() ?? [];

            this.isAllowed = this.children.length > 0 || this.parents.length > 0;
        });
    }
}
