/*
 * Camel Api Route test generated by camel-api-component-maven-plugin
 * Generated on: Wed May 10 14:00:35 EDT 2017
 */
package org.apache.camel.component.as2;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.component.as2.internal.AS2ApiCollection;
import org.apache.camel.component.as2.internal.AS2SendManagerApiMethod;

/**
 * Test class for {@link org.apache.camel.component.as2.api.AS2ClientManager} APIs.
 * TODO Move the file to src/test/java, populate parameter values, and remove @Ignore annotations.
 * The class source won't be generated again if the generator MOJO finds it under src/test/java.
 */
public class AS2SendManagerIntegrationTest extends AbstractAS2TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AS2SendManagerIntegrationTest.class);
    private static final String PATH_PREFIX = AS2ApiCollection.getCollection().getApiName(AS2SendManagerApiMethod.class).getName();

    private static final String AS2_NAME = "878051556";
    private static final String SUBJECT = "Test Case";
    
    public static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            +"UNH+00000000000117+INVOIC:D:97B:UN'\n"
            +"BGM+380+342459+9'\n"
            +"DTM+3:20060515:102'\n"
            +"RFF+ON:521052'\n"
            +"NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            +"NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            +"CUX+1:USD'\n"
            +"LIN+1++157870:IN'\n"
            +"IMD+F++:::WIDGET'\n"
            +"QTY+47:1020:EA'\n"
            +"ALI+US'\n"
            +"MOA+203:1202.58'\n"
            +"PRI+INV:1.179'\n"
            +"LIN+2++157871:IN'\n"
            +"IMD+F++:::DIFFERENT WIDGET'\n"
            +"QTY+47:20:EA'\n"
            +"ALI+JP'\n"
            +"MOA+203:410'\n"
            +"PRI+INV:20.5'\n"
            +"UNS+S'\n"
            +"MOA+39:2137.58'\n"
            +"ALC+C+ABG'\n"
            +"MOA+8:525'\n"
            +"UNT+23+00000000000117'\n"
            +"UNZ+1+00000000000778'\n";

    @Test
    public void testSendNoEncryptNoSign() throws Exception {
        final Map<String, Object> headers = new HashMap<String, Object>();
        // parameter type is String
        headers.put("CamelAS2.ediMessage", EDI_MESSAGE);
        // parameter type is String
        headers.put("CamelAS2.subject", SUBJECT);
        // parameter type is String
        headers.put("CamelAS2.as2From", AS2_NAME);
        // parameter type is String
        headers.put("CamelAS2.as2To", AS2_NAME);

        final org.apache.camel.component.as2.api.AS2ClientManager.Result result = requestBodyAndHeaders("direct://SENDNOENCRYPTNOSIGN", null, headers);

        assertNotNull("sendNoEncryptNoSign result", result);
        LOG.debug("sendNoEncryptNoSign: " + result);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // test route for sendNoEncryptNoSign
                from("direct://SENDNOENCRYPTNOSIGN")
                    .to("as2://" + PATH_PREFIX + "/sendNoEncryptNoSign");

            }
        };
    }
}
