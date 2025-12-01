// @ts-strict-ignore
import { Injectable, WritableSignal, signal } from "@angular/core";
import { Subject, takeUntil } from "rxjs";
import { ChannelAddress, Edge, Service } from "../../shared";

@Injectable()
export abstract class DataService {

    /** Used to retrieve values */
    public currentValue: WritableSignal<{ allComponents: { [id: string]: any } }> = signal({ allComponents: {} });
    public lastUpdated: WritableSignal<Date | null> = signal(new Date());

    protected edge: Edge | null = null;
    protected stopOnDestroy: Subject<void> = new Subject<void>();
    protected timestamps: string[] = [];

    constructor(service: Service) {
        service.getCurrentEdge().then((edge) => {
            this.edge = edge;
            edge.currentData.pipe(takeUntil(this.stopOnDestroy))
                .subscribe(() => this.lastUpdated.set(new Date()));
        });
    }

    /**
   * Gets the values from passed channelAddresses
   *
   * @param channelAddress the channelAddresses to be subscribed
   * @param edge the edge
   * @param componentId the componentId
   */
    public abstract getValues(channelAddress: ChannelAddress[], edge: Edge, componentId?: string);

    /**
   * Unsubscribes from passed channels
   *
   * @param channels the channels
   */
    public abstract unsubscribeFromChannels(channels: ChannelAddress[]);

    public abstract refresh(ev: CustomEvent);
}
