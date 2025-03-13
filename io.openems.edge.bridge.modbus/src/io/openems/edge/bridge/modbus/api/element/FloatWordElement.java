/*
 *   FloatWordElement
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program and the accompanying materials are made available under
 *   the terms of the Eclipse Public License v2.0 which accompanies this
 *   distribution, and is available at
 *
 *   https://www.eclipse.org/legal/epl-2.0
 *
 *   SPDX-License-Identifier: EPL-2.0
 *
 */

package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * A FloatWordElement represents a Float value in an {@link AbstractSingleWordElement}.
 */
public class FloatWordElement extends AbstractSingleWordElement<FloatWordElement, Float> {

    public FloatWordElement(int address) {
        super(OpenemsType.FLOAT, address);
    }
    
    @Override
    protected FloatWordElement self() {
        return this;
    }
    
    @Override
    protected Float byteBufferToValue(ByteBuffer buff) {
        Short s = buff.getShort(0);
        return s.floatValue();
    }
    
    @Override
    protected void valueToByteBuffer(ByteBuffer buff, Float value) {
        Short s = TypeUtils.getAsType(OpenemsType.SHORT, value);
        buff.putShort(s.shortValue());
    }

}