package oi.github.jeallasia.tplapi;

import io.github.jeallasia.tplapi.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.*;

public class ParkingTest extends TestHelper {

    private Parking<TestCar> parking;
    private List<TestCar> cars;
    private TestCar e20, e50, gas, e20ChargeNotRequired, e50ChargeNotRequired, other;

    @Before
    public void initialize() {
        parking = Parking.<TestCar>builder().setPricingPolicy(FIVE_PER_HOUR_STARTED_ONE_FIXED)
                .addSlots(isE20, 10)
                .addSlots(isE50, 10)
                .addSlots(isGAS, (isE20.or(isE50)).and(isChargeNotRequired), 40)
                .build();
        e20 = e20();
        e50 = e50();
        gas = gas();
        e20ChargeNotRequired = e20ChargeNotRequired();
        e50ChargeNotRequired = e50ChargeNotRequired();
        other = other();

        // Note that if we park all theses cars:
        cars = e20List(5);  // -> will use 5 isE20 slot
        cars.addAll(e50List(5));  // -> will use 5 isE50 slot
        cars.addAll(gasList(5));  // -> will use 5 isGAS slot
        cars.addAll(e20ChargeNotRequiredList(10)); // -> will use the 5 remaining isE20 and then 5 isGAS
        cars.addAll(e50ChargeNotRequiredList(10)); // -> will use the 5 remaining isE50 and then 5 isGAS

    }

    @Test
    public void testCheckInResultSuccessful() {
        CheckInResult<TestCar> checkInResult = parking.checkIn(e20, dateTime6h5min);
        assertTrue(checkInResult.isSuccessful());
        ParkingSlotUsage<TestCar> inUsage = checkInResult.getUsage();
        assertEquals(e20, inUsage.getCar());
        assertEquals(dateTime6h5min, inUsage.getIncomingDateTime());
        assertEquals("0", inUsage.getSlot().getId());
        assertFalse(inUsage.isUsingAlternative());
    }

    @Test
    public void testCheckInResultFailed() {
        // Not managed car
        assertFalse(parking.checkIn(other).isSuccessful());
        // No space left
        e20List(10).forEach(parking::checkIn); // feel the 10 slots for e20 cars
        assertFalse(parking.checkIn(e20).isSuccessful()); // this one should failed
    }

    @Test
    public void testCheckOutResult() {
        assertTrue(parking.checkIn(e20, dateTime6h5min).isSuccessful());
        CheckOutResult<TestCar> checkOutResult = parking.checkOut(e20, dateTime6h55min);
        ParkingSlotUsage<TestCar> outUsage = checkOutResult.getUsage();
        assertEquals(dateTime6h5min, outUsage.getIncomingDateTime());
        assertEquals(dateTime6h55min, outUsage.getOutgoingDateTime());
        assertEquals(duration50min, outUsage.computeDuration());
        assertFalse(outUsage.isUsingAlternative());
        // Finally the price has to be 5 euros for the started hour + 1 euro fixed
        assertEquals(FIVE.add(ONE), checkOutResult.getPrice());
    }

    @Test
    public void testGetSizeFor() {
        assertEquals(10, parking.getSizeFor(e20));
        assertEquals(10, parking.getSizeFor(e50));
        assertEquals(40, parking.getSizeFor(gas));
        assertEquals(10, parking.getSizeFor(e20ChargeNotRequired));
        assertEquals(10, parking.getSizeFor(e50ChargeNotRequired));
        cars.forEach(parking::checkIn);
        assertEquals(10, parking.getSizeFor(e20));
        assertEquals(10, parking.getSizeFor(e50));
        assertEquals(40, parking.getSizeFor(gas));
        assertEquals(10, parking.getSizeFor(e20ChargeNotRequired));
        assertEquals(10, parking.getSizeFor(e50ChargeNotRequired));
    }

    @Test
    public void testGetSizeAltFor() {
        assertEquals(0, parking.getSizeAltFor(e20()));
        assertEquals(0, parking.getSizeAltFor(e50()));
        assertEquals(0, parking.getSizeAltFor(gas()));
        assertEquals(40, parking.getSizeAltFor(e20ChargeNotRequired()));
        assertEquals(40, parking.getSizeAltFor(e50ChargeNotRequired()));
        cars.forEach(parking::checkIn);
        assertEquals(0, parking.getSizeAltFor(e20()));
        assertEquals(0, parking.getSizeAltFor(e50()));
        assertEquals(0, parking.getSizeAltFor(gas()));
        assertEquals(40, parking.getSizeAltFor(e20ChargeNotRequired()));
        assertEquals(40, parking.getSizeAltFor(e50ChargeNotRequired()));
    }

