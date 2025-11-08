import { TEnumKeys, TPartialBy } from "../../type/utility";
import { Icon, WidgetClass } from "../../type/widget";

export enum NavigationId {
    LIVE = "live",
    HISTORY = "history",
}

type IconColor = "primary" | "secondary" | "tertiary" | "success" | "danger" | "medium" | "light" | "dark" | "warning" | "normal" | "production";
type PartialedIcon = TPartialBy<Pick<Omit<Icon, "size" | "color"> & { color: IconColor }, "color" | "name">, "color">;

export class NavigationTree {

    constructor(
        public id: NavigationId | string,
        public routerLink: { baseString: string, queryParams?: { [key: string]: string } },
        public icon: PartialedIcon,
        public label: string,

        // Display mode of chip
        public mode: "icon" | "label",
        public children: NavigationTree[],

        /** Use null for nested node */
        public parent: NavigationTree | null,
    ) { }

    /**
     * Creates new navigation tree instance from existing navigation tree object
     *
     * @param navigationTree
     * @returns the new navigationTree
     */
    public static of(navigationTree: NavigationTree | null): NavigationTree | null {
        if (!navigationTree) {
            return null;
        }
        return new NavigationTree(navigationTree.id, navigationTree.routerLink, navigationTree.icon, navigationTree.label, navigationTree.mode, navigationTree.children, navigationTree.parent);
    }

    public static dummy() {
        return new NavigationTree("", { baseString: "" }, { name: "help-outline" }, "", "label", [], null);
    }

    public toConstructorParams(): ConstructorParameters<typeof NavigationTree> {
        return [
            this.id, this.routerLink, this.icon,
            this.label, this.mode, this.children, this.parent,
        ];
    }

    public getChildren(): NavigationTree[] | null {
        return this.children?.filter(el => el != null) ?? null;
    }

    public getParents(): NavigationTree[] | null {


        let root: NavigationTree | null = this.parent;
        if (root == null) {
            return null;
        }

        const navigationParents: NavigationTree[] | null = [root];
        while (root?.parent) {
            root = root.parent;
            navigationParents.push(root);
        }

        return navigationParents.reverse();
    }

    /**
     * Sets the child for a given parent navigation id
     *
     * @info set parent to null for nested children
     *
     * @param parentNavigationId the parent navigation id
     * @param childNavigationTree the child navigation tree
     */
    public setChild(parentNavigationId: NavigationId | string, childNavigationTree: NavigationTree) {
        this.children = this.getUpdatedNavigationTree(this, parentNavigationId, childNavigationTree)?.children ?? [];

        function setParentRecursive(node: NavigationTree, parent: NavigationTree | null): void {
            node.parent = parent;

            if (!node.children) {
                return;
            }
            for (const child of node.children) {
                setParentRecursive(child, node);
            }
        }

        setParentRecursive(this, null);
    }

    public getUpdatedNavigationTree(tree: NavigationTree, navigationId: NavigationId | string, newNavigation: NavigationTree): NavigationTree | null {

        if (!tree) {
            return null;
        }

        if (tree?.id === navigationId) {

            // Initialize
            tree.children ??= [];
            const currentChildren = tree.children.map(child => child.id == navigationId ? newNavigation : child);

            if (!currentChildren.some(el => el.id == newNavigation.id)) {
                currentChildren.push(newNavigation);
            }
            tree.children = currentChildren;
            return tree;
        }

        if (tree && tree.children && tree.children.length > 0) {
            for (const child of tree.children) {
                const result = this.getUpdatedNavigationTree(child, navigationId, newNavigation);
                if (result) {
                    return result;
                }
            }
        }

        return null;
    }

    public updateIconColor(color: IconColor) {
        this.icon.color = color;
    }
}

export type NavigationNode = {
    id: NavigationId | string,
    routerLink: string,
    icon: Pick<Icon, "name">,
    label: string,
    mode: "icon" | "label",
};

export namespace NavigationConstants {

    /**
     * The widgets to show in new navigation
     */
    export const newWidgets: TEnumKeys<typeof WidgetClass>[] = [
        "Common_Autarchy",
        // "Common_Production",
        "Common_Selfconsumption",
        // "Consumption",
        "Grid",
        // "Storage",
    ];
}
