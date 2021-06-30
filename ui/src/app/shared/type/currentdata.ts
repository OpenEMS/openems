/**
 * Holds subscribed 'CurrentData' provided by AbstractFlatWidget or AbstractModal.
 */
export interface CurrentData {
    thisComponent: {
        [channelId: string]: any
    },
    allComponents: {
        [channelAddress: string]: any
    }
}