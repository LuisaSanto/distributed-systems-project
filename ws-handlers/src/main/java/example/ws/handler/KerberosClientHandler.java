package example.ws.handler;

import pt.ulisboa.tecnico.sdis.kerby.*;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClient;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClientException;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 *  This SOAP handler intecerpts the remote calls done by binas-ws-cli for authentication,
 *  and creates a KerbyClient to authenticate with the kerby server in RNL
 */
public class KerberosClientHandler implements SOAPHandler<SOAPMessageContext> {
    public static final String REQUIRE_AUTHENTICATION_PROPERTY = "requireAuthenticationProperty";
    public static final String ENDPOINT_ADDRESS_PROPERTY = "endpointAddressProperty";

    public static final String KERBY_WS_URL = "http://sec.sd.rnl.tecnico.ulisboa.pt:8888/kerby";
    private static final String VALID_CLIENT_NAME = "alice@A58.binas.org";
    private static final String VALID_CLIENT_PASSWORD = "r6p67xdOV";
    private static SecureRandom randomGenerator = new SecureRandom();
    private static final int VALID_DURATION = 30;

    private static final String VALID_SERVER_NAME = "binas@A58.binas.org";

    private static CipheredView ticketForServer;
    private static CipheredView authenticatorForServer;
    private static CipheredView mensagemCifradaEnviada;
    private static String digestMACEnviado;


    /**
     * Gets the header blocks that can be processed by this Handler instance. If
     * null, processes all.
     */
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    /**
     * The handleMessage method is invoked for normal processing of inbound and
     * outbound messages.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if(outbound)
            handleOutboundMessage(smc);
        else
            handleInboundMessage(smc);

        return true;
    }


    // TODO MAC HANDLER ??? separate class file, gets called in the chain after authentication!! not here


    /** Handles outbound messages */
    private boolean handleOutboundMessage(SOAPMessageContext smc){
        boolean requireAuthentication = (boolean) smc.get(REQUIRE_AUTHENTICATION_PROPERTY);

        if(requireAuthentication){
            authenticateWithKerby();
        }

        // TODO send auth, ticket and create request and send to KerberosServerHandler by putting inside the soap message context smc

        return true;
    }

    /** Authenticate with Kerby */
    private void authenticateWithKerby(){
        try{
            KerbyClient kerbyClient = new KerbyClient(KERBY_WS_URL);

            // nounce to prevent replay attacks
            long nounce = randomGenerator.nextLong();

            // 1. authenticate user and get ticket and session key by requesting a ticket to kerby
            SessionKeyAndTicketView sessionKeyAndTicketView = kerbyClient.requestTicket(VALID_CLIENT_NAME, VALID_SERVER_NAME, nounce, VALID_DURATION);

            // 2. generate a key from alice's password, to decipher and retrieve the session key with the Kc (client key)
            Key aliceKey = SecurityHelper.generateKeyFromPassword(VALID_CLIENT_PASSWORD);

            // NOTE: SessionKey : {Kc.s , n}Kc
            // to get the actual session key, we call getKeyXY
            Key sessionKey = new SessionKey(sessionKeyAndTicketView.getSessionKey(), aliceKey).getKeyXY();

            // 3. save ticket for server
            ticketForServer = sessionKeyAndTicketView.getTicket();

            // 4. create authenticator (Auth)
            Auth authToBeCiphered = new Auth(VALID_CLIENT_NAME, new Date());
            // cipher the auth with the session key Kcs
            authenticatorForServer = authToBeCiphered.cipher(sessionKey);

            // TODO contexto resposta do KerberosServerHandler

        } catch(KerbyClientException e){
            e.printStackTrace();
        } catch(BadTicketRequest_Exception e){
            e.printStackTrace();
        } catch(KerbyException e){
            e.printStackTrace();
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        } catch(InvalidKeySpecException e){
            e.printStackTrace();
        }

    }

    /** Handles inbound messages received from KerberosServerHandler */
    private boolean handleInboundMessage(SOAPMessageContext smc){


        return true;
    }


    /** The handleFault method is invoked for fault message processing. */
    @Override
    public boolean handleFault(SOAPMessageContext smc) {
        System.out.println("Ignoring fault message...");
        return true;
    }

    /**
     * Called at the conclusion of a message exchange pattern just prior to the
     * JAX-WS runtime dispatching a message, fault or exception.
     */
    @Override
    public void close(MessageContext messageContext) {
        // nothing to clean up
    }

}