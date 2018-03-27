//package tools;
//
//import java.io.IOException;
//import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.HashSet;
//import java.util.Set;
//
//import com.google.common.reflect.ClassPath;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.JsonObject;
//
//import io.openems.common.exceptions.OpenemsException;
//import io.openems.common.types.ThingStateInfo;
//import io.openems.common.utils.JsonUtils;
//
///**
// * This tool transpiles OpenEMS Edge Fault and Warning Enums to TypeScript for
// * usage in OpenEMS UI.
// * 
// * @author stefan.feilmeier
// *
// */
//public class FaultsAndWarningsTranspiler {
//
//	private final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
//
//	public static void main(String[] args)
//			throws OpenemsException, InstantiationException, IllegalAccessException, IOException {
//		Path outputFile = Paths.get(".", "..", "ui", "src", "app", "device", "overview", "state", "thingstates.ts");
//		// JsonObject j = getJson();
////		writeTypeScriptFile(outputFile, j);
//		System.out.println("Wrote file " + outputFile.toAbsolutePath());
//	}
//
//	private static void writeTypeScriptFile(Path outputFile, JsonObject j) throws IOException {
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		String json = gson.toJson(j);
//		String out = "interface ThingStates { [clazz: string]: { 'faults'?: { [id: number]: string }, 'warnings'?: { [id: number]: string } } };"
//				+ "\n" + "\n" + "export const THING_STATES: ThingStates = " + json + ";";
//		Files.write(outputFile, out.getBytes(DEFAULT_CHARSET));
//	}
//
//	// private static JsonObject getJson() throws ReflectionException,
//	// OpenemsException {
//	// JsonObject j = new JsonObject();
//	// for (Class<ThingStateEnum> clazz : getEnums()) {
//	// ThingStateInfo annotation = clazz.getAnnotation(ThingStateInfo.class);
//	// if (annotation == null) {
//	// System.err.println("@ThingStateInfo is missing for Enum [" + clazz.getName()
//	// + "]");
//	// continue;
//	// }
//	// // Find existing Thing definition or create new one
//	// for (Class<?> thingClazz : annotation.reference()) {
//	// String thingClassName = thingClazz.getName();
//	// JsonObject jThing = JsonUtils.getAsOptionalJsonObject(j,
//	// thingClassName).orElse(new JsonObject());
//	//
//	// // Parse enum constants
//	// ThingStateEnum[] enoms = clazz.getEnumConstants();
//	// JsonObject jState = new JsonObject();
//	// for (ThingStateEnum enom : enoms) {
//	// String name = splitCamelCase(enom.toString());
//	// jState.addProperty(String.valueOf(enom.getValue()), name);
//	// }
//	//
//	// // Is it Fault or Warning?
//	// if (FaultEnum.class.isAssignableFrom(clazz)) {
//	// jThing.add("faults", jState);
//	// } else if (WarningEnum.class.isAssignableFrom(clazz)) {
//	// jThing.add("warnings", jState);
//	// } else {
//	// throw new OpenemsException("Neither Fault nor Warning in Enum [" +
//	// clazz.getName() + "]");
//	// }
//	//
//	// j.add(thingClassName, jThing);
//	// }
//	// }
//	// return j;
//	// }
//
////	@SuppressWarnings("unchecked")
////	private static Set<Class<ThingStateEnum>> getEnums() throws ReflectionException {
////		String topLevelPackage = "io.openems.impl";
////		Class<ThingStateEnum> searchClazz = ThingStateEnum.class;
////		Set<Class<ThingStateEnum>> clazzes = new HashSet<>();
////		try {
////			ClassPath classpath = ClassPath.from(ClassLoader.getSystemClassLoader());
////			for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(topLevelPackage)) {
////				Class<?> thisClazz = classInfo.load();
////				if (searchClazz.isAssignableFrom(thisClazz)) {
////					clazzes.add((Class<ThingStateEnum>) thisClazz);
////				}
////			}
////		} catch (IllegalArgumentException | IOException e) {
////			throw new ReflectionException(e.getMessage());
////		}
////		return clazzes;
////	}
//
//	/**
//	 * Source:
//	 * https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
//	 * 
//	 * @param s
//	 * @return
//	 */
//	private static String splitCamelCase(String s) {
//		return s.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
//				"(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
//	}
//}
