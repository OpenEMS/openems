import { Component, effect, EventEmitter, Output } from "@angular/core";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-chips",
    templateUrl: "./chips.html",
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
            const currentNode = navigationService.currentNode();
            this.children = currentNode?.getChildren() ?? [];
            this.isVisible = this.children.length > 0;
        });
    }

    /**
    * Navigates to passed link
    *
    * @param link the link segment to navigate to
    * @returns
    */
    public async navigateTo(node: NavigationTree): Promise<void> {
        this.navigate.emit(node);
    }
}
