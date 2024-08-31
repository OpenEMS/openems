package io.openems.edge.timedata.rrd4j;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdMemoryBackendFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Tuple;
import io.openems.edge.timedata.rrd4j.version.Version.CreateDatabaseConfig;
import io.openems.edge.timedata.rrd4j.version.Version3Test;
import io.openems.edge.timedata.rrd4j.version.VersionHandler;

public class Rrd4jReadHandlerTest {
	// starts at 1. January 2020 00:00:00
	private static final Instant START = Instant.ofEpochSecond(1577836800L);

	private final String rrdbId = "rrdbId";
	private RrdBackendFactory factory;
	private RrdDb db;
	private Rrd4jReadHandler readHandler;
	private DummyComponent dummyComponent;

	@Before
	public void setUp() throws Exception {
		this.factory = new RrdMemoryBackendFactory();
		final var version3 = Version3Test.createDummyVersion3();

		this.db = version3.createNewDb(new CreateDatabaseConfig(//
				this.rrdbId, //
				Unit.WATT, //
				"comp0/DummyChannel", //
				START.getEpochSecond() - 1, //
				this.factory, //
				null //
		));

		final var robin = this.db.getArchive(0).getRobin(0);
		for (int i = 0; i < robin.getSize(); i++) {
			final var value = i * 100;
			this.db.createSample(START.plus(5 * i, ChronoUnit.MINUTES).getEpochSecond()) //
					.setValues(value) //
					.update();
		}

		this.readHandler = new Rrd4jReadHandler();
		final var dcm = new DummyComponentManager();
		this.dummyComponent = new DummyComponent("comp0");
		dcm.addComponent(this.dummyComponent);
		final var rrd4jSupplier = new Rrd4jSupplier(this.factory, (t, u) -> {
			return t.toString();
		});

		final var versionHandler = new VersionHandler();
		versionHandler.bindVersion(version3);

		ReflectionUtils.setAttribute(Rrd4jReadHandler.class, this.readHandler, "componentManager", dcm);
		ReflectionUtils.setAttribute(Rrd4jReadHandler.class, this.readHandler, "rrd4jSupplier", rrd4jSupplier);
		ReflectionUtils.setAttribute(Rrd4jSupplier.class, rrd4jSupplier, "versionHandler", versionHandler);

	}

	@Test
	public void testGetArchivesSortedByArcStep() throws Exception {
		final var sorted = Rrd4jReadHandler.getArchivesSortedByArcStep(this.db);
		long lastStepSize = 0L;
		for (var archive : sorted) {
			assertTrue("The last step size should be lower than the next step size.",
					lastStepSize < archive.getArcStep());
			lastStepSize = archive.getArcStep();
		}

	}

	@Test
	public void testQueryHistoricDataWithResolution1minutes() throws Exception {
		assertEquals(values(//
				Tuple.of(START.plus(0, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(1, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(2, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(3, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(4, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(5, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(6, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(7, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(8, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(9, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(10, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(11, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(12, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(13, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(14, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(15, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(16, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(17, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(18, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(19, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(20, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(21, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(22, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(23, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(24, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(25, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500), //
				Tuple.of(START.plus(26, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500), //
				Tuple.of(START.plus(27, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500), //
				Tuple.of(START.plus(28, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500), //
				Tuple.of(START.plus(29, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500) //
		), this.query(new Resolution(1, ChronoUnit.MINUTES)));
	}

	@Test
	public void testQueryHistoricDataWithResolution5minutes() throws Exception {
		assertEquals(values(//
				Tuple.of(START.plus(0, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 0), //
				Tuple.of(START.plus(5, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(10, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 200), //
				Tuple.of(START.plus(15, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 300), //
				Tuple.of(START.plus(20, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400), //
				Tuple.of(START.plus(25, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 500) //
		), this.query(new Resolution(5, ChronoUnit.MINUTES)));
	}

	@Test
	public void testQueryHistoricDataWithResolution10minutes() throws Exception {
		assertEquals(values(//
				Tuple.of(START.plus(0, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 50), //
				Tuple.of(START.plus(10, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 250), //
				Tuple.of(START.plus(20, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 450) //
		), this.query(new Resolution(10, ChronoUnit.MINUTES)));
	}

	@Test
	public void testQueryHistoricDataWithResolution15minutes() throws Exception {
		assertEquals(values(//
				Tuple.of(START.plus(0, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 100), //
				Tuple.of(START.plus(15, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), 400) //
		), this.query(new Resolution(15, ChronoUnit.MINUTES)));
	}

	@Test
	public void testStreamRanges() throws Exception {
		final var utc = ZoneId.of("UTC");
		final var from = ZonedDateTime.of(2023, 12, 26, 0, 0, 0, 0, utc);
		final var to = ZonedDateTime.of(2024, 3, 8, 0, 0, 0, 0, utc);
		final var result = Rrd4jReadHandler.streamRanges(from, to, new Resolution(1, ChronoUnit.MONTHS)).toList();
		assertEquals(4, result.size());
		assertEquals(new Rrd4jReadHandler.Range(from, ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, utc)), result.get(0));
		assertEquals(new Rrd4jReadHandler.Range(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, utc),
				ZonedDateTime.of(2024, 2, 1, 0, 0, 0, 0, utc)), result.get(1));
		assertEquals(new Rrd4jReadHandler.Range(ZonedDateTime.of(2024, 2, 1, 0, 0, 0, 0, utc),
				ZonedDateTime.of(2024, 3, 1, 0, 0, 0, 0, utc)), result.get(2));
		assertEquals(new Rrd4jReadHandler.Range(ZonedDateTime.of(2024, 3, 1, 0, 0, 0, 0, utc), to), result.get(3));
	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> query(Resolution resolution)
			throws IllegalArgumentException, OpenemsNamedException {
		return this.readHandler.queryHistoricData(this.rrdbId, //
				START.atZone(ZoneId.of("UTC")), //
				START.plus(30, ChronoUnit.MINUTES).atZone(ZoneId.of("UTC")), //
				Set.of(this.dummyComponent.channel(DummyComponent.ChannelId.DUMMY_CHANNEL).address()), //
				resolution, false);
	}

	@SafeVarargs
	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> values(
			Tuple<ZonedDateTime, Number>... values) {
		return Arrays.stream(values) //
				.collect(toMap(Tuple::a, t -> {
					final var valueMap = new TreeMap<ChannelAddress, JsonElement>();
					valueMap.put(this.dummyComponent.channel(DummyComponent.ChannelId.DUMMY_CHANNEL).address(),
							new JsonPrimitive(t.b()));
					return valueMap;
				}, (t, u) -> u, TreeMap::new));
	}

	private class DummyComponent extends AbstractDummyOpenemsComponent<DummyComponent> implements OpenemsComponent {

		public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
			DUMMY_CHANNEL(Doc.of(OpenemsType.INTEGER)); //

			private final Doc doc;

			private ChannelId(Doc doc) {
				this.doc = doc;
			}

			@Override
			public Doc doc() {
				return this.doc;
			}
		}

		public DummyComponent(String id) {
			super(id, //
					OpenemsComponent.ChannelId.values(), //
					ChannelId.values() //
			);
		}

		@Override
		protected DummyComponent self() {
			return this;
		}

	}

}
