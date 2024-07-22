package io.openems.edge.predictor.lstmmodel.common;

import java.io.File;

public interface FilePathGenerator {
	/**
	 * Generates the path.
	 * 
	 * @param file     abssolute path
	 * @param fileName the name of the file
	 * @return path the path of the file
	 */
	String generatePath(File file, String fileName);

}
