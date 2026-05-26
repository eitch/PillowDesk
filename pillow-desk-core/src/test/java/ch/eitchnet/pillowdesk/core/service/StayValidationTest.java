package ch.eitchnet.pillowdesk.core.service;

import li.strolch.agent.api.StrolchAgent;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.StrolchModelConstants;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.parameter.StringParameter;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.service.api.ServiceHandler;
import li.strolch.service.api.ServiceResult;
import li.strolch.testbase.runtime.RuntimeMock;
import li.strolch.utils.helper.FileHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;
import java.util.List;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static org.junit.jupiter.api.Assertions.*;

public class StayValidationTest {

	private static final String RUNTIME_PATH = "target/StayValidationTest";
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

	@BeforeEach
	public void beforeEach() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		try (StrolchTransaction tx = agent.openTx(cert, StayValidationTest.class, false)) {
			List<Order> stays = tx.streamOrders(TYPE_STAY).toList();
			for (Order stay : stays) {
				tx.remove(stay);
			}
			tx.commitOnClose();
		}
	}

	private Order createStay(String id, String name, Date checkIn, Date checkOut, String room) {
		Order stay = new Order(id, name, TYPE_STAY);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new DateParameter(PARAM_CHECK_IN, "Check-In", checkIn));
		bag.addParameter(new DateParameter(PARAM_CHECK_OUT, "Check-Out", checkOut));
		bag.addParameter(new IntegerParameter(PARAM_ADULTS, "Adults", 1));
		bag.addParameter(new IntegerParameter(PARAM_CHILDREN, "Children", 0));
		bag.addParameter(new StringParameter(PARAM_GUEST_NAME, "Guest Name", name));
		stay.addParameterBag(bag);

		ParameterBag relationsBag = new ParameterBag(BAG_RELATIONS, "Relations", "Relations");
		StringParameter roomParam = new StringParameter(PARAM_ROOM, "Room", room);
		roomParam.setInterpretation(StrolchModelConstants.INTERPRETATION_RESOURCE_REF);
		roomParam.setUom(TYPE_ROOM);
		relationsBag.addParameter(roomParam);
		StringParameter rateParam = new StringParameter(PARAM_RATE, "Rate", "rate_standard");
		rateParam.setInterpretation(StrolchModelConstants.INTERPRETATION_RESOURCE_REF);
		rateParam.setUom(TYPE_RATE);
		relationsBag.addParameter(rateParam);
		stay.addParameterBag(relationsBag);

		return stay;
	}

	@Test
	public void shouldNotAllowOverlappingStays() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		long day = 86400000;
		Date now = new Date();
		Date tomorrow = new Date(now.getTime() + day);
		Date dayAfterTomorrow = new Date(now.getTime() + 2 * day);

		// 1. Add first stay
		Order stay1 = createStay("stay1", "Stay 1", now, tomorrow, "room_1");
		AddStayService.AddStayArg addArg1 = new AddStayService.AddStayArg();
		addArg1.stay = stay1;
		ServiceResult addResult1 = serviceHandler.doService(cert, new AddStayService(), addArg1);
		assertTrue(addResult1.isOk(), addResult1.getMessage());

		// 2. Add overlapping stay (same room, same dates)
		Order stay2 = createStay("stay2", "Stay 2", now, tomorrow, "room_1");
		AddStayService.AddStayArg addArg2 = new AddStayService.AddStayArg();
		addArg2.stay = stay2;
		ServiceResult addResult2 = serviceHandler.doService(cert, new AddStayService(), addArg2);
		assertFalse(addResult2.isOk(), "Should not allow overlapping stay");
		assertTrue(addResult2.getMessage().contains("overlaps"), "Error message should mention overlap");

		// 3. Add overlapping stay (same room, partially overlapping)
		Order stay3 = createStay("stay3", "Stay 3", new Date(now.getTime() + day / 2), dayAfterTomorrow, "room_1");
		AddStayService.AddStayArg addArg3 = new AddStayService.AddStayArg();
		addArg3.stay = stay3;
		ServiceResult addResult3 = serviceHandler.doService(cert, new AddStayService(), addArg3);
		assertFalse(addResult3.isOk(), "Should not allow partially overlapping stay");

		// 4. Add non-overlapping stay (same room, different dates)
		Order stay4 = createStay("stay4", "Stay 4", tomorrow, dayAfterTomorrow, "room_1");
		AddStayService.AddStayArg addArg4 = new AddStayService.AddStayArg();
		addArg4.stay = stay4;
		ServiceResult addResult4 = serviceHandler.doService(cert, new AddStayService(), addArg4);
		assertTrue(addResult4.isOk(), "Should allow non-overlapping stay: " + addResult4.getMessage());

		// 5. Add overlapping stay (different room, same dates)
		Order stay5 = createStay("stay5", "Stay 5", now, tomorrow, "room_2");
		AddStayService.AddStayArg addArg5 = new AddStayService.AddStayArg();
		addArg5.stay = stay5;
		ServiceResult addResult5 = serviceHandler.doService(cert, new AddStayService(), addArg5);
		assertTrue(addResult5.isOk(), "Should allow same dates for different room");
	}

	@Test
	public void shouldGenerateUniqueBookingIdAndIgnoreProvidedOne() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		Date now = new Date();
		Date tomorrow = new Date(now.getTime() + 86400000);

		Order stay = createStay("providedId", "Stay", now, tomorrow, "room_1");
		AddStayService.AddStayArg addArg = new AddStayService.AddStayArg();
		addArg.stay = stay;
		ServiceResult addResult = serviceHandler.doService(cert, new AddStayService(), addArg);
		assertTrue(addResult.isOk(), addResult.getMessage());

		try (StrolchTransaction tx = agent.openTx(cert, StayValidationTest.class, true)) {
			Order addedStay = tx.getOrderBy(TYPE_STAY, "providedId");
			assertNull(addedStay, "Should have ignored provided ID");

			// We don't know the generated ID, so we search for all stays
			java.util.List<Order> stays = tx.streamOrders(TYPE_STAY).toList();
			assertEquals(1, stays.size());
			Order actualStay = stays.get(0);
			assertNotEquals("providedId", actualStay.getId());
			assertNotNull(actualStay.getId());

			// Check if bookingId parameter is also set and unique
			StringParameter bookingIdParam = actualStay.getParameter(BAG_PARAMETERS, PARAM_BOOKING_ID, false);
			assertNotNull(bookingIdParam, "Booking ID parameter should be set");
			assertEquals(actualStay.getId(), bookingIdParam.getValue(), "Booking ID parameter should match Strolch ID");
		}
	}

	@Test
	public void shouldNotAllowStayShorterThanOneNight() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		Date now = new Date();
		// Same check-in and check-out means 0 nights
		Order stay = createStay("stay_short", "Short Stay", now, now, "room_1");
		AddStayService.AddStayArg addArg = new AddStayService.AddStayArg();
		addArg.stay = stay;
		ServiceResult addResult = serviceHandler.doService(cert, new AddStayService(), addArg);
		assertFalse(addResult.isOk(), "Should not allow stay shorter than 1 night");
		assertTrue(addResult.getMessage().contains("at least one night"), "Error message should mention stay length");
	}

	@Test
	public void shouldNotAllowEmptyGuestName() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		long day = 86400000;
		Date now = new Date();
		Date tomorrow = new Date(now.getTime() + day);

		Order stay = createStay("stay_no_name", "Valid Name", now, tomorrow, "room_1");
		stay.getParameter(BAG_PARAMETERS, PARAM_GUEST_NAME, true).setValue("");

		AddStayService.AddStayArg addArg = new AddStayService.AddStayArg();
		addArg.stay = stay;
		ServiceResult addResult = serviceHandler.doService(cert, new AddStayService(), addArg);
		assertFalse(addResult.isOk(), "Should not allow empty guest name");
		assertTrue(addResult.getMessage().contains("Guest name must be set"), "Error message should mention guest name");
	}

	@Test
	public void shouldNotAllowZeroPeople() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();
		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		long day = 86400000;
		Date now = new Date();
		Date tomorrow = new Date(now.getTime() + day);

		Order stay = createStay("stay_no_people", "No People", now, tomorrow, "room_1");
		stay.getParameter(BAG_PARAMETERS, PARAM_ADULTS, true).setValue(0);
		stay.getParameter(BAG_PARAMETERS, PARAM_CHILDREN, true).setValue(0);

		AddStayService.AddStayArg addArg = new AddStayService.AddStayArg();
		addArg.stay = stay;
		ServiceResult addResult = serviceHandler.doService(cert, new AddStayService(), addArg);
		assertFalse(addResult.isOk(), "Should not allow zero people");
		assertTrue(addResult.getMessage().contains("At least one adult or child"), "Error message should mention people count");
	}
}
