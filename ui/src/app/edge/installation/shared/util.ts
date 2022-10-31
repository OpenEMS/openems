import { AbstractIbn } from "../installation-systems/abstract-ibn";

export class Util {

    /**
     * Adds the IBN data to session storage safely.
     * This mehtod is specifically implemented to deal with "cyclic object value" exception, which is caused due to Object reference.
     * 
     * @param ibn The IBN
     */
    public static addIbnToSessionStorage(ibn: AbstractIbn) {
        sessionStorage.setItem('ibn', JSON.stringify(ibn, (key, value) => {
            // Do not stringify the translate service
            if (key === 'translate') return undefined;
            return value;
        }));
    }
}