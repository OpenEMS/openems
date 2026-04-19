export namespace InetUtils {
    export enum IpType {
        None = 0,
        IPv4 = 4,
        IPv6 = 6,
    }

    export const IPV4_PATTERN: RegExp = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    export const IPV6_PATTERN: RegExp = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|:((:[0-9a-fA-F]{1,4}){1,7})|::|([0-9a-fA-F]{1,4}:){1}(:[0-9a-fA-F]{1,4}){1,6}|([0-9a-fA-F]{1,4}:){2}(:[0-9a-fA-F]{1,4}){1,5}|([0-9a-fA-F]{1,4}:){3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){6}:[0-9a-fA-F]{1,4})$/;
    export const HOSTNAME_PATTERN: RegExp = /^([A-Za-z0-9][A-Za-z0-9-]*\.)*[A-Za-z][A-Za-z0-9-]*\.?$/;

    export function isIPv4(value: string): boolean {
        return IPV4_PATTERN.test(value);
    }

    export function isIPv6(value: string): boolean {
        return IPV6_PATTERN.test(value);
    }

    export function isIP(value: string): IpType {
        if (isIPv4(value)) {
            return IpType.IPv4;
        }
        if (isIPv6(value)) {
            return IpType.IPv6;
        }
        return IpType.None;
    }

    export function isValidIP(value: string): boolean {
        return isIPv4(value) || isIPv6(value);
    }

    /**
     * Check if input string is a valid hostname according to RFC 1123 and RFC 952.
     * As the syntax of Hostnames and IPv4-Addresses have a intersection, valid IPv4-Addresses are not considered hostnames.
     *
     * ```js
     * InetUtils.isHostname('openems.io'); // returns true
     * InetUtils.isHostname('localhost'); // returns true
     * InetUtils.isHostname('127.0.0.1'); // returns false
     * InetUtils.isHostname(''); // returns false
     * ```
     * @param value to check for hostname
     * @returns true if it is a valid hostename
     */
    export function isHostname(value: string): boolean {
        return HOSTNAME_PATTERN.test(value) && !isIPv4(value);
    }

    /**
     * Check if string represents a valid Hostname, IPv4 or IPv6.
     *
     * ```js
     * InetUtils.isHostnameOrIp('openems.io'); // returns true
     * InetUtils.isHostnameOrIp('localhost'); // returns true
     * InetUtils.isHostnameOrIp('127.0.0.1'); // returns true
     * InetUtils.isHostnameOrIp('::1'); // returns true
     * InetUtils.isHostnameOrIp(''); // returns false
     * ```
     * @param value te check
     * @returns true if string is a valid Hostname, IPv4 or IPv6.
     */
    export function isHostnameOrIp(value: string): boolean {
        return isValidIP(value) || isHostname(value);
    }
}
