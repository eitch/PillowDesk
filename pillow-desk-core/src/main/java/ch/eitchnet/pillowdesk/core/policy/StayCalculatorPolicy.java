package ch.eitchnet.pillowdesk.core.policy;

import ch.eitchnet.pillowdesk.core.search.RateOverrideSearch;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.FloatParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.persistence.api.StrolchTransaction;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StayCalculatorPolicy {

	public record StayCosts(long nights, double touristTax, double accommodationGross, double totalGross,
	                        double payout) {
	}

	public static StayCosts calculate(StrolchTransaction tx, Order stay, Resource rate) {
		ZonedDateTime checkIn = stay.getDate(PARAM_CHECK_IN);
		ZonedDateTime checkOut = stay.getDate(PARAM_CHECK_OUT);

		int adults = stay.getInteger(PARAM_ADULTS);
		if (adults == 0) adults = 2;
		int children = stay.getInteger(PARAM_CHILDREN);

		double basePrice = rate.getDouble(PARAM_BASE_PRICE);
		double serviceFee = rate.getDouble(PARAM_SERVICE_FEE);
		double discount = rate.getDouble(PARAM_DISCOUNT);

		long nights = ChronoUnit.DAYS.between(checkIn.toLocalDate(), checkOut.toLocalDate());
		if (nights < 0)
			nights = 0;

		Map<LocalDate, Double> overrides = Map.of();
		if (tx != null) {
			overrides = new RateOverrideSearch()
					.forRate(rate.getId())
					.search(tx)
					.asStream()
					.collect(Collectors.toMap(r -> r.getDate(PARAM_DATE).toLocalDate(), r -> r.getDouble(PARAM_VALUE)));
		}

		boolean isAirBnb = rate.is(BAG_PARAMETERS, PARAM_IS_AIR_BNB);

		double touristTax = nights * (2.0 * adults + 1.0 * children);
		double accommodationGross = 0;
		double payout = 0;

		double accommodation = 0;
		for (int i = 0; i < nights; i++) {
			LocalDate date = checkIn.toLocalDate().plusDays(i);
			accommodation += overrides.getOrDefault(date, basePrice);
		}

		if (isAirBnb) {
			double extraGuestRate = rate.getDouble(PARAM_EXTRA_GUEST_RATE);
			double vatPercent = rate.getDouble(PARAM_VAT);

			int guests = adults + children;
			double extraGuests = Math.max(0, guests - 1);
			double guestFees = nights * extraGuests * extraGuestRate;
			double discountAmount = accommodation * (discount / 100.0);
			double bookingAmount = accommodation + guestFees - discountAmount;
			double hostFee = bookingAmount * (serviceFee / 100.0) * (1.0 + vatPercent / 100.0);

			accommodationGross = bookingAmount;
			double accommodationNet = bookingAmount - hostFee;
			payout = round2(accommodationNet);
		} else {
			accommodationGross = accommodation * (1.0 - discount / 100.0);
			double accommodationNet = accommodationGross * (1.0 - serviceFee / 100.0);
			payout = accommodationNet;
		}

		double totalGross = accommodationGross;

		return new StayCosts(nights, touristTax, accommodationGross, totalGross, payout);
	}

	public static StayCosts calculate(Order stay, Resource rate) {
		return calculate(null, stay, rate);
	}

	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	public static void calculateAndFill(StrolchTransaction tx, Order stay) {
		Resource rate = tx.getResourceByRelation(stay, PARAM_RATE, true);
		StayCosts costs = calculate(tx, stay, rate);

		stay.setDouble(PARAM_TOTAL_REVENUE, costs.totalGross());
		stay.setDouble(PARAM_PAYOUT, costs.payout());
	}
}
