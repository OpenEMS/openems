import type { EdgeConfig } from "./edgeconfig";

/**
 * Base class for component wrappers that must not have a runtime dependency on 'edgeconfig'. Extending
 * {@link EdgeConfig.Component} directly would create a circular import edgeconfig -> widgets -> ... -> this module ->
 * edgeconfig, whose 'extends' dereferences a not yet initialized module.
 */
export class EdgeComponent {
    constructor(
        public id: string = "",
        public alias: string = "",
        public isEnabled: boolean = false,
        public showProperties: boolean = false,
        public readonly factoryId: string = "",
        public readonly properties: { [key: string]: any } = {},
        public readonly channels?: { [channelId: string]: EdgeConfig.ComponentChannel },
    ) {}

    public static of(component: EdgeComponent | null): EdgeComponent | null {
        if (component == null) {
            return null;
        }
        return new EdgeComponent(
            component.id,
            component.alias,
            component.isEnabled,
            component.showProperties ?? false,
            component.factoryId,
            component.properties,
            component.channels ?? {},
        );
    }

    /* Safely gets a property from a component, if it exists, else returns null.
     *
     * @param component The component from which to retrieve the property.
     * @param property The property name to retrieve.
     * @returns The property value if it exists, otherwise null.
     */
    public getPropertyFromComponent<T>(property: string): T | null {
        return this.properties[property] ?? null;
    }

    /**
     * Checks if property has a given value
     *
     * @param propertyName - The name of the property to check.
     * @param value - The value to compare against.
     * @returns True if the property exists and has the given value; otherwise, false.
     */
    public hasPropertyValue<T>(propertyName: string, value: T): boolean {
        const propertyValue = this.getPropertyFromComponent<T>(propertyName);
        if (propertyValue == null) {
            return false;
        }

        if (typeof value === "boolean" && typeof propertyValue === "string") {
            return propertyValue.toLowerCase() === String(value);
        }

        if (typeof value === "string" && typeof propertyValue === "boolean") {
            return String(propertyValue) === value.toLowerCase();
        }

        if (typeof value === "number" && typeof propertyValue === "string") {
            return Number(propertyValue) === value;
        }

        if (typeof value === "string" && typeof propertyValue === "number") {
            return propertyValue === Number(value);
        }

        return propertyValue === value;
    }
}
