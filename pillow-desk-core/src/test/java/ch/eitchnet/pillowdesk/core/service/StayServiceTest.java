package ch.eitchnet.pillowdesk.core.service;

import li.strolch.agent.api.StrolchAgent;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.StrolchModelConstants;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.StringParameter;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.service.api.ServiceHandler;
import li.strolch.service.api.ServiceResult;
import li.strolch.testbase.runtime.RuntimeMock;
import li.strolch.utils.helper.FileHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static org.junit.jupiter.api.Assertions.*;

public class StayServiceTest {

	private static final String RUNTIME_PATH = "target/StayServiceTest";
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
	public void shouldAddUpdateAndRemoveStay() {
		StrolchAgent agent = runtimeMock.getAgent();
		Certificate cert = runtimeMock.loginAdmin();

		ServiceHandler serviceHandler = agent.getComponent(ServiceHandler.class);

		// 1. Add
		Order stay = new Order("stay1", "Stay 1", TYPE_STAY);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new DateParameter(PARAM_CHECK_IN, "Check-In", new Date()));
		bag.addParameter(
				new DateParameter(PARAM_CHECK_OUT, "Check-Out", new Date(System.currentTimeMillis() + 86400000)));
		stay.addParameterBag(bag);

		ParameterBag relationsBag = new ParameterBag(BAG_RELATIONS, "Relations", "Relations");
		StringParameter roomParam = new StringParameter(PARAM_ROOM, "Room", "room_1");
		roomParam.setInterpretation(StrolchModelConstants.INTERPRETATION_RESOURCE_REF);
		roomParam.setUom(TYPE_ROOM);
		relationsBag.addParameter(roomParam);
		StringParameter rateParam = new StringParameter(PARAM_RATE, "Rate", "rate_standard");
		rateParam.setInterpretation(StrolchModelConstants.INTERPRETATION_RESOURCE_REF);
		rateParam.setUom(TYPE_RATE);
		relationsBag.addParameter(rateParam);
		stay.addParameterBag(relationsBag);

		AddStayService.AddStayArg addArg = new AddStayService.AddStayArg();
		addArg.stay = stay;

		AddStayService addService = new AddStayService();
		ServiceResult addResult = serviceHandler.doService(cert, addService, addArg);
		assertTrue(addResult.isOk(), addResult.getMessage());

		try (StrolchTransaction tx = agent.openTx(cert, StayServiceTest.class, true)) {
			assertNotNull(tx.getOrderBy(TYPE_STAY, "stay1"));
		}

		// 2. Update
		stay = stay.getClone();
		stay.setName("Updated Stay 1");
		UpdateStayService.UpdateStayArg updateArg = new UpdateStayService.UpdateStayArg();
		updateArg.stay = stay;

		UpdateStayService updateService = new UpdateStayService();
		ServiceResult updateResult = serviceHandler.doService(cert, updateService, updateArg);
		assertTrue(updateResult.isOk(), updateResult.getMessage());

		try (StrolchTransaction tx = agent.openTx(cert, StayServiceTest.class, true)) {
			assertEquals("Updated Stay 1", tx.getOrderBy(TYPE_STAY, "stay1").getName());
		}

		// 3. Remove
		RemoveStayService.RemoveStayArg removeArg = new RemoveStayService.RemoveStayArg();
		removeArg.type = TYPE_STAY;
		removeArg.id = "stay1";

		RemoveStayService removeService = new RemoveStayService();
		ServiceResult removeResult = serviceHandler.doService(cert, removeService, removeArg);
		assertTrue(removeResult.isOk(), removeResult.getMessage());

		try (StrolchTransaction tx = agent.openTx(cert, StayServiceTest.class, true)) {
			assertNull(tx.getOrderBy(TYPE_STAY, "stay1"));
		}
	}
}
