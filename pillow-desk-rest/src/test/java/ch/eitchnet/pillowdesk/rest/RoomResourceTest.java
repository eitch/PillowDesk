package ch.eitchnet.pillowdesk.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomResourceTest extends AbstractPillowDeskRestfulTest {

	@Test
	void shouldGetRooms() {
		String authToken = authenticate();

		try (Response response = target()
				.path("pillowdesk/rooms")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonArray data = result.get("data").getAsJsonArray();

			// There should be at least one room from Model.xml
			assertFalse(data.isEmpty());

			boolean foundRoom1 = false;
			for (int i = 0; i < data.size(); i++) {
				if ("room_1".equals(data.get(i).getAsJsonObject().get("id").getAsString())) {
					foundRoom1 = true;
					break;
				}
			}
			assertTrue(foundRoom1, "Should find room_1");
		}
	}

	@Test
	void shouldGetRates() {
		String authToken = authenticate();

		try (Response response = target()
				.path("pillowdesk/rates")
				.request(MediaType.APPLICATION_JSON)
				.header("Authorization", authToken)
				.get()) {
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject result = JsonParser.parseString(response.readEntity(String.class)).getAsJsonObject();
			JsonArray data = result.get("data").getAsJsonArray();

			// There should be at least two rates from Model.xml (standard and airbnb)
			assertTrue(data.size() >= 2);
		}
	}
}
