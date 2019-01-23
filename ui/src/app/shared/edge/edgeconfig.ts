import { GetEdgeConfigResponse } from "../jsonrpc/response/getEdgeConfigResponse";
import { Component } from "@angular/compiler/src/core";

export class EdgeConfig {

    constructor(source?: GetEdgeConfigResponse) {
        if (source) {
            this.components = source.result.components;
            this.factories = source.result.factories;
        }

        // initialize Components
        for (let componentId in this.components) {
            this.components[componentId].id = componentId;
        }

        // initialize Factorys
        for (let factoryPid in this.factories) {
            this.factories[factoryPid].pid = factoryPid;
            this.factories[factoryPid].componentIds = [];
        }

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

                // Complete 'factories' map
                factory.componentIds.push(componentId);

                // Fill 'natures' map
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
     * Get Component-IDs of Component instances by the given Factory.
     * 
     * @param nature the given Nature.
     */
    public getComponentIdsByFactory(factoryPid: string): string[] {
        let factory = this.factories[factoryPid];
        if (factory) {
            return factory.componentIds;
        } else {
            return [];
        }
    }

    /**
     * Get Component instances by the given Factory.
     * 
     * @param nature the given Nature.
     */
    public getComponentsByFactory(factoryPid: string): EdgeConfig.Component[] {
        let componentIds = this.getComponentIdsByFactory(factoryPid);
        let result: EdgeConfig.Component[] = [];
        for (let componentId of componentIds) {
            result.push(this.components[componentId]);
        }
        return result;
    }

    /**
     * Get Component-IDs of Components that implement the given Nature.
     * 
     * @param nature the given Nature.
     */
    public getComponentsImplementingNature(nature: string): string[] {
        let components = this.natures[nature];
        if (components) {
            return components;
        } else {
            return [];
        }
    }

    /**
     * Get the implemented Natures by Factory-ID.
     * 
     * @param factoryId the Factory-ID
     */
    public getNaturesByFactoryId(factoryId: string): string[] {
        let factory = this.factories[factoryId];
        if (factory) {
            return factory.natures;
        } else {
            return [];
        }
    }

    /**
     * Get the implemented Natures by Component-ID.
     * 
     * @param componentId the Component-ID
     */
    public getNaturesByComponentId(componentId: string): string[] {
        let component = this.components[componentId];
        if (!component) {
            return [];
        }
        let factoryPid = component.factoryPid;
        return this.getNaturesByFactoryId(factoryPid);
    }

    /**
     * Get the Component properties.
     * 
     * @param componentId the Component-ID
     */
    public getComponentProperties(componentId: string): { [key: string]: any } {
        let component = this.components[componentId];
        if (component) {
            return component.properties;
        } else {
            return {};
        }
    }
}

export module EdgeConfig {

    export class Component {
        public id: string = "";

        constructor(
            public readonly factoryPid: string = "",
            public readonly properties: { [key: string]: any } = {}
        ) { }
    }

    export class Factory {
        public pid: string = "";
        public componentIds: string[] = [];

        constructor(
            public readonly natures: string[] = []
        ) { }
    }

}
