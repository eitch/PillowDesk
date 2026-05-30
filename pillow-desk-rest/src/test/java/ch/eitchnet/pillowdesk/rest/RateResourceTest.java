package ch.eitchnet.pillowdesk.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.model.ParameterBag;
import li.strolch.model.Resource;
import li.strolch.model.json.StrolchRootElementToJsonVisitor;
import li.strolch.model.parameter.BooleanParameter;
import li.strolch.model.parameter.FloatParameter;
import org.junit.jupiter.api.Test;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class RateResourceTest extends AbstractPillowDeskRestfulTest {

	@Test
	void shouldManageRates() {
		String authToken = authenticate();

		// 1. Add
		Resource rate = new Resource("rate_test", "Test Rate", TYPE_RATE);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new FloatParameter(PARAM_BASE_PRICE, "Base Price", 100.0));
		bag.addParameter(new FloatParameter(PARAM_SERVICE_FEE, "Service Fee", 10.0));
		bag.addParameter(new FloatParameter(PARAM_DISCOUNT, "Discount", 5.0));
		bag.addParameter(new BooleanParameter(PARAM_IS_AIR_BNB, "Is AirBnb", false));
		rate.addParameterBag(bag);

		JsonObject rateJson = rate.accept(new StrolchRootElementToJsonVisitor());

		try (Response response = target()
				.path("pillowdesk/rates")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.post(Entity.json(rateJson.toString()))) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		}

		// 2. Get
		try (Response response = target()
				.path("pillowdesk/rates/rate_test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonObject data = result.get("data").getAsJsonObject();
			assertEquals("Test Rate", data.get("name").getAsString());
		}

		// 3. Search
		try (Response response = target()
				.path("pillowdesk/rates")
				.queryParam("name", "Test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonArray data = result.get("data").getAsJsonArray();
			assertTrue(data.size() >= 1);
			
			boolean found = false;
			for (int i = 0; i < data.size(); i++) {
				if ("rate_test".equals(data.get(i).getAsJsonObject().get("id").getAsString())) {
					found = true;
					break;
				}
			}
			assertTrue(found, "Should find rate_test");
		}

		// 4. Update
		rate.setName("Updated Test Rate");
		rateJson = rate.accept(new StrolchRootElementToJsonVisitor());
		try (Response response = target()
				.path("pillowdesk/rates/rate_test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.put(Entity.json(rateJson.toString()))) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		}

		// Verify update
		try (Response response = target()
				.path("pillowdesk/rates/rate_test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonObject data = result.get("data").getAsJsonObject();
			assertEquals("Updated Test Rate", data.get("name").getAsString());
		}

		// 5. Delete
		try (Response response = target()
				.path("pillowdesk/rates/rate_test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.delete()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		}

		// 6. Get again (should be empty object, not 404)
		try (Response response = target()
				.path("pillowdesk/rates/rate_test")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonObject data = result.get("data").getAsJsonObject();
			assertEquals(0, data.size());
		}
	}
}
