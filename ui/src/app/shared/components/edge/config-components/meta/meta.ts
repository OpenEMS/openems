import { EdgeConfig } from "../../edgeconfig";

export class MetaComponent extends EdgeConfig.Component {

    private longitude: number | null = null;
    private latitude: number | null = null;

    constructor(
        config: EdgeConfig | null,
    ) {
        const component = config?.getComponentSafelyOrDefault("_meta") ?? new EdgeConfig.Component();
        super(component.id, component.alias, component.isEnabled, false, component.factoryId, component.properties, component.channels);
        this.longitude = this.getPropertyFromComponent<number>("longitude");
        this.latitude = this.getPropertyFromComponent<number>("latitude");
    }

    public getCoordinates(): { longitude: number | null, latitude: number | null } {
        return { longitude: this.longitude, latitude: this.latitude };
    }

    public hasValidCoordinates(): boolean {
        return this.latitude != null && this.longitude != null &&
            this.latitude >= -90 && this.latitude <= 90 &&
            this.longitude >= -180 && this.longitude <= 180;
    }
}
