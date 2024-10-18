package io.openems.edge.io.gpio;

import static io.openems.edge.io.gpio.hardware.HardwareType.MODBERRY_X500_M40804_W;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.gpio.api.AbstractGpioChannel;
import io.openems.edge.io.gpio.api.ReadChannelId;
import io.openems.edge.io.gpio.api.WriteChannelId;

public class ModberryCM4Test {

	private File root;

	private static final List<AbstractGpioChannel> CHANNEL_IDS = List.of(//
			new ReadChannelId(18, "DigitalInput1"), //
			new ReadChannelId(19, "DigitalInput2"), //
			new ReadChannelId(20, "DigitalInput3"), //
			new ReadChannelId(21, "DigitalInput4"), //
			new WriteChannelId(22, "DigitalOutput1"), //
			new WriteChannelId(23, "DigitalOutput2"), //
			new WriteChannelId(24, "DigitalOutput3"), //
			new WriteChannelId(25, "DigitalOutput4") //
	);

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void setUp() throws IOException {
		try {
			this.folder.create();
			this.root = this.folder.getRoot();
			new File(this.root.getAbsolutePath() + "/gpio").mkdir();
			this.fileWithDirectoryAssurance(this.root.getAbsolutePath() + File.separator + "gpio", "export");
		} catch (IOException ioe) {
			throw new IOException("error creating temporary test file in " + this.getClass().getSimpleName());
		}
		for (var channelId : CHANNEL_IDS) {
			this.createDirectoryForGpio(this.folder, channelId.gpio);
		}
	}

	private File fileWithDirectoryAssurance(String directory, String filename) {
		File dir = new File(directory);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new File(directory + File.separatorChar + filename);
	}

	private void createDirectoryForGpio(TemporaryFolder root, int gpioNumber) throws IOException {
		var rootPath = root.getRoot().getAbsolutePath();
		var basePath = rootPath + File.separatorChar + "gpio" + File.separatorChar + "gpio" + gpioNumber;
		var valueFile = this.fileWithDirectoryAssurance(basePath, "value");
		Files.writeString(valueFile.toPath(), "0", Charset.defaultCharset());
		this.fileWithDirectoryAssurance(basePath, "direction");
	}

	private String readGpioFile(File root, int gpioNumber) throws IOException {
		var path = Path.of(String.join(File.separator, root.getAbsolutePath(), "gpio", "gpio" + gpioNumber, "value"));
		return Files.readString(path);
	}

	private void setGpioFile(File root, int gpioNumber, int value) throws IOException {
		var path = Path.of(String.join(File.separator, root.getAbsolutePath(), "gpio", "gpio" + gpioNumber, "value"));
		Files.writeString(path, String.valueOf(value));
	}

	@Test
	public void testChannelIdsAreCorrect() throws Exception {
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		IoGpio modberryComponent = new IoGpioImpl();
		new ComponentTest(modberryComponent).activate(config);
		assertNotNull(modberryComponent.channel("DigitalInput1"));
	}

	@Test
	public void testComponentLoadsSucesfully() throws Exception {
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		new ComponentTest(new IoGpioImpl()) //
				.activate(config);
	}

	@Test
	public void testInputValuesAreDefault() throws Exception {
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		new ComponentTest(new IoGpioImpl()) //
				.activate(config) //
				.next(new TestCase("Default input values are false") //
						.output(new ChannelAddress("io0", "DigitalInput1"), false) //
						.output(new ChannelAddress("io0", "DigitalInput2"), false) //
						.output(new ChannelAddress("io0", "DigitalInput3"), false) //
						.output(new ChannelAddress("io0", "DigitalInput4"), false) //
				);
	}

