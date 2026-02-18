import { TEnumKeys, TPartialBy } from "../../type/utility";
import { Icon, WidgetClass } from "../../type/widget";

export enum NavigationId {
    LIVE = "live",
    HISTORY = "history",
}

type IconColor = "primary" | "secondary" | "tertiary" | "success" | "danger" | "medium" | "light" | "dark" | "warning" | "normal" | "production";
export type PartialedIcon = TPartialBy<Pick<Omit<Icon, "size" | "color"> & { color: IconColor }, "color" | "name">, "color">;

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

    public updateNavigationTreeByAbsolutePath(
        root: NavigationTree | null,
        absolutePath: string,                         // e.g. "/home/settings/profile"
        updateFn: (node: NavigationTree) => void | NavigationTree,
        currentPath: string = ""                      // internal tracker
    ): boolean {
        if (root == null) {
            return false;
        }
        const fullPath = `${currentPath}${root.routerLink.baseString}`.replace(/\/+/g, "/");

        // Check if this node matches the absolute path exactly
        if (fullPath === absolutePath) {
            const result = updateFn(root);
            if (result instanceof NavigationTree && root.parent) {
                const idx = root.parent.children.findIndex(
                    c => c.routerLink.baseString === root.routerLink.baseString
                );
                if (idx !== -1) { root.parent.children[idx] = result; }
            }
            return true;
        }

        // Otherwise, recurse into children
        for (const child of root.children) {
            if (this.updateNavigationTreeByAbsolutePath(child, absolutePath, updateFn, `${fullPath}/`)) {
                return true;
            }
        }

        return false;
    }

    public updateNavigationTree(
        root: NavigationTree,
        path: string[],               // list of routerLink.baseString along the path
        updateFn: (node: NavigationTree) => void | NavigationTree, // called when found
        currentPath: string[] = []    // used internally during recursion
    ): NavigationTree {
        const fullPath = [...currentPath, root.routerLink.baseString];

        // Check if we reached the target path
        const isMatch =
            fullPath.length === path.length &&
            fullPath.every((segment, i) => segment === path[i]);

        if (isMatch) {
            // Apply the update function to this node
            const result = updateFn(root);
            if (result instanceof NavigationTree) {
                // If updateFn returns a new node, replace it in parent's children
                if (root.parent) {
                    const index = root.parent.children.findIndex(
                        c => c.routerLink.baseString === root.routerLink.baseString
                    );
                    if (index !== -1) { root.parent.children[index] = result; }
                }
            }
            return root;
        }

        // Recursively search children
        for (const child of root.children) {
            if (this.updateNavigationTree(child, path, updateFn, fullPath)) {
                return root; // stop when found
            }
        }

        return root;
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
    public setChildToCurrentNode(childNavigationTree: NavigationTree): NavigationTree {
        const nodeFoundInArr = this.children?.some(child => child.id === childNavigationTree.id);
        if (nodeFoundInArr) {
            throw new Error(`NavigationTree with id '${childNavigationTree.id}' already exists as child of '${this.id}'`);
        }
        this.children = [...this.children, childNavigationTree];
        this.setChild(this.id, childNavigationTree);
        return this;
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
        return this;
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
        "Consumption",
        "Grid",
        // "Storage",
    ];
}
