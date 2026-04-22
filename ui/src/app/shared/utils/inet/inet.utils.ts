export namespace InetUtils {
    export enum IpType {
        None = 0,
        IPv4 = 4,
        IPv6 = 6,
    }

    export const SUBNET_MASK_PATTERN: RegExp = /^(128|192|224|240|248|252|254|255)\.0\.0\.0$|^255\.(0|128|192|224|240|248|252|254|255)\.0\.0$|^255\.255\.(0|128|192|224|240|248|252|254|255)\.0$|^255\.255\.255\.(0|128|192|224|240|248|252|254|255)$/;
    export const IPV4_PATTERN: RegExp = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    export const IPV6_PATTERN: RegExp = /^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|:((:[0-9a-fA-F]{1,4}){1,7})|::|([0-9a-fA-F]{1,4}:){1}(:[0-9a-fA-F]{1,4}){1,6}|([0-9a-fA-F]{1,4}:){2}(:[0-9a-fA-F]{1,4}){1,5}|([0-9a-fA-F]{1,4}:){3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){6}:[0-9a-fA-F]{1,4})$/;
    export const HOSTNAME_PATTERN: RegExp = /^([A-Za-z0-9][A-Za-z0-9-]*\.)*[A-Za-z][A-Za-z0-9-]*\.?$/;

    /**
     * Checks whether a string is a valid IPv4 address.
     *
     * @param value the input string
     * @returns true if the value is a valid IPv4 address
     */
    export function isIPv4(value: string): boolean {
        return value != null && IPV4_PATTERN.test(value);
    }

    /**
     * Checks whether a string is a valid IPv6 address.
     *
     * @param value the input string
     * @returns true if the value is a valid IPv6 address
     */
    export function isIPv6(value: string): boolean {
        return value != null && IPV6_PATTERN.test(value);
    }

    /**
     * Checks whether a string is a valid IPv4 subnet mask.
     *
     * @param value the input string
     * @returns true if the value is a valid subnet mask
     */
    export function isSubnetMask(value: string): boolean {
        return value != null && SUBNET_MASK_PATTERN.test(value);
    }

    /**
     * Detects whether a string is IPv4, IPv6 or neither.
     *
     * @param value the input string
     * @returns the detected IP type
     */
    export function isIP(value: string): IpType {
        if (isIPv4(value)) {
            return IpType.IPv4;
        }
        if (isIPv6(value)) {
            return IpType.IPv6;
        }
        return IpType.None;
    }

    /**
     * Checks whether a string is either a valid IPv4 or IPv6 address.
     *
     * @param value the input string
     * @returns true if the value is a valid IP address
     */
    export function isValidIP(value: string): boolean {
        return isIPv4(value) || isIPv6(value);
    }

    /**
     * Checks whether a string is a valid network address in CIDR notation.
     *
     * @param value the input string in the format "address/prefix"
     * @returns the detected network address type
     */
    export function isNetworkAddress(value: string): IpType {
        if (value === null || value.length == 0) { return IpType.None; }

        const parts: string[] = value.split("/");
        if (parts.length != 2) { return IpType.None; }

        const cidrNum: number = Number.parseInt(parts[1], 10);
        if (Number.isNaN(cidrNum)) { return IpType.None; }

        const ipType = isIP(parts[0]);
        if (ipType === IpType.IPv4 && isValidIPv4Cidr(cidrNum)) {
            return IpType.IPv4;
        }
        if (ipType === IpType.IPv6 && isValidIPv6Cidr(cidrNum)) {
            return IpType.IPv6;
        }
        return IpType.None;
    }

    /**
     * Checks whether a string is a valid IPv4 or IPv6 network address in CIDR notation.
     *
     * @param value the input string
     * @returns true if the value is a valid network address
     */
    export function isValidNetworkAddress(value: string): boolean {
        const type = isNetworkAddress(value);
        return type === IpType.IPv4 || type === IpType.IPv6;
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
        return value != null && HOSTNAME_PATTERN.test(value) && !isIPv4(value);
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
     * @param value to check
     * @returns true if string is a valid Hostname, IPv4 or IPv6.
     */
    export function isHostnameOrIp(value: string): boolean {
        return isValidIP(value) || isHostname(value);
    }

    /**
     * Check if number is a valid CIDR.
     *
     *
     * ```js
     * InetUtils.isValidIPv4Cidr(24); // returns true
     * InetUtils.isValidIPv4Cidr(-1); // returns false
     * InetUtils.isValidIPv4Cidr(100); // returns false
     * InetUtils.isValidIPv4Cidr(null); // returns false
     * ```
     * @param value  to check
     * @returns true if valid ipv4 CIDR
     */
    export function isValidIPv4Cidr(value: number): boolean {
        return Number.isFinite(value) && value >= 0 && value <= 32;
    }

    /**
     * Check if number is a valid CIDR.
     *
     *
     * ```js
     * InetUtils.isValidIPv6Cidr(24); // returns true
     * InetUtils.isValidIPv6Cidr(-1); // returns false
     * InetUtils.isValidIPv6Cidr(200); // returns false
     * InetUtils.isValidIPv6Cidr(null); // returns false
     * ```
     * @param value  to check
     * @returns true if valid ipv6 CIDR
     */
    export function isValidIPv6Cidr(value: number): boolean {
        return Number.isFinite(value) && value >= 0 && value <= 128;
    }
}
