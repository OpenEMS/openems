import { GetEdgeConfigResponse } from "../jsonrpc/response/getEdgeConfigResponse";

export class EdgeConfig {

    constructor(source?: GetEdgeConfigResponse) {
        if (source) {
            this.components = source.result.components;
            this.factories = source.result.factories;
        }

        // Fill 'natures' map
        if (Object.keys(this.components).length != 0 && Object.keys(this.factories).length == 0) {
            console.warn("Factory definitions are missing.");
        } else {
            for (let componentId in this.components) {
                let component = this.components[componentId];
                if (component.factoryPid === "") {
                    continue; // Singleton components have no factory-PID
                }
                let factory = this.factories[component.factoryPid];
                if (!factory) {
                    console.warn("Factory definition for [" + component.factoryPid + "] is missing.");
                    continue;
                }
                for (let nature of factory.natures) {
                    if (!(nature in this.natures)) {
                        this.natures[nature] = [];
                    }
                    this.natures[nature].push(componentId);
                }
            }
        }
    }

    /**
     * Component-ID -> Component.
     */
    public readonly components: { [id: string]: EdgeConfig.Component } = {};

    /**
     * Factory-PID -> OSGi Factory.
     */
    public readonly factories: { [pid: string]: EdgeConfig.Factory } = {};

    /**
     * Nature -> Component-IDs.
     */
    public readonly natures: { [nature: string]: string[] } = {}

    public isValid(): boolean {
        return Object.keys(this.components).length > 0 && Object.keys(this.factories).length > 0;
    }

    /**
     * Get Component-IDs of Components that implement the given Nature.
     * 
     * @param nature the given Nature.
     */
    public getComponentsImplementingNature(nature: string): string[] {
        let natures = this.natures[nature];
        if (natures) {
            return natures;
        } else {
            return [];
        }
    }
}

export module EdgeConfig {

    export class Component {
        constructor(
            public readonly factoryPid: string = ""
        ) { }
    }

    export class Factory {
        constructor(
            public readonly natures: string[] = []
        ) { }
    }

}
