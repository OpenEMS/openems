import { FormGroup } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";

export type NetworkInterface = {
  dhcp: boolean,
  gateway?: string | null,
  dns?: string | null,
  linkLocalAddressing?: boolean,
  metric?: number,
  addresses?: IpAddress[]
}

export type IpAddress = {
  label: string,
  address: string,
  subnetmask: string
}

export type InterfaceForm = {
  name: string,
  formGroup: FormGroup,
  model: InterfaceModel,
  fields: FormlyFieldConfig[]
};

export type InterfaceModel = NetworkInterface & {
  addressesList?: string[],
  ip?: string | null,
  subnetmask?: string | null,
}

export type NetworkConfig = {
  interfaces: {
    [name: string]: NetworkInterface;
  }
}

export namespace NetworkUtils {

  /**
   * Converts the CIDR notation to a subnet mask string.
   * For example, converts "24" to "255.255.255.0".
   *
   * @param cidr The CIDR notation representing the subnet mask length.
   * @returns the subnetmask as a string.
   * @remarks
   * This method calculates the subnet mask based on the CIDR notation provided.
   * It splits the CIDR into octets and determines the corresponding subnet mask values.
   * The resulting subnet mask string is returned.
   *
   */
  export function getSubnetmaskAsString(cidr: number): string {
    const octets: number[] = [];
    for (let i = 0; i < 4; i++) {
      const bits = Math.min(cidr, 8);
      octets.push(256 - Math.pow(2, 8 - bits));
      cidr -= bits;
    }
    return octets.join('.');
  }

  /**
   * Converts a subnet mask to its CIDR notation.
   *
   * @param subnetmask - The subnet mask in dotted-decimal notation (e.g., "255.255.255.0").
   * @returns The CIDR notation as a number.
   *
   * @example
   * ```typescript
   * const cidr = getCidrFromSubnetmask("255.255.255.0"); // Returns 24
   * ```
   */
  export function getCidrFromSubnetmask(subnetmask: string): number {
    // Split the subnet mask into its octets, convert them to binary, and join the binary strings
    const binaryString = subnetmask
      .split('.')
      .map(Number)
      .map(part => (part >>> 0).toString(2).padStart(8, '0')) // Ensure each part is represented as 8 bits
      .join('');

    // return the number of '1's in the binary string to get the CIDR notation
    return binaryString.split('1').length - 1;
  }
}
