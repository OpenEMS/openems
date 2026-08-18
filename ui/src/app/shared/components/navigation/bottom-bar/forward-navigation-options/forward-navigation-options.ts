import { ChangeDetectionStrategy, Component, computed, inject } from "@angular/core";
import { RouterModule } from "@angular/router";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { NavigationLabelLineComponent } from "../../label-line/label-line";
import { NavigationService } from "../../service/navigation.service";
import { NavigationTree } from "../../shared";

@Component({
    selector: "oe-forward-navigation-options",
    templateUrl: "./forward-navigation-options.html",
    imports: [CommonUiModule, RouterModule, NavigationLabelLineComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    styles: `
        ion-button::part(native) {
            span {
                width: 100%;
                justify-content: left;
            }
        }
    `,
})
export class ForwardNavigationOptions {
    protected readonly navigationService = inject(NavigationService);
    protected children = computed(() => {
        return this.filterVisibleNodes(this.navigationService.currentNode()?.getChildren() ?? []);
    });
    protected isAllowed = computed(() => this.navigationService.position() === "bottom");

    /**
     * Filters out the nodes to hide in navigation.
     *
     * @param nodes The navigation tree nodes
     * @returns The adjusted nodes
     */
    public filterVisibleNodes(nodes: NavigationTree[]): NavigationTree[] {
        return nodes
            .filter((node) => node.showOrder !== "HIDE") // keep only locally visible nodes
            .map((node) => ({
                ...node,
                children: node.children ? this.filterVisibleNodes(node.children) : [],
            })) as NavigationTree[];
    }
}
