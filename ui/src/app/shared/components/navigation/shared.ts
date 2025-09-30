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
        return new NavigationTree(NAVIGATION_TREE.ID, NAVIGATION_TREE.ROUTER_LINK, NAVIGATION_TREE.ICON, NAVIGATION_TREE.LABEL, NAVIGATION_TREE.MODE, NAVIGATION_TREE.CHILDREN, NAVIGATION_TREE.PARENT);
    }

    public getChildren(): NavigationTree[] | null {
        return THIS.CHILDREN?.filter(el => el != null) ?? null;
    }

    public getParents(): NavigationTree[] | null {


        let root: NavigationTree | null = THIS.PARENT;
        if (root == null) {
            return null;
        }

        const navigationParents: NavigationTree[] | null = [root];
        while (root?.parent) {
            root = ROOT.PARENT;
            NAVIGATION_PARENTS.PUSH(root);
        }

        return NAVIGATION_PARENTS.REVERSE();
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
        THIS.CHILDREN = THIS.GET_UPDATED_NAVIGATION_TREE(this, parentNavigationId, childNavigationTree)?.children ?? [];

        function setParentRecursive(node: NavigationTree, parent: NavigationTree | null): void {
            NODE.PARENT = parent;

            if (!NODE.CHILDREN) {
                return;
            }
            for (const child of NODE.CHILDREN) {
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
            TREE.CHILDREN ??= [];
            const currentChildren = TREE.CHILDREN.MAP(child => CHILD.ID == navigationId ? newNavigation : child);

            if (!CURRENT_CHILDREN.SOME(el => EL.ID == NEW_NAVIGATION.ID)) {
                CURRENT_CHILDREN.PUSH(newNavigation);
            }
            TREE.CHILDREN = currentChildren;
            return tree;
        }

        if (tree && TREE.CHILDREN && TREE.CHILDREN.LENGTH > 0) {
            for (const child of TREE.CHILDREN) {
                const result = THIS.GET_UPDATED_NAVIGATION_TREE(child, navigationId, newNavigation);
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

