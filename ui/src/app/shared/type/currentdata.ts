/**
 * Holds subscribed 'CurrentData' provided by AbstractFlatWidget.
 */
export interface CurrentData {
    allComponents: {
        [channelAddress: string]: any
    }
}