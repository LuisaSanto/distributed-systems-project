package org.binas.station.ws.it;

import java.io.IOException;
import java.util.Properties;

import org.binas.station.ws.cli.StationClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.xml.ws.Response;

import static junit.framework.TestCase.fail;

/**
 * Base class for testing a station Load properties from test.properties
 */
public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;

	protected static StationClient client;

	@BeforeClass
	public static void oneTimeSetup() throws Exception {
		testProps = new Properties();
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Loaded test properties:");
			System.out.println(testProps);
		} catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		}

		final String uddiEnabled = testProps.getProperty("uddi.enabled");
		final String verboseEnabled = testProps.getProperty("verbose.enabled");

		final String uddiURL = testProps.getProperty("uddi.url");
		final String wsName = testProps.getProperty("ws.name");
		final String wsURL = testProps.getProperty("ws.url");

		if ("true".equalsIgnoreCase(uddiEnabled)) {
			client = new StationClient(uddiURL, wsName);
		} else {
			client = new StationClient(wsURL);
		}
		client.setVerbose("true".equalsIgnoreCase(verboseEnabled));
	}

	@AfterClass
	public static void cleanup() {
	}

    /**
     * Poll 10 times if response is ready, if not, fail
     * @param response
     */
	protected void pollResponse(Response response){
        for(int i = 10; !response.isDone(); i--){
            try{
                Thread.sleep(100);
            } catch(InterruptedException e){
                e.printStackTrace();
            }

            if(i == 0){
                fail();
            }
        }
    }

}
