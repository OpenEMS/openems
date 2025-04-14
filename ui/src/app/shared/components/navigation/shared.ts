import { TPartialBy } from "../../type/utility";
import { Icon } from "../../type/widget";

export enum NavigationId {
    LIVE = "live",
    HISTORY = "history",
}

type IconColor = "primary" | "secondary" | "tertiary" | "success" | "danger" | "medium" | "light" | "dark" | "warning";
type PartialedIcon = TPartialBy<Pick<Omit<Icon, "size" | "color"> & { color: IconColor }, "color" | "name">, "color">;

export class NavigationTree {

    constructor(
        public id: NavigationId | string,
        public routerLink: string,
        public icon: PartialedIcon,
        public label: string,
        public mode: "icon" | "label",
        public children: NavigationTree[] | null,
        public parent: NavigationTree | null,
    ) { }

    public static of(navigationTree: NavigationTree) {
        return new NavigationTree(navigationTree.id, navigationTree.routerLink, navigationTree.icon, navigationTree.label, navigationTree.mode, navigationTree.children, navigationTree.parent);
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

        return navigationParents;
    }

    public setChild(parentNavigationId: NavigationId, childNavigationTree: NavigationTree) {
        this.children = this.getUpdatedNavigationTree(this, parentNavigationId, childNavigationTree)?.children ?? [];
    }

    public getUpdatedNavigationTree(tree: NavigationTree, navigationId: NavigationId, newNavigation: NavigationTree): NavigationTree | null {

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
}

export type NavigationNode = {
    id: NavigationId | string,
    routerLink: string,
    icon: Pick<Icon, "name">,
    label: string,
    mode: "icon" | "label",
};

export const baseNavigationTree: NavigationTree = new NavigationTree(NavigationId.LIVE, "live", { name: "home-outline" }, "live", "icon", [], null);
