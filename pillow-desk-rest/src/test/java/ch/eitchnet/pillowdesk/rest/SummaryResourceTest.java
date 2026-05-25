package ch.eitchnet.pillowdesk.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.StrolchModelConstants;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.parameter.StringParameter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SummaryResourceTest extends AbstractPillowDeskRestfulTest {

	@Test
	void shouldGetSummaries() {
		String authToken = authenticate();

		LocalDate today = LocalDate.now();
		int year = today.getYear();
		int month = today.getMonthValue();

		// Add a stay first to have some data
		Order stay = new Order("summary_stay", "Summary Stay", TYPE_STAY);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new StringParameter(PARAM_GUEST_NAME, "Guest Name", "John Doe"));
		bag.addParameter(new DateParameter(PARAM_CHECK_IN, "Check-In", new Date())); // today
		bag.addParameter(new DateParameter(PARAM_CHECK_OUT, "Check-Out",
				new Date(System.currentTimeMillis() + 2 * 86400000))); // +2 days
		bag.addParameter(new IntegerParameter(PARAM_ADULTS, "Adults", 2));
		bag.addParameter(new IntegerParameter(PARAM_CHILDREN, "Children", 0));
		bag.addParameter(new li.strolch.model.parameter.BooleanParameter(PARAM_IS_AIR_BNB, "Is Airbnb", true));
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

		try (Response response = target()
				.path("pillowdesk/stays")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.post(jakarta.ws.rs.client.Entity.json(
						stay.accept(new StrolchRootElementToJsonVisitor()).toString()))) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		}

		// Month summary
		try (Response response = target()
				.path("pillowdesk/summaries/month")
				.queryParam("year", year)
				.queryParam("month", month)
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonArray data = result.get("data").getAsJsonArray();
			assertEquals(2, data.size());
			JsonObject summary = data.get(0).getAsJsonObject();
			assertEquals("Room 1", summary.get("room").getAsString());

			JsonObject totals = result.get("totals").getAsJsonObject();
			assertEquals(1, totals.get("bookingsCount").getAsInt());
			assertEquals(2, totals.get("totalNights").getAsInt());
			assertEquals(1, totals.get("airbnbBookings").getAsInt());
			assertEquals(0, totals.get("directBookings").getAsInt());
		}

		// Year summary
		try (Response response = target()
				.path("pillowdesk/summaries/year")
				.queryParam("year", year)
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonArray data = result.get("data").getAsJsonArray();
			assertEquals(1, data.size());
			JsonObject summary = data.get(0).getAsJsonObject();
			assertEquals(1, summary.get("bookingsCount").getAsInt());
			assertEquals(2, summary.get("totalNights").getAsInt());

			JsonObject totals = result.get("totals").getAsJsonObject();
			assertEquals(1, totals.get("bookingsCount").getAsInt());
			assertEquals(2, totals.get("totalNights").getAsInt());
		}
	}
}
