import { Component, effect, EventEmitter, Output } from "@angular/core";
import { NavigationService } from "../service/NAVIGATION.SERVICE";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-chips",
    templateUrl: "./CHIPS.HTML",
    standalone: false,
})
export class NavigationChipsComponent {

    @Output() public navigate: EventEmitter<any> = new EventEmitter();
    protected children: (NavigationTree | null)[] = [];
    protected isVisible: boolean = false;

    constructor(
        protected navigationService: NavigationService,
    ) {
        effect(() => {
            const currentNode = NAVIGATION_SERVICE.CURRENT_NODE();
            THIS.CHILDREN = currentNode?.getChildren() ?? [];
            THIS.IS_VISIBLE = THIS.CHILDREN.LENGTH > 0;
        });
    }

    /**
    * Navigates to passed link
    *
    * @param link the link segment to navigate to
    * @returns
    */
    public async navigateTo(node: NavigationTree): Promise<void> {
        THIS.NAVIGATE.EMIT(node);
    }
}
