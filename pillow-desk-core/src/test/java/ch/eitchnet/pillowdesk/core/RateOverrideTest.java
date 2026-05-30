package ch.eitchnet.pillowdesk.core;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.agent.api.StrolchAgent;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.testbase.runtime.RuntimeMock;
import li.strolch.utils.helper.FileHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZonedDateTime;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_RATE;
import static ch.eitchnet.pillowdesk.core.model.StayCalculatorTest.createRateOverride;
import static ch.eitchnet.pillowdesk.core.model.StayCalculatorTest.createStay;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateOverrideTest {

	private static final String RUNTIME_PATH = "target/RateOverrideTest";
	private static final String CONFIG_SRC = "src/test/resources/runtime";
	private static final Logger log = LoggerFactory.getLogger(RateOverrideTest.class);
	private static RuntimeMock runtimeMock;

	@BeforeAll
	public static void beforeAll() {
		File rootPath = new File(RUNTIME_PATH);
		if (rootPath.exists())
			FileHelper.deleteFile(rootPath, true);

		runtimeMock = new RuntimeMock();
		runtimeMock.mockRuntime(RUNTIME_PATH, CONFIG_SRC);
		runtimeMock.startContainer();
	}

	@AfterAll
	public static void afterAll() {
		if (runtimeMock != null)
			runtimeMock.destroyRuntime();
	}

	@Test
	public void shouldCalculateWithOverrides() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();

		try (StrolchTransaction tx = agent.openTx(cert, RateOverrideTest.class.getName(), false)) {
			Resource rate = tx.getResourceBy(TYPE_RATE, "rate_standard");

			// 2026-05-15 to 2026-05-17 (2 nights)
			Order stay = createStay("stay_ov", ZonedDateTime.parse("2026-05-15T14:00:00Z"),
					ZonedDateTime.parse("2026-05-17T10:00:00Z"), 2, 0, "rate_standard");

			// Default: 50 * 2 = 100
			StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(tx, stay, rate);
			assertEquals(100.0, costs.accommodationGross());

			// Add override for 16. May 2026 -> 75
			Resource override = createRateOverride("ov1", rate, ZonedDateTime.parse("2026-05-16T00:00:00Z"), 75.0);
			tx.add(override);

			// Add override for ANOTHER rate on the same date -> should not affect calculation
			Resource otherRate = tx.getResourceBy(TYPE_RATE, "rate_student");
			Resource otherOverride = createRateOverride("ov2", otherRate, ZonedDateTime.parse("2026-05-16T00:00:00Z"), 10.0);
			tx.add(otherOverride);

			// Calculation: 15. May (50) + 16. May (75) = 125
			costs = StayCalculatorPolicy.calculate(tx, stay, rate);
			assertEquals(125.0, costs.accommodationGross());

			tx.commitOnClose();
		}
	}

	@Test
	public void shouldCalculateWithOverrides2() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();

		try (StrolchTransaction tx = agent.openTx(cert, RateOverrideTest.class.getName(), false)) {
			Resource rate = tx.getResourceBy(TYPE_RATE, "rate_airbnb_refundable");

			// 2026-05-15 to 2026-05-17 (2 nights)
			ZonedDateTime checkIn = ZonedDateTime.parse("2026-05-15T14:00:00+02:00");
			ZonedDateTime checkOut = ZonedDateTime.parse("2026-05-17T10:00:00+02:00");
			Order stay = createStay(checkIn, checkOut, 2, 0);

			// Default: 57 * 2 = 100
			StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(tx, stay, rate);
			assertEquals(179, costs.payout(), 0.01);

			// Add override for 16. May 2026 -> 58
			Resource override = createRateOverride("ov2_1", rate, ZonedDateTime.parse("2026-05-16T00:00:00+02:00"), 58.0);
			log.info(override.toXmlString());
			tx.add(override);

			// Calculation: 15. May (57) + 16. May (58)
			costs = StayCalculatorPolicy.calculate(tx, stay, rate);
			assertEquals(179.0, costs.payout());

			tx.commitOnClose();
		}
	}

	//
	//	@Test
	//	public void testStandardRateWithOverride() {
	//		Resource rate = standardRate;
	//		log.info("Testing {} calculation with override:\n{}", rate.getId(), rate.toXmlString());
	//		ZonedDateTime checkIn = ZonedDateTime.parse("2026-06-01T14:00:00+02:00");
	//		ZonedDateTime checkOut = ZonedDateTime.parse("2026-06-04T10:00:00+02:00");
	//		Order stay = createStay(checkIn, checkOut, 2, 0);
	//
	//		// We need a TX to test overrides, but this is a unit test.
	//		// Let's mock or just test the logic by manually calling calculate with overrides if we had a way.
	//		// Since StayCalculatorPolicy.calculate(tx, stay, rate) is static and does its own search,
	//		// we should probably use a StrolchTransaction if possible, or trust the logic.
	//		// Given the environment, I'll try to run the test and see if I can add an integration test later.
	//	}

}
