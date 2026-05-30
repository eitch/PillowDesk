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
		DateParameter checkInParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_IN);
		ZonedDateTime checkIn = checkInParam.getValueZdt();
		DateParameter checkOutParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHECK_OUT);
		ZonedDateTime checkOut = checkOutParam.getValueZdt();

		IntegerParameter adultsParam = stay.getParameter(BAG_PARAMETERS, PARAM_ADULTS, false);
		int adults = adultsParam == null ? 2 : adultsParam.getValue();
		IntegerParameter childrenParam = stay.getParameter(BAG_PARAMETERS, PARAM_CHILDREN, false);
		int children = childrenParam == null ? 0 : childrenParam.getValue();

		FloatParameter basePriceParam = rate.getParameter(BAG_PARAMETERS, PARAM_BASE_PRICE);
		double basePrice = basePriceParam.getValue();
		FloatParameter serviceFeeParam = rate.getParameter(BAG_PARAMETERS, PARAM_SERVICE_FEE);
		double serviceFee = serviceFeeParam.getValue();
		FloatParameter discountParam = rate.getParameter(BAG_PARAMETERS, PARAM_DISCOUNT);
		double discount = discountParam.getValue();

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
			FloatParameter extraGuestRateParam = rate.getParameter(BAG_PARAMETERS, PARAM_EXTRA_GUEST_RATE, false);
			double extraGuestRate = extraGuestRateParam == null ? 0.0 : extraGuestRateParam.getValue();
			FloatParameter vatParam = rate.getParameter(BAG_PARAMETERS, PARAM_VAT, false);
			double vatPercent = vatParam == null ? 0.0 : vatParam.getValue();

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

		FloatParameter totalRevenueP = stay.getParameter(BAG_PARAMETERS, PARAM_TOTAL_REVENUE, false);
		if (totalRevenueP == null) {
			totalRevenueP = new FloatParameter(PARAM_TOTAL_REVENUE, "Total Revenue", costs.totalGross);
			stay.addParameter(BAG_PARAMETERS, totalRevenueP);
		} else {
			totalRevenueP.setValue(costs.payout());
		}

		FloatParameter payoutP = stay.getParameter(BAG_PARAMETERS, PARAM_PAYOUT, false);
		if (payoutP == null) {
			payoutP = new FloatParameter(PARAM_PAYOUT, "Payout", costs.payout());
			stay.addParameter(BAG_PARAMETERS, payoutP);
		} else {
			payoutP.setValue(costs.payout());
		}
	}
}
