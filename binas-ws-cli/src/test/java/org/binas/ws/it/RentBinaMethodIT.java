package org.binas.ws.it;

import org.binas.ws.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.fail;

public class RentBinaMethodIT extends BaseIT {
    private String validEmail1 = "valid.email@valid.domain";
    private String validEmail2 = "valid1.email@valid.domain";
    private String stationID = "A58_Station1";
    private String noStationID = "station ID";
    UserView user;
    StationView station;


    @Before
    public void setUp() {
        try {
            client.testInit(10);
            user = client.activateUser(validEmail1);
            station = client.getInfoStation(stationID);
            client.testInitStation(stationID, 1, 1, 20, 2);
        } catch (EmailExists_Exception | InvalidEmail_Exception | InvalidStation_Exception e) {
            //TODO SOMETHING HERE
            System.out.println("yay error");
        } catch (BadInit_Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = InvalidStation_Exception.class)
    public void noStationView() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception {
       client.rentBina(noStationID, validEmail1);
    }

    @Test(expected = AlreadyHasBina_Exception.class)
    public void userHasBina() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception {
        client.rentBina(stationID, validEmail1);
        client.rentBina(stationID, validEmail1);
    }

    @Test(expected = NoCredit_Exception.class)
    public void userHasNoCredit() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception {
        createUserWithCredits(0);
        client.rentBina(stationID, validEmail1);
    }

    @Test(expected = NoCredit_Exception.class)
    public void userHasNegativeCredit() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception {
        createUserWithCredits(-1);
        client.rentBina(stationID, validEmail1);
    }

    @Test(expected = UserNotExists_Exception.class)
    public void userNotExist() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception {
        client.rentBina(stationID, validEmail2);
    }

    @Test(expected = NoBinaAvail_Exception.class)
    public void noBinaAvail() throws UserNotExists_Exception, InvalidStation_Exception,
            AlreadyHasBina_Exception, NoBinaAvail_Exception, NoCredit_Exception, BadInit_Exception {
        client.testInitStation(stationID, station.getCoordinate().getX(), station.getCoordinate().getY(), 0, 2);
        client.rentBina(stationID, validEmail1);
    }

    @After
    public void tearDown(){
        client.testClear();
    }

    private void createUserWithCredits(int credits){
        try{
            client.testInit(credits);
            user = client.activateUser(validEmail1);

        } catch(BadInit_Exception e){
            e.printStackTrace();
        } catch(InvalidEmail_Exception e){
            System.out.println("Error activating user with credits test, invalid email");
        } catch(EmailExists_Exception e){
            System.out.println("Error activating user with credits test, email exists");
        }

    }

}
