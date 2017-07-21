export interface CustomFieldDefinition {
    [thing: string]: {
        [channel: string]: {
            title: string,
            map?: {
                [value: string]: string
            },
            unit?: string
        }
    }
}