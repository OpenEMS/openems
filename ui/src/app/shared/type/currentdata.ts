/**
 * Holds subscribed 'CurrentData' provided by AbstractFlatWidget.
 */
export interface CurrentData {
    // thisComponent: {
    //     [channelId: string]: any
    // },
    allComponents: {
        [channelAddress: string]: any
    }
}