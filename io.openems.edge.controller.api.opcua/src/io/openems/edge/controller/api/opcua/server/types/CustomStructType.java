/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.openems.edge.controller.api.opcua.server.types;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.serialization.SerializationContext;
import org.eclipse.milo.opcua.stack.core.serialization.UaDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaEncoder;
import org.eclipse.milo.opcua.stack.core.serialization.UaStructure;
import org.eclipse.milo.opcua.stack.core.serialization.codecs.GenericDataTypeCodec;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import io.openems.edge.controller.api.opcua.server.ExampleNamespace;

public class CustomStructType implements UaStructure {

    public static final ExpandedNodeId TYPE_ID = ExpandedNodeId.parse(String.format(
        "nsu=%s;s=%s",
        ExampleNamespace.NAMESPACE_URI,
        "DataType.CustomStructType"
    ));

    public static final ExpandedNodeId BINARY_ENCODING_ID = ExpandedNodeId.parse(String.format(
        "nsu=%s;s=%s",
        ExampleNamespace.NAMESPACE_URI,
        "DataType.CustomStructType.BinaryEncoding"
    ));

    private final String foo;
    private final UInteger bar;
    private final boolean baz;

    public CustomStructType() {
        this(null, uint(0), false);
    }

    public CustomStructType(String foo, UInteger bar, boolean baz) {
        this.foo = foo;
        this.bar = bar;
        this.baz = baz;
    }

    public String getFoo() {
        return foo;
    }

    public UInteger getBar() {
        return bar;
    }

    public boolean isBaz() {
        return baz;
    }

    @Override
    public ExpandedNodeId getTypeId() {
        return TYPE_ID;
    }

    @Override
    public ExpandedNodeId getBinaryEncodingId() {
        return BINARY_ENCODING_ID;
    }

    @Override
    public ExpandedNodeId getXmlEncodingId() {
        // XML encoding not supported
        return ExpandedNodeId.NULL_VALUE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomStructType that = (CustomStructType) o;
        return baz == that.baz &&
            Objects.equal(foo, that.foo) &&
            Objects.equal(bar, that.bar);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(foo, bar, baz);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("foo", foo)
            .add("bar", bar)
            .add("baz", baz)
            .toString();
    }

    public static class Codec extends GenericDataTypeCodec<CustomStructType> {
        @Override
        public Class<CustomStructType> getType() {
            return CustomStructType.class;
        }

        @Override
        public CustomStructType decode(
            SerializationContext context,
            UaDecoder decoder) throws UaSerializationException {

            String foo = decoder.readString("Foo");
            UInteger bar = decoder.readUInt32("Bar");
            boolean baz = decoder.readBoolean("Baz");

            return new CustomStructType(foo, bar, baz);
        }

        @Override
        public void encode(
            SerializationContext context,
            UaEncoder encoder, CustomStructType value) throws UaSerializationException {

            encoder.writeString("Foo", value.foo);
            encoder.writeUInt32("Bar", value.bar);
            encoder.writeBoolean("Baz", value.baz);
        }
    }

}
