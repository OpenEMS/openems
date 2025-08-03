import { Component, effect, EventEmitter, Output, inject } from "@angular/core";
import { NavigationService } from "../service/navigation.service";
import { NavigationTree } from "../shared";

@Component({
    selector: "oe-navigation-chips",
    templateUrl: "./chips.html",
    standalone: false,
})
export class NavigationChipsComponent {
    protected navigationService = inject(NavigationService);


    @Output() public navigate: EventEmitter<any> = new EventEmitter();
    protected children: (NavigationTree | null)[] = [];
    protected isVisible: boolean = false;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() {
        const navigationService = this.navigationService;

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
