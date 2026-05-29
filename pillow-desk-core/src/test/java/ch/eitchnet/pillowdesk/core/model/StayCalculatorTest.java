package ch.eitchnet.pillowdesk.core.model;

import ch.eitchnet.pillowdesk.core.policy.StayCalculatorPolicy;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.parameter.BooleanParameter;
import li.strolch.model.parameter.DateParameter;
import li.strolch.model.parameter.FloatParameter;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.ParameterBag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StayCalculatorTest {

    private static final Logger log = LoggerFactory.getLogger(StayCalculatorTest.class);

    @Test
    public void testStandardRate() {
        Resource rate = createRate(50.0, 0.0, false, 0.0, 0.0, 0.0);
        rate.setId("rate_standard");
        log.info("Testing standard rate calculation");
        Order stay = createStay(
                ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
                ZonedDateTime.parse("2026-06-04T10:00:00+02:00"),
                2, 0);

        StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

        assertEquals(3, costs.nights());
        assertEquals(150.0, costs.accommodationGross());
        assertEquals(12.0, costs.touristTax()); // 3 nights * (2*2 + 1*0) = 12
        assertEquals(162.0, costs.totalGross());
    }

    @Test
    public void testAirBnbRate() {
        // AirBnb Non-Refundable: 1 adult, 2 nights at 57 CHF.
        // accommodation = 2 * 57 = 114
        // extraGuests = 1 - 1 = 0
        // discount = 114 * 0.1 = 11.4
        // bookingAmount = 114 + 0 - 11.4 = 102.6
        // hostFee = 102.6 * 0.03 * 1.081 = 3.32871
        // payout = 102.6 - 3.32871 = 99.27129 -> 99.27
        Resource rate = createRate(57.0, 0.0, true, 3.0, 10.0, 8.1);
        Order stay = createStay(
                ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
                ZonedDateTime.parse("2026-06-03T10:00:00+02:00"),
                1, 0);

        StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

        assertEquals(2, costs.nights());
        assertEquals(102.60, costs.accommodationGross(), 0.01);
        assertEquals(99.27, costs.payout(), 0.01);
        assertEquals(4.0, costs.touristTax());
    }

    @Test
    public void testAirBnbRateExample() {
        // nights=2, nightlyRate=57.0, guests=2, extraGuestRate=35.0, discount=10.0, hostFee=3.0, vat=8.1
        // accommodation = 2 * 57 = 114
        // guestFees = 2 * (2-1) * 35 = 70
        // discount = 114 * 0.1 = 11.4
        // bookingAmount = 114 + 70 - 11.4 = 172.6
        // hostFee = 172.6 * 0.03 * 1.081 = 5.597418
        // payout = 172.6 - 5.597418 = 167.002582 -> 167.00

        Resource rate = createRate(57.0, 35.0, true, 3.0, 10.0, 8.1);
        Order stay = createStay(
                ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
                ZonedDateTime.parse("2026-06-03T10:00:00+02:00"),
                2, 0);

        StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

        assertEquals(2, costs.nights());
        assertEquals(172.6, costs.accommodationGross(), 0.01);
        assertEquals(167.0, costs.payout(), 0.01);
    }

    private Resource createRate(double basePrice, double extraGuestRate, boolean isAirBnb, double serviceFee, double discount, double vat) {
        Resource rate = new Resource("rate", "Rate", TYPE_RATE);
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

    private Order createStay(ZonedDateTime checkIn, ZonedDateTime checkOut, int adults, int children) {
        Order stay = new Order("stay", "Stay", TYPE_STAY);
        ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
        bag.addParameter(new DateParameter(PARAM_CHECK_IN, "Check-In", Date.from(checkIn.toInstant())));
        bag.addParameter(new DateParameter(PARAM_CHECK_OUT, "Check-Out", Date.from(checkOut.toInstant())));
        bag.addParameter(new IntegerParameter(PARAM_ADULTS, "Adults", adults));
        bag.addParameter(new IntegerParameter(PARAM_CHILDREN, "Children", children));
        stay.addParameterBag(bag);
        return stay;
    }
}
