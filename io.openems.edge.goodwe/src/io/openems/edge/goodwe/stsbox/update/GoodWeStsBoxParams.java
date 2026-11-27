package io.openems.edge.goodwe.stsbox.update;

import com.google.common.hash.HashCode;

public record GoodWeStsBoxParams(String type, String version, GoodWeStsBoxVersion latestVersion, HashCode checksum) {

}