    @Test
    public void testGetSizeCompatibleFor() {
        assertEquals(10, parking.getSizeCompatibleFor(e20));
        assertEquals(10, parking.getSizeCompatibleFor(e50));
        assertEquals(40, parking.getSizeCompatibleFor(gas));
        assertEquals(50, parking.getSizeCompatibleFor(e20ChargeNotRequired));
        assertEquals(50, parking.getSizeCompatibleFor(e50ChargeNotRequired));
        cars.forEach(parking::checkIn);
        assertEquals(10, parking.getSizeCompatibleFor(e20));
        assertEquals(10, parking.getSizeCompatibleFor(e50));
        assertEquals(40, parking.getSizeCompatibleFor(gas));
        assertEquals(50, parking.getSizeCompatibleFor(e20ChargeNotRequired));
        assertEquals(50, parking.getSizeCompatibleFor(e50ChargeNotRequired));
    }

    @Test
    public void testGetAvailableSizeFor() {
        assertEquals(10, parking.getAvailableSizeFor(e20));
        assertEquals(10, parking.getAvailableSizeFor(e50));
        assertEquals(40, parking.getAvailableSizeFor(gas));
        assertEquals(10, parking.getAvailableSizeFor(e20ChargeNotRequired));
        assertEquals(10, parking.getAvailableSizeFor(e50ChargeNotRequired));
        cars.forEach(parking::checkIn);
        assertEquals(0, parking.getAvailableSizeFor(e20));
        assertEquals(0, parking.getAvailableSizeFor(e50));
        assertEquals(25, parking.getAvailableSizeFor(gas));
        assertEquals(0, parking.getAvailableSizeFor(e20ChargeNotRequired));
        assertEquals(0, parking.getAvailableSizeFor(e50ChargeNotRequired));
    }

    @Test
    public void testGetAvailableSizeAltFor() {
        assertEquals(0, parking.getAvailableSizeAltFor(e20));
        assertEquals(0, parking.getAvailableSizeAltFor(e50));
        assertEquals(0, parking.getAvailableSizeAltFor(gas));
        assertEquals(40, parking.getAvailableSizeAltFor(e20ChargeNotRequired));
        assertEquals(40, parking.getAvailableSizeAltFor(e50ChargeNotRequired));
        cars.forEach(parking::checkIn);
        assertEquals(0, parking.getAvailableSizeAltFor(e20));
        assertEquals(0, parking.getAvailableSizeAltFor(e50));
        assertEquals(0, parking.getAvailableSizeAltFor(gas));
        assertEquals(25, parking.getAvailableSizeAltFor(e20ChargeNotRequired));
        assertEquals(25, parking.getAvailableSizeAltFor(e50ChargeNotRequired));
    }

    @Test
    public void testGetAvailableSizeCompatibleFor() {
        assertEquals(10, parking.getAvailableSizeCompatibleFor(e20));
        assertEquals(10, parking.getAvailableSizeCompatibleFor(e50));
        assertEquals(40, parking.getAvailableSizeCompatibleFor(gas));
        assertEquals(50, parking.getAvailableSizeCompatibleFor(e20ChargeNotRequired));
        assertEquals(50, parking.getAvailableSizeCompatibleFor(e50ChargeNotRequired));
        cars.forEach(parking::checkIn);
        assertEquals(0, parking.getAvailableSizeCompatibleFor(e20));
        assertEquals(0, parking.getAvailableSizeCompatibleFor(e50));
        assertEquals(25, parking.getAvailableSizeCompatibleFor(gas));
        assertEquals(25, parking.getAvailableSizeCompatibleFor(e20ChargeNotRequired));
        assertEquals(25, parking.getAvailableSizeCompatibleFor(e50ChargeNotRequired));
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testInvalidOutgoingDate(){
        parking.checkIn(e20, dateTime6h55min);
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Outgoing date should be after incoming date !");
        parking.checkOut(e20, dateTime6h5min);
    }

    @Test
    public void testClassicCheckInCheckOut(){
        parking.checkIn(e20);
        assertEquals(e20, parking.checkOut(e20).getUsage().getCar());
    }

}