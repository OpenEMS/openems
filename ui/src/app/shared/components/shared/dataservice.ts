// @ts-strict-ignore
import { Injectable, WritableSignal, signal } from "@angular/core";
import { Subject, takeUntil } from "rxjs";
import { ChannelAddress, CurrentData, Edge, Service } from "../../shared";

@Injectable({ providedIn: "root" })
export abstract class DataService {
    /** Used to retrieve values */
    public currentValue: WritableSignal<CurrentData> = signal({ allComponents: {} });
    public lastUpdated: WritableSignal<Date | null> = signal(new Date());

    protected edge: Edge | null = null;
    protected stopOnDestroy: Subject<void> = new Subject<void>();
    protected timestamps: string[] = [];

    constructor(service: Service) {
        service.getCurrentEdge().then((edge) => {
            this.edge = edge;
            edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(() => this.lastUpdated.set(new Date()));
        });
    }

    /**
     * Gets the values from passed channelAddresses
     *
     * @param channelAddress The channelAddresses to be subscribed
     * @param edge The edge
     * @param componentId The componentId
     */
    public abstract subscribeChannels(channelAddress: ChannelAddress[], edge: Edge, componentId?: string): void;

    /**
     * Unsubscribes from passed channels
     *
     * @param channels The channels
     */
    public abstract unsubscribeFromChannels(channels: ChannelAddress[]);

    public abstract refresh(ev: CustomEvent);
}
