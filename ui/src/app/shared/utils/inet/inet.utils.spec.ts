import { InetUtils } from "./inet.utils";

const VALID_IPV4 = "192.168.0.1";
const VALID_IPV6 = "2001:0db8:1234:5678:9999::ABCD:EFff";
const VALID_HOSTNAME = "openems.io";

describe("InetUtils", () => {

    it("#isSubnetMask", () => {
        expect(InetUtils.isSubnetMask("255.255.255.255")).toBeTrue();
        expect(InetUtils.isSubnetMask("255.255.255.0")).toBeTrue();
        expect(InetUtils.isSubnetMask("255.255.192.0")).toBeTrue();
        expect(InetUtils.isSubnetMask("255.240.0.0")).toBeTrue();
        expect(InetUtils.isSubnetMask("128.0.0.0")).toBeTrue();

        expect(InetUtils.isSubnetMask("255.255.255.200")).toBeFalse();
        expect(InetUtils.isSubnetMask("255.255.255.255.255")).toBeFalse();
        expect(InetUtils.isSubnetMask("255.255.255")).toBeFalse();
        expect(InetUtils.isSubnetMask("255.0.255.0")).toBeFalse();
        expect(InetUtils.isSubnetMask("255.192.255.192")).toBeFalse();
        expect(InetUtils.isSubnetMask("0.0.0.0")).toBeFalse();
        expect(InetUtils.isSubnetMask("255:255:255:255")).toBeFalse();
        expect(InetUtils.isSubnetMask(null)).toBeFalse();
    });

    it("#isIpv4", () => {
        expect(InetUtils.isIPv4(VALID_IPV4)).toBeTrue();
        expect(InetUtils.isIPv4(VALID_IPV6)).toBeFalse();
        expect(InetUtils.isIPv4(VALID_HOSTNAME)).toBeFalse();

        expect(InetUtils.isIPv4("001.001.001.001")).toBeTrue();

        expect(InetUtils.isIPv4("1.1.1.1.1")).toBeFalse();
        expect(InetUtils.isIPv4("1.1.1")).toBeFalse();
        expect(InetUtils.isIPv4("10.0.0.256")).toBeFalse();
        expect(InetUtils.isIPv4("1.0.0.1111")).toBeFalse();
        expect(InetUtils.isIPv4("1:1:1:1")).toBeFalse();
    });

    it("#isIpv6", () => {
        expect(InetUtils.isIPv6(VALID_IPV4)).toBeFalse();
        expect(InetUtils.isIPv6(VALID_IPV6)).toBeTrue();
        expect(InetUtils.isIPv6(VALID_HOSTNAME)).toBeFalse();

        expect(InetUtils.isIPv6("::")).toBeTrue();
        expect(InetUtils.isIPv6("::1")).toBeTrue();
        expect(InetUtils.isIPv6("1::1")).toBeTrue();

        expect(InetUtils.isIPv6("1:1:1:1")).toBeFalse();
        expect(InetUtils.isIPv6("1::defg")).toBeFalse();
        expect(InetUtils.isIPv6("1111::2222::3333")).toBeFalse();
    });

    it("#isIp", () => {
        expect(InetUtils.isIP(VALID_IPV4)).toBe(InetUtils.IpType.IPv4);
        expect(InetUtils.isIP(VALID_IPV6)).toBe(InetUtils.IpType.IPv6);
        expect(InetUtils.isIP(VALID_HOSTNAME)).toBe(InetUtils.IpType.None);

        expect(InetUtils.isIP("001.001.001.001")).toBe(InetUtils.IpType.IPv4);
        expect(InetUtils.isIP("::")).toBe(InetUtils.IpType.IPv6);
        expect(InetUtils.isIP("::1")).toBe(InetUtils.IpType.IPv6);
        expect(InetUtils.isIP("1::1")).toBe(InetUtils.IpType.IPv6);

        expect(InetUtils.isIP("localhost")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isIP("")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isIP(null)).toBe(InetUtils.IpType.None);
    });

    it("#isValidIp", () => {
        expect(InetUtils.isValidIP(VALID_IPV4)).toBeTrue();
        expect(InetUtils.isValidIP(VALID_IPV6)).toBeTrue();
        expect(InetUtils.isValidIP(VALID_HOSTNAME)).toBeFalse();

        expect(InetUtils.isValidIP("001.001.001.001")).toBeTrue();
        expect(InetUtils.isValidIP("::")).toBeTrue();
        expect(InetUtils.isValidIP("::1")).toBeTrue();
        expect(InetUtils.isValidIP("1::1")).toBeTrue();

        expect(InetUtils.isValidIP("localhost")).toBeFalse();
        expect(InetUtils.isValidIP("")).toBeFalse();
        expect(InetUtils.isValidIP(null)).toBeFalse();
    });

    it("#checkHostname", () => {
        expect(InetUtils.isHostname(VALID_IPV4)).toBeFalse();
        expect(InetUtils.isHostname(VALID_IPV6)).toBeFalse();
        expect(InetUtils.isHostname(VALID_HOSTNAME)).toBeTrue();

        expect(InetUtils.isHostname("localhost")).toBeTrue();

        expect(InetUtils.isHostname("1.test")).toBeTrue();
        expect(InetUtils.isHostname("1.1")).toBeFalse();
        expect(InetUtils.isHostname("local-test")).toBeTrue();
        expect(InetUtils.isHostname("-localtest")).toBeFalse();
        expect(InetUtils.isHostname("local.-test")).toBeFalse();
        expect(InetUtils.isHostname("a-b.c")).toBeTrue();
        expect(InetUtils.isHostname("a@b.c")).toBeFalse();
        expect(InetUtils.isHostname("openems.io.")).toBeTrue();
        expect(InetUtils.isHostname(".openems.io")).toBeFalse();
        expect(InetUtils.isHostname("1.1.1.1")).toBeFalse();
        expect(InetUtils.isHostname("256.256.256.256")).toBeFalse();
        expect(InetUtils.isHostname("1:1:1:1")).toBeFalse();
        expect(InetUtils.isHostname(null)).toBeFalse();
    });

    it("#checkHostnameOrIp", () => {
        expect(InetUtils.isHostnameOrIp(VALID_IPV4)).toBeTrue();
        expect(InetUtils.isHostnameOrIp(VALID_IPV6)).toBeTrue();
        expect(InetUtils.isHostnameOrIp(VALID_HOSTNAME)).toBeTrue();

        // Localhost/Loopback
        expect(InetUtils.isHostnameOrIp("127.0.0.1")).toBeTrue();
        expect(InetUtils.isHostnameOrIp("::1")).toBeTrue();
        expect(InetUtils.isHostnameOrIp("localhost")).toBeTrue();

        // Any
        expect(InetUtils.isHostnameOrIp("0.0.0.0")).toBeTrue();
        expect(InetUtils.isHostnameOrIp("::")).toBeTrue();

        expect(InetUtils.isHostnameOrIp("a@b.c")).toBeFalse();
        expect(InetUtils.isHostnameOrIp("openems::io")).toBeFalse();
        expect(InetUtils.isHostnameOrIp(".openems.io")).toBeFalse();
        expect(InetUtils.isHostnameOrIp("1:1:1:1")).toBeFalse();
        expect(InetUtils.isHostnameOrIp(null)).toBeFalse();
    });

    it("#isNetworkAddress", () => {
        expect(InetUtils.isNetworkAddress("0.0.0.0/0")).toBe(InetUtils.IpType.IPv4);
        expect(InetUtils.isNetworkAddress("1.1.1.1/24")).toBe(InetUtils.IpType.IPv4);
        expect(InetUtils.isNetworkAddress("1::1/64")).toBe(InetUtils.IpType.IPv6);;
        expect(InetUtils.isNetworkAddress("::/0")).toBe(InetUtils.IpType.IPv6);

        expect(InetUtils.isNetworkAddress("1.1.1.1/64")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress("1.1.1.1/test")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress("1::1/200")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress("1.1.1.1/4/8")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress("1.1.1.1")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress("localhost")).toBe(InetUtils.IpType.None);
        expect(InetUtils.isNetworkAddress(null)).toBe(InetUtils.IpType.None);
    });


    it("#isValidNetworkAddress", () => {
        expect(InetUtils.isValidNetworkAddress("0.0.0.0/0")).toBeTrue();
        expect(InetUtils.isValidNetworkAddress("1.1.1.1/24")).toBeTrue();
        expect(InetUtils.isValidNetworkAddress("1::1/64")).toBeTrue();
        expect(InetUtils.isValidNetworkAddress("::/0")).toBeTrue();

        expect(InetUtils.isValidNetworkAddress("1.1.1.1/64")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress("1.1.1.1/test")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress("1::1/200")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress("1.1.1.1/4/8")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress("1.1.1.1")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress("localhost")).toBeFalse();
        expect(InetUtils.isValidNetworkAddress(null)).toBeFalse();
    });

});
