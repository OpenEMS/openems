import { TEnumKeys, TPartialBy } from "../../type/utility";
import { Icon, Widget, WidgetClass } from "../../type/widget";
import { ArrayUtils } from "../../utils/array/array.utils";

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

    public findParentByUrl(currentUrl: string | null): NavigationTree | null {

        /**
         * Converts a relative routerLink to absolute from root node.
         *
         * @param tree the current navigation node
            * @returns a navigationTree
         */
        function convertRelativeToAbsoluteLink(tree: NavigationTree | null): NavigationTree | null {

            /**
             * Builds the absolute link from root node to current node.
             *
             * @param node the current node
             * @returns a update navigation tree
             */
            function buildAbsoluteLink(node: NavigationTree): NavigationTree {
                const segments: (string | null)[] = [];
                const current: NavigationTree | null = node;

                segments.unshift(current.routerLink.baseString);
                segments.unshift(current?.parent?.routerLink.baseString ?? null);

                const routerLink = segments.filter(el => el != null).join("/").replace(/\/+/g, "/");
                node.routerLink.baseString = routerLink;
                return node;
            }

            /**
             * Traverses through the navigation tree.
             *
             * @param node the current node
             */
            function traverse(node: NavigationTree | null): void {

                if (node == null) {
                    return;
                }
                const _node = structuredClone(node);
                node.routerLink = buildAbsoluteLink(_node).routerLink;

                if (node.children) {
                    for (const child of node.children) {
                        traverse(child);
                    }
                }
            }

            traverse(tree);
            return tree;
        }

        /**
         * Gets the navigation id from a navigation tree and current router url
         *
         * @param tree the navigation tree
         * @param url the current router url
         * @returns the navigationId if found, else null
         */

        function findParentNode(tree: NavigationTree | null, url: string | null): NavigationTree | null {

            if (tree == null || url == null) {
                return null;
            }

            function buildRoutes(segments: string[]): string[] {
                const routes: string[] = [];

                for (let i = 1; i < segments.length; i++) {
                    const path = segments.slice(1, i + 1).join("/");
                    routes.push(path);
                }

                return routes;
            }

            const upperMostParent = tree.routerLink.baseString;
            const allRoutes = buildRoutes(url.split("/")).reverse().map(el => el.includes(upperMostParent) ? el.slice(el.indexOf(upperMostParent)) : el); // ["device", "device/fems888", "device/fems888/live"...]
            let resultTree: NavigationTree | null = null;
            for (const entry of allRoutes) {

                if (resultTree != null) {
                    continue;
                }

                function traverse(navigationTree: NavigationTree, segments: string): NavigationTree | null {
                    const urlSegments = navigationTree.routerLink.baseString.split("/");
                    const foundNode = ArrayUtils.containsAll({ strings: urlSegments, arr: segments.split("/") });
                    if (foundNode) {
                        return navigationTree;
                    }

                    for (const child of navigationTree.children) {
                        const result = traverse(child, segments);

                        if (result != null) {
                            return result;
                        }
                    }
                    return null;
                }
                const result = traverse(tree, entry);
                resultTree = result;
            }
            return resultTree;
        }

        if (currentUrl == null) {
            return null;
        }

        const flattenedNavigationTree: NavigationTree | null = convertRelativeToAbsoluteLink(this);
        return findParentNode(flattenedNavigationTree, currentUrl);
    }

    public updateNavigationTreeByAbsolutePath(
        root: NavigationTree | null, absolutePath: string, updateFn: (node: NavigationTree) => void | NavigationTree, currentPath: string = ""
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

    public getAllNavigationNodes(tree: NavigationTree[]): NavigationTree[] {
        const result: NavigationTree[] = [];

        const recursion = (node: NavigationTree) => {
            result.push(node);
            node.children?.forEach(recursion);
        };

        tree.map(recursion);

        return result;
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

        return this.setParentRecursively();
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
        return this.setParentRecursively();
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

    private setParentRecursively() {
        function traverse(node: NavigationTree, parent: NavigationTree | null): void {

            if (node.parent == null) {
                node.parent = parent;
            }

            if (node.children == null) {
                return;
            }
            for (const child of node.children) {
                traverse(child, node);
            }
        }

        traverse(this, null);
        return this;
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
     * The widgets classes to show in new navigation
     */
    export const newClasses: TEnumKeys<typeof WidgetClass>[] = [
        "Common_Autarchy",
        "Common_Selfconsumption",
        "Consumption",
        "Grid",
    ];

    /**
     * The widget factories to show in new navigation
     */
    export const newWidgets: Widget["name"][] = ["System.Fenecon.Industrial.L"];
}
