package io.openems.edge.phoenixcontact.plcnext.common.mapper;

/**
 * Exception thrown on mapping errors
 */
public class PlcNextGdsDataMappingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PlcNextGdsDataMappingException(String message) {
		super(message);
	}

	public PlcNextGdsDataMappingException(String message, Throwable t) {
		super(message, t);
	}

}
