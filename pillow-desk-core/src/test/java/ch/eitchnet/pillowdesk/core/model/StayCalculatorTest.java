package ch.eitchnet.pillowdesk.core.model;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.Resource;
import li.strolch.model.parameter.BooleanParameter;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.FloatParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.parameter.StringParameter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StayCalculatorTest {

	private static final Logger log = LoggerFactory.getLogger(StayCalculatorTest.class);

	private static Resource standardRate;
	private static Resource airBnbRateRefundable;
	private static Resource airBnbRateNonRefundable;

	@BeforeAll
	public static void beforeClass() {
		standardRate = createRate("rate_standard", 50.0, 0.0, false, 0.0, 0.0, 0.0);
		airBnbRateRefundable = createRate("rate_airbnb_extra_guest", 57.0, 35.0, true, 3.0, 0.0, 8.1);
		airBnbRateNonRefundable = createRate("rate_airbnb_extra_guest", 57.0, 35.0, true, 3.0, 10.0, 8.1);
	}

	@Test
	public void testStandardRate() {
		Resource rate = standardRate;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		Order stay = createStay(ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
				ZonedDateTime.parse("2026-06-04T10:00:00+02:00"), 2, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(3, costs.nights());
		assertEquals(150.0, costs.accommodationGross());
		assertEquals(12.0, costs.touristTax()); // 3 nights * (2*2 + 1*0) = 12
		assertEquals(150.0, costs.totalGross());
	}

	@Test
	public void testAirBnbRateRefundable() {
		// AirBnb Non-Refundable: 1 adult, 3 nights at 57 CHF.
		// accommodation = 3 * 57 = 171
		// extraGuests = 1 - 1 = 0
		// discount = 171 * 0 = 0
		// bookingAmount = 171 + 0 - 0 = 170
		// hostFee = 171 * 0.03 * 1.081 = 5.55
		// payout = 171 - 5.55 = 165.45

		Resource rate = airBnbRateRefundable;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		ZonedDateTime checkIn = ZonedDateTime.parse("2026-01-22T14:00:00+02:00");
		ZonedDateTime checkOut = ZonedDateTime.parse("2026-01-25T10:00:00+02:00");
		Order stay = createStay(checkIn, checkOut, 1, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(3, costs.nights());
		assertEquals(171, costs.accommodationGross(), 0.01);
		assertEquals(165.45, costs.payout(), 0.01);
		assertEquals(6.0, costs.touristTax());
	}

	@Test
	public void testAirBnbRateRefundable2() {
		// AirBnb Non-Refundable: 2 adults, 2 nights at 57 CHF

		Resource rate = airBnbRateRefundable;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		ZonedDateTime checkIn = ZonedDateTime.parse("2026-05-15T14:00:00+02:00");
		ZonedDateTime checkOut = ZonedDateTime.parse("2026-05-17T10:00:00+02:00");
		Order stay = createStay(checkIn, checkOut, 2, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(2, costs.nights());
		assertEquals(184, costs.accommodationGross(), 0.01);
		assertEquals(178.03, costs.payout(), 0.01);
		assertEquals(8.0, costs.touristTax());
	}

	@Test
	public void testAirBnbRateNonRefundable() {
		// nights=2, nightlyRate=57.0, guests=2, extraGuestRate=35.0, discount=10.0, hostFee=3.0, vat=8.1
		// accommodation = 2 * 57 = 114
		// guestFees = 2 * (2-1) * 35 = 70
		// discount = 114 * 0.1 = 11.4
		// bookingAmount = 114 + 70 - 11.4 = 172.6
		// hostFee = 172.6 * 0.03 * 1.081 = 5.597418
		// payout = 172.6 - 5.597418 = 167.002582 -> 167.00

		Resource rate = airBnbRateNonRefundable;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		ZonedDateTime checkIn = ZonedDateTime.parse("2026-06-01T14:00:00+02:00");
		ZonedDateTime checkOut = ZonedDateTime.parse("2026-06-03T10:00:00+02:00");
		Order stay = createStay(checkIn, checkOut, 2, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(2, costs.nights());
		assertEquals(172.6, costs.accommodationGross(), 0.01);
		assertEquals(167.0, costs.payout(), 0.01);
	}

	@Test
	public void testAirBnbRateNonRefundable2() {
		// nights=2, nightlyRate=57.0, guests=2, extraGuestRate=35.0, discount=10.0, hostFee=3.0, vat=8.1
		// accommodation = 2 * 57 = 114
		// guestFees = 2 * (2-1) * 35 = 70
		// discount = 114 * 0.1 = 11.4
		// bookingAmount = 114 + 70 - 11.4 = 172.6
		// hostFee = 172.6 * 0.03 * 1.081 = 5.597418
		// payout = 172.6 - 5.597418 = 167.002582 -> 167.00

		Resource rate = airBnbRateNonRefundable;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		Order stay = createStay(ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
				ZonedDateTime.parse("2026-06-03T10:00:00+02:00"), 2, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(2, costs.nights());
		assertEquals(172.6, costs.accommodationGross(), 0.01);
		assertEquals(167.0, costs.payout(), 0.01);
	}

	@Test
	public void testAirBnbRateNonRefundable3() {
		// nights=1, nightlyRate=57.0, guests=2, extraGuestRate=35.0, discount=10.0, hostFee=3.0, vat=8.1
		// accommodation = 1 * 57 = 57
		// guestFees = 1 * (2-1) * 35 = 35
		// discount = 57 * 0.1 = 5.7
		// bookingAmount = 57 + 35 - 5.7 = 86.3
		// hostFee = 86.3 * 0.03 * 1.081 = 2.798709
		// payout = 86.3 - 2.798709 = 83.501291 -> 83.50

		Resource rate = airBnbRateNonRefundable;
		log.info("Testing {} calculation:\n{}", rate.getId(), rate.toXmlString());
		ZonedDateTime checkIn = ZonedDateTime.parse("2026-06-01T14:00:00+02:00");
		ZonedDateTime checkOut = ZonedDateTime.parse("2026-06-02T10:00:00+02:00");
		Order stay = createStay(checkIn, checkOut, 2, 0);

		StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

		assertEquals(1, costs.nights());
		assertEquals(86.30, costs.accommodationGross(), 0.01);
		assertEquals(83.50, costs.payout(), 0.01);
	}

	public static Resource createRate(String id, double basePrice, double extraGuestRate, boolean isAirBnb, double serviceFee,
			double discount, double vat) {
		Resource rate = new Resource(id, "Rate", TYPE_RATE);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new FloatParameter(PARAM_BASE_PRICE, "Base Price", basePrice));
		bag.addParameter(new FloatParameter(PARAM_EXTRA_GUEST_RATE, "Extra Guest Rate", extraGuestRate));
		bag.addParameter(new BooleanParameter(PARAM_IS_AIR_BNB, "Is AirBnb", isAirBnb));
		bag.addParameter(new FloatParameter(PARAM_SERVICE_FEE, "Service Fee", serviceFee));
		bag.addParameter(new FloatParameter(PARAM_DISCOUNT, "Discount", discount));
		bag.addParameter(new FloatParameter(PARAM_VAT, "VAT", vat));
		rate.addParameterBag(bag);
		return rate;
	}

	public static Order createStay(ZonedDateTime checkIn, ZonedDateTime checkOut, int adults, int children) {
		return createStay("stay", checkIn, checkOut, adults, children, null);
	}

	public static Order createStay(String id, ZonedDateTime checkIn, ZonedDateTime checkOut, int adults, int children,
			String rateId) {
		Order stay = new Order(id, "Stay", TYPE_STAY);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new DateParameter(PARAM_CHECK_IN, "Check-In", Date.from(checkIn.toInstant())));
		bag.addParameter(new DateParameter(PARAM_CHECK_OUT, "Check-Out", Date.from(checkOut.toInstant())));
		bag.addParameter(new IntegerParameter(PARAM_ADULTS, "Adults", adults));
		bag.addParameter(new IntegerParameter(PARAM_CHILDREN, "Children", children));
		stay.addParameterBag(bag);

		if (rateId != null) {
			ParameterBag relBag = new ParameterBag(BAG_RELATIONS, "Relations", "Relations");
			relBag.addParameter(new StringParameter(PARAM_RATE, "Rate", rateId));
			stay.addParameterBag(relBag);
		}

		return stay;
	}

	public static Resource createRateOverride(String id, Resource rate, ZonedDateTime date, double value) {
		Resource override = new Resource(id, "Override", TYPE_RATE_OVERRIDE);
		ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
		bag.addParameter(new DateParameter(PARAM_DATE, "Date", Date.from(date.toInstant())));
		bag.addParameter(new FloatParameter(PARAM_VALUE, "Value", value));
		override.addParameterBag(bag);

		ParameterBag relBag = new ParameterBag(BAG_RELATIONS, "Relations", "Relations");
		relBag.addParameter(new StringParameter(PARAM_RATE, "Rate", rate.getId()));
		override.addParameterBag(relBag);

		return override;
	}
}
