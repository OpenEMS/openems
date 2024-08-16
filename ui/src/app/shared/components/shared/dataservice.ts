// @ts-strict-ignore
import { Injectable } from "@angular/core";
import { RefresherCustomEvent } from "@ionic/angular";
import { BehaviorSubject, Subject } from "rxjs";
import { ChannelAddress, Edge } from "../../shared";

@Injectable()
export abstract class DataService {

  /** Used to retrieve values */
  public currentValue: BehaviorSubject<{ allComponents: {} }> = new BehaviorSubject({ allComponents: {} });

  protected edge: Edge | null = null;
  protected stopOnDestroy: Subject<void> = new Subject<void>();
  protected timestamps: string[] = [];

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

  public abstract refresh(ev: RefresherCustomEvent);
}
