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

import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static ch.eitchnet.pillowdesk.core.model.ModelConstants.*;

public class StayCalculatorTest {

    @Test
    public void testStandardRate() {
        Resource rate = createRate(50.0, false, 0.0, 0.0);
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
        // Expect: 102.60 CHF accommodation gross (114 * 0.9), ~99.52 CHF net (after 3% fee), 4 CHF tax.
        Resource rate = createRate(57.0, true, 0.03, 0.10);
        Order stay = createStay(
                ZonedDateTime.parse("2026-06-01T14:00:00+02:00"),
                ZonedDateTime.parse("2026-06-03T10:00:00+02:00"),
                1, 0);

        StayCalculatorPolicy.StayCosts costs = StayCalculatorPolicy.calculate(stay, rate);

        assertEquals(2, costs.nights());
        assertEquals(102.60, costs.accommodationGross(), 0.01);
        assertEquals(99.52, costs.accommodationNet(), 0.01);
        assertEquals(4.0, costs.touristTax());
        assertEquals(106.60, costs.totalGross(), 0.01);
    }

    private Resource createRate(double basePrice, boolean isAirBnb, double serviceFee, double discount) {
        Resource rate = new Resource("rate", "Rate", TYPE_RATE);
        ParameterBag bag = new ParameterBag(BAG_PARAMETERS, "Parameters", "Parameters");
        bag.addParameter(new FloatParameter(PARAM_BASE_PRICE, "Base Price", basePrice));
        bag.addParameter(new BooleanParameter(PARAM_IS_AIR_BNB, "Is AirBnb", isAirBnb));
        bag.addParameter(new FloatParameter(PARAM_SERVICE_FEE, "Service Fee", serviceFee));
        bag.addParameter(new FloatParameter(PARAM_DISCOUNT, "Discount", discount));
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
