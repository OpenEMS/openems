import { UUID } from 'angular2-uuid';

export class DefaultMessages {

    public static refreshConfig() {
        return {
            device: String,
            requestId: UUID.UUID(),
            config: {
                mode: "query",
                language: 'de'
            }
        }
    };
}