	@Test
	public void testChangeOutputWrittenToFs() throws Exception {
		assertEquals(this.readGpioFile(this.root, 22), "0");
		assertEquals(this.readGpioFile(this.root, 23), "0");
		assertEquals(this.readGpioFile(this.root, 24), "0");
		assertEquals(this.readGpioFile(this.root, 25), "0");
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		new ComponentTest(new IoGpioImpl()) //
				.activate(config) //
				.next(new TestCase("Write values are written to fs.") //
						.input(new ChannelAddress("io0", "DigitalOutput1"), true) //
						.input(new ChannelAddress("io0", "DigitalOutput2"), true) //
						.input(new ChannelAddress("io0", "DigitalOutput3"), true) //
						.input(new ChannelAddress("io0", "DigitalOutput4"), true) //
				);
		assertEquals(this.readGpioFile(this.root, 22), "1");
		assertEquals(this.readGpioFile(this.root, 23), "1");
		assertEquals(this.readGpioFile(this.root, 24), "1");
		assertEquals(this.readGpioFile(this.root, 25), "1");
	}

	@Test
	public void testChangeInputIsDetected() throws Exception {
		assertEquals(this.readGpioFile(this.root, 18), "0");
		assertEquals(this.readGpioFile(this.root, 19), "0");
		assertEquals(this.readGpioFile(this.root, 20), "0");
		assertEquals(this.readGpioFile(this.root, 21), "0");

		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();

		new ComponentTest(new IoGpioImpl()) //
				.activate(config) //
				.next(new TestCase("Read values are detected by the component.") //
						.output(new ChannelAddress("io0", "DigitalInput1"), false) //
						.output(new ChannelAddress("io0", "DigitalInput2"), false) //
						.output(new ChannelAddress("io0", "DigitalInput3"), false) //
						.output(new ChannelAddress("io0", "DigitalInput4"), false) //
				);

		this.setGpioFile(this.root, 18, 1);
		this.setGpioFile(this.root, 19, 1);
		this.setGpioFile(this.root, 20, 1);
		this.setGpioFile(this.root, 21, 1);

		assertEquals(this.readGpioFile(this.root, 18), "1");
		assertEquals(this.readGpioFile(this.root, 19), "1");
		assertEquals(this.readGpioFile(this.root, 20), "1");
		assertEquals(this.readGpioFile(this.root, 21), "1");

		new ComponentTest(new IoGpioImpl()) //
				.activate(config) //
				.next(new TestCase("Read values are detected by the component.") //
						.output(new ChannelAddress("io0", "DigitalInput1"), true) //
						.output(new ChannelAddress("io0", "DigitalInput2"), true) //
						.output(new ChannelAddress("io0", "DigitalInput3"), true) //
						.output(new ChannelAddress("io0", "DigitalInput4"), true) //
				);
	}

	@Test
	public void testJavaApi() throws Exception {
		this.setGpioFile(this.root, 22, 0);
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		var componentManager = new DummyComponentManager();
		var componentTest = new ComponentTest(new IoGpioImpl()) //
				.activate(config);
		componentManager.addComponent(componentTest.getSut());

		// Get get component channel value as java reference
		WriteChannel<Boolean> writeChannel = componentManager.getChannel(new ChannelAddress("io0", "DigitalOutput1"));
		assertFalse(writeChannel.value().isDefined());
		writeChannel.setNextValue(true);
	}

	@Test
	public void testInterfaceDigitalOutputChannels() throws Exception {
		this.setGpioFile(this.root, 22, 0);
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		var componentManager = new DummyComponentManager();
		var componentTest = new ComponentTest(new IoGpioImpl()) //
				.activate(config);
		componentManager.addComponent(componentTest.getSut());
		var comp = componentTest.getSut();
		assertTrue(((DigitalOutput) comp).digitalOutputChannels().length == 4);
	}

	@Test
	public void testInterfaceDigitalInputChannels() throws Exception {
		this.setGpioFile(this.root, 22, 0);
		var config = MyConfig.create() //
				.setId("io0") //
				.setAlias("io0") //
				.setEnabled(true) //
				.setGpioPath(this.folder.getRoot().getAbsolutePath()) //
				.setHardwareType(MODBERRY_X500_M40804_W) //
				.build();
		var componentManager = new DummyComponentManager();
		var componentTest = new ComponentTest(new IoGpioImpl()) //
				.activate(config);
		componentManager.addComponent(componentTest.getSut());
		var comp = componentTest.getSut();
		assertTrue(((DigitalInput) comp).digitalInputChannels().length == 4);
	}
}
