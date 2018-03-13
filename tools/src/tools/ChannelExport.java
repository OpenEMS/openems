package tools;

import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.doc.ThingDoc;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.OpenemsException;
import io.openems.core.ClassRepository;
import io.openems.impl.device.system.asymmetricsymmetriccombinationess.AsymmetricSymmetricCombinationEssNature;
import io.openems.impl.device.system.esscluster.EssClusterNature;
import io.openems.impl.protocol.modbus.ModbusDeviceNature;

/**
 * This generates 'Readme.md' files for OpenEMS Edge devices 
 *
 */
public class ChannelExport {

	public static void main(String[] args) throws OpenemsException {
		String openemsPath = "C:\\Users\\matthias.rossmann\\Dev\\git\\openems-neu\\edge\\src";
		Collection<ThingDoc> deviceNatures;
		HashMap<Path, FileWriter> files = new HashMap<>();
		try {
			deviceNatures = ClassRepository.getInstance().getAvailableDeviceNatures();
			FileWriter devices = new FileWriter(
					Paths.get(openemsPath, "\\io\\openems\\impl\\device\\Readme.md").toFile());
			devices.write("# List of implemented Devices.\r\n\r\n");
			for (ThingDoc thingDoc : deviceNatures) {
				try {
					System.out.println(thingDoc.getClazz().getName());
					if (thingDoc.getClazz().equals(AsymmetricSymmetricCombinationEssNature.class)
							|| thingDoc.getClazz().equals(EssClusterNature.class) || thingDoc.getClazz().isInterface()
							|| Modifier.isAbstract(thingDoc.getClazz().getModifiers())) {
						continue;
					}
					Path p = Paths.get(openemsPath,
							thingDoc.getClazz().getName().replaceAll("[^\\.]*$", "").replace(".", "/"), "Readme.md");
					FileWriter fw;
					if (files.containsKey(p)) {
						fw = files.get(p);
					} else {
						fw = new FileWriter(p.toFile());
						files.put(p, fw);
						fw.write("");
					}
					fw.append("# " + thingDoc.getTitle() + "\r\n" + thingDoc.getText()
							+ "\r\n\r\nFollowing Values are implemented:\r\n\r\n" + "|ChannelName|Unit|\r\n"
							+ "|---|---|\r\n");
					devices.append("* [" + thingDoc.getTitle() + "]("
							+ Paths.get(thingDoc.getClazz().getName().replaceAll("io.openems.impl.device.", "")
									.replaceAll("[^\\.]*$", "").replace(".", "/"), "Readme.md").toString()
									.replace("\\", "/")
							+ ")\r\n");
					Thing thing = thingDoc.getClazz().getConstructor(String.class, Device.class).newInstance("", null);
					if (thing instanceof ModbusDeviceNature) {
						((ModbusDeviceNature) thing).init();
					}
					List<ChannelDoc> channelDocs = new LinkedList<>(thingDoc.getChannelDocs());
					Collections.sort(channelDocs, new Comparator<ChannelDoc>() {

						@Override
						public int compare(ChannelDoc arg0, ChannelDoc arg1) {
							return arg0.getName().compareTo(arg1.getName());
						}
					});
					for (ChannelDoc channelDoc : channelDocs) {
						Member member = channelDoc.getMember();
						try {
							List<Channel> channels = new ArrayList<>();
							if (member instanceof Method) {
								if (((Method) member).getReturnType().isArray()) {
									Channel[] ch = (Channel[]) ((Method) member).invoke(thing);
									for (Channel c : ch) {
										channels.add(c);
									}
								} else {
									// It's a Method with ReturnType Channel
									channels.add((Channel) ((Method) member).invoke(thing));
								}
							} else if (member instanceof Field) {
								// It's a Field with Type Channel
								channels.add((Channel) ((Field) member).get(thing));
							} else {
								continue;
							}
							if (channels.isEmpty()) {
								System.out.println("Channel is returning null! Thing [" + thing.id() + "], Member ["
										+ member.getName() + "]");
								continue;
							}
							for (Channel channel : channels) {
								if (channel != null) {
									StringBuilder unit = new StringBuilder();
									if (channel instanceof ReadChannel) {
										ReadChannel rchannel = ((ReadChannel) channel);
										unit.append(rchannel.unitOptional());
										rchannel.getLabels().forEach((key, value) -> {
											unit.append(key + ": " + value + "<br/>");
										});
									}
									fw.append("|" + channel.id() + "|" + unit + "|\r\n");
								}
							}
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							System.out.println("Unable to add Channel. Member [" + member.getName() + "]");
						}
					}
				} catch (NoSuchMethodException e) {

				}
			}
			for (FileWriter fw : files.values()) {
				fw.close();
			}
			devices.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
