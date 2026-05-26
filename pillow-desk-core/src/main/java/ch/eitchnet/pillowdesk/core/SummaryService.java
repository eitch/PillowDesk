package ch.eitchnet.pillowdesk.core;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import ch.eitchnet.pillowdesk.core.search.StaySearch;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.parameter.BooleanParameter;
import li.strolch.model.parameter.DateParameter;
import li.strolch.persistence.api.StrolchTransaction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class SummaryService {

	public record Summary(YearMonth period, long bookingsCount, long totalNights, double occupancy,
	                      double accommodationGross, double touristTax, double totalGross, long airbnbBookings,
	                      long directBookings) {
	}


	public record BookingSummary(String guestName, boolean isAirBnb, String room, long nights, double netRevenue,
	                             double touristTax) {
	}

	public record YearlySummary(int year, long bookingsCount, long totalNights, double occupancy,
	                            double accommodationGross, double touristTax, double totalGross, long airbnbBookings,
	                            long directBookings) {
	}

	public List<YearlySummary> getYearlySummaries(StrolchTransaction tx, int fromYear, int toYear) {
		ZonedDateTime from = ZonedDateTime.of(fromYear, 1, 1, 0, 0, 0, 0, ZoneId.of(tx.getAgent().getTimezone()));
		ZonedDateTime to = ZonedDateTime.of(toYear, 12, 31, 23, 59, 59, 999999999,
				ZoneId.of(tx.getAgent().getTimezone()));
		List<Summary> monthlySummaries = getMonthlySummaries(tx, from, to);

		Map<Integer, YearlySummaryData> dataMap = new TreeMap<>();
		for (Summary s : monthlySummaries) {
			int year = s.period().getYear();
			YearlySummaryData data = dataMap.computeIfAbsent(year, k -> new YearlySummaryData());
			data.bookingsCount += s.bookingsCount();
			data.totalNights += s.totalNights();
			data.accommodationGross += s.accommodationGross();
			data.touristTax += s.touristTax();
			data.totalGross += s.totalGross();
			data.airbnbBookings += s.airbnbBookings();
			data.directBookings += s.directBookings();
		}

		long roomCount = tx.getResourceCount(TYPE_ROOM);
		List<YearlySummary> summaries = new ArrayList<>();
		dataMap.forEach((year, data) -> {
			double daysInYear = LocalDate.of(year, 1, 1).isLeapYear() ? 366 : 365;
			double occupancy = roomCount == 0 ? 0 : (double) data.totalNights / (roomCount * daysInYear);
			summaries.add(new YearlySummary(year, data.bookingsCount, data.totalNights, occupancy,
					data.accommodationGross, data.touristTax, data.totalGross, data.airbnbBookings,
					data.directBookings));
		});
		return summaries;
	}

	private static class YearlySummaryData {
		long bookingsCount;
		long totalNights;
		double accommodationGross;
		double touristTax;
		double totalGross;
		long airbnbBookings;
		long directBookings;
	}

	public List<Summary> getMonthlySummaries(StrolchTransaction tx, ZonedDateTime from, ZonedDateTime to) {
		List<Order> stays = new StaySearch().dateRange(from, to).search(tx).toList();
		long roomCount = tx.getResourceCount(TYPE_ROOM);

		Map<YearMonth, SummaryData> dataMap = new TreeMap<>();

		for (Order stay : stays) {
			Resource rate = tx.getResourceByRelation(stay, PARAM_RATE, true);
			StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

			DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN);
			ZonedDateTime checkIn = checkInParam.getValueZdt();
			DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT);
			ZonedDateTime checkOut = checkOutParam.getValueZdt();

			// Increment bookings count for the starting month
			YearMonth startMonth = YearMonth.from(checkIn);
			SummaryData dataForStartMonth = dataMap.computeIfAbsent(startMonth, k -> new SummaryData());
			dataForStartMonth.bookingsCount++;

			boolean isAirBnb = isAirBnb(stay, rate);
			if (isAirBnb) {
				dataForStartMonth.airbnbBookings++;
			} else {
				dataForStartMonth.directBookings++;
			}

			// Pro-rate nights and costs
			ZonedDateTime current = checkIn;
			while (current.isBefore(checkOut) && costs.nights() > 0) {
				YearMonth currentMonth = YearMonth.from(current);
				SummaryData data = dataMap.computeIfAbsent(currentMonth, k -> new SummaryData());

				data.totalNights++;
				data.accommodationGross += costs.accommodationGross() / costs.nights();
				data.touristTax += costs.touristTax() / costs.nights();
				data.totalGross += costs.totalGross() / costs.nights();

				current = current.plusDays(1);
			}
		}

		List<Summary> summaries = new ArrayList<>();
		dataMap.forEach((month, data) -> {
			double daysInMonth = month.lengthOfMonth();
			double occupancy = roomCount == 0 ? 0 : (double) data.totalNights / (roomCount * daysInMonth);
			summaries.add(new Summary(month, data.bookingsCount, data.totalNights, occupancy, data.accommodationGross,
					data.touristTax, data.totalGross, data.airbnbBookings, data.directBookings));
		});

		return summaries;
	}

	public List<BookingSummary> getBookingSummaries(StrolchTransaction tx, ZonedDateTime from, ZonedDateTime to) {
		List<Order> stays = new StaySearch().dateRange(from, to).search(tx).toList();

		List<BookingSummary> summaries = new ArrayList<>();

		for (Order stay : stays) {
			Resource rate = tx.getResourceByRelation(stay, PARAM_RATE, true);
			StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);
			Resource room = tx.getResourceByRelation(stay, PARAM_ROOM, false);
			String roomName = room != null ? room.getName() : "Unknown";

			boolean isAirBnb = isAirBnb(stay, rate);

			DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN);
			ZonedDateTime checkIn = checkInParam.getValueZdt();
			DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT);
			ZonedDateTime checkOut = checkOutParam.getValueZdt();

			long nightsInPeriod = 0;
			double netRevenueInPeriod = 0;
			double touristTaxInPeriod = 0;

			ZonedDateTime current = checkIn;
			while (current.isBefore(checkOut) && costs.nights() > 0) {
				if (!current.isBefore(from) && current.isBefore(to)) {
					nightsInPeriod++;
					netRevenueInPeriod += costs.accommodationGross() / costs.nights();
					touristTaxInPeriod += costs.touristTax() / costs.nights();
				}
				current = current.plusDays(1);
			}

			if (nightsInPeriod > 0) {
				String guestName = stay.getParameter(BAG_PARAMETERS, PARAM_GUEST_NAME).getValue();
				summaries.add(new BookingSummary(guestName, isAirBnb, roomName, nightsInPeriod, netRevenueInPeriod,
						touristTaxInPeriod));
			}
		}

		return summaries;
	}


	private boolean isAirBnb(Order stay, Resource rate) {
		BooleanParameter isAirBnbParam = stay.getParameter(BAG_PARAMETERS, PARAM_IS_AIR_BNB, false);
		if (isAirBnbParam == null)
			isAirBnbParam = rate.getParameter(BAG_PARAMETERS, PARAM_IS_AIR_BNB, false);
		return isAirBnbParam != null && isAirBnbParam.getValue();
	}


	private static class SummaryData {
		long bookingsCount;
		long totalNights;
		double accommodationGross;
		double touristTax;
		double totalGross;
		long airbnbBookings;
		long directBookings;
	}
}
