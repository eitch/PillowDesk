package ch.eitchnet.pillowdesk.core;

import li.strolch.agent.api.StrolchAgent;
import li.strolch.model.Order;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.testbase.runtime.RuntimeMock;
import li.strolch.utils.helper.FileHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.TYPE_STAY;
import static org.junit.jupiter.api.Assertions.*;

public class BookingImportTest {

	private static final String RUNTIME_PATH = "target/BookingImportTest";
	private static final String CONFIG_SRC = "src/test/resources/runtime";
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
	public void shouldImportBookings() throws Exception {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();

		// Create import directory and a test CSV file
		File importDir = new File(RUNTIME_PATH, "data/import");
		if (!importDir.exists() && !importDir.mkdirs())
			fail("Could not create import directory");

		File csvFile = new File(importDir, "test_bookings.csv");
		try (FileWriter writer = new FileWriter(csvFile)) {
			writer.write("Booking ID,Check-in,Check-out,Guest Name,Airbnb?,Price / Night,Adults,Children,Nights,Accommodation Revenue,Tourist Tax,Total Revenue,Channel / Payment,Invoice No.,Paid?,Notes,Month No.\n");
			writer.write("100,01.06.26,05.06.26,Test Guest,No,50 CHF,2,,4,200,16,216,,,,,6\n");
			writer.write("101,10.06.26,12.06.26,Airbnb Guest,Yes,,1,,2,0,4,4,,,,,6\n");
		}

		BookingImportHandler importHandler = agent.getComponent(BookingImportHandler.class);
		importHandler.importFiles();

		try (StrolchTransaction tx = agent.openTx(cert, BookingImportTest.class.getName(), true)) {
			List<Order> stays = tx.getOrderMap().getElementsBy(tx, TYPE_STAY);
			assertFalse(stays.isEmpty(), "No stays imported");
			
			Order stay100 = tx.getOrderBy(TYPE_STAY, "stay_100");
			assertNotNull(stay100, "Stay 100 not found");
			assertEquals("Test Guest", stay100.getName());
			assertEquals("rate_standard", stay100.getParameter("relations", "rate").getValue());

			Order stay101 = tx.getOrderBy(TYPE_STAY, "stay_101");
			assertNotNull(stay101, "Stay 101 not found");
			assertEquals("Airbnb Guest", stay101.getName());
			assertEquals("rate_airbnb_refundable", stay101.getParameter("relations", "rate").getValue());
			Boolean isAirBnb = stay101.getParameter("parameters", "isAirBnb", true).getValue();
			assertTrue(isAirBnb, "isAirBnb should be true for stay 101, but was: " + isAirBnb);
		}

		// Check if file moved to done
		File doneFile = new File(importDir, "done/test_bookings.csv");
		assertTrue(doneFile.exists(), "File was not moved to done directory");
		assertFalse(csvFile.exists(), "Original file still exists");
	}
}
