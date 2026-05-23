package ch.eitchnet.pillowdesk.rest;

import ch.eitchnet.pillowdesk.core.SummaryService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.helper.ResponseUtil;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static li.strolch.rest.StrolchRestfulConstants.STROLCH_CERTIFICATE;
import static li.strolch.utils.helper.ExceptionHelper.getCallerMethod;

@Path("pillowdesk/summaries")
@Tag(name = "Summaries", description = "Endpoints for generating summaries.")
public class SummaryResource {

	private static Certificate getCertificate(HttpServletRequest request) {
		return (Certificate) request.getAttribute(STROLCH_CERTIFICATE);
	}

	private static StrolchTransaction openTx(Certificate certificate) {
		return RestfulStrolchComponent.getInstance().openTx(certificate, getCallerMethod());
	}

	@GET
	@Path("month")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMonthlySummary(@Context HttpServletRequest request, @QueryParam("year") int year,
			@QueryParam("month") int month) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			SummaryService service = new SummaryService();
			YearMonth yearMonth = YearMonth.of(year, month);
			ZonedDateTime from = yearMonth.atDay(1).atStartOfDay(ZoneId.of(tx.getAgent().getTimezone()));
			ZonedDateTime to = yearMonth
					.atEndOfMonth()
					.atTime(23, 59, 59, 999999999)
					.atZone(ZoneId.of(tx.getAgent().getTimezone()));

			List<SummaryService.DailySummary> summaries = service.getDailySummaries(tx, from, to);

			JsonArray array = new JsonArray();
			for (SummaryService.DailySummary summary : summaries) {
				JsonObject obj = new JsonObject();
				obj.addProperty("date", summary.date().toString());
				obj.addProperty("room", summary.room());
				obj.addProperty("netRevenue", summary.netRevenue());
				obj.addProperty("touristTax", summary.touristTax());
				array.add(obj);
			}
			return ResponseUtil.toResponse("data", array);
		}
	}

	@GET
	@Path("year")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getYearlySummary(@Context HttpServletRequest request, @QueryParam("year") int year) {

		Certificate cert = getCertificate(request);
		try (StrolchTransaction tx = openTx(cert)) {
			SummaryService service = new SummaryService();
			ZonedDateTime from = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneId.of(tx.getAgent().getTimezone()));
			ZonedDateTime to = ZonedDateTime.of(year, 12, 31, 23, 59, 59, 999999999,
					ZoneId.of(tx.getAgent().getTimezone()));
			List<SummaryService.Summary> summaries = service.getMonthlySummaries(tx, from, to);

			JsonArray array = new JsonArray();
			for (SummaryService.Summary summary : summaries) {
				JsonObject obj = new JsonObject();
				obj.addProperty("month", summary.period().toString());
				obj.addProperty("netRevenue", summary.accommodationGross());
				obj.addProperty("touristTax", summary.touristTax());
				obj.addProperty("occupancy", summary.occupancy());
				array.add(obj);
			}
			return ResponseUtil.toResponse("data", array);
		}
	}
}
