package handlers;

import example.ws.handler.MACHandler;
import org.binas.ws.BinasPortImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pt.ulisboa.tecnico.sdis.kerby.*;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 *  This SOAP handler intecepts the remote calls done by binas-ws-cli for authentication,
 *  and creates a KerbyClient to authenticate with the kerby server in RNL
 */
public class KerberosServerHandler implements SOAPHandler<SOAPMessageContext> {
    private static final String VALID_SERVER_PASSWORD = "nhdchdps";
    private static final String TICKET_ELEMENT_NAME = "ticket";
    private static final String AUTH_ELEMENT_NAME = "auth";

    private CipheredView cipheredTicketView;
    private CipheredView cipheredAuthView;

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
            return handleOutboundMessage(smc);
        else
            return handleInboundMessage(smc);

    }


    /** Handles outbound messages */
    private boolean handleOutboundMessage(SOAPMessageContext smc){
        return true;
    }

    /** Handles inbound messages */
    private boolean handleInboundMessage(SOAPMessageContext smc) {
        retrieveTicketAndAuthFromMessageHeaders(smc);

        try{
            // 1. O servidor abre o ticket com a sua chave (Ks) e deve validá-lo.
            Ticket ticket = new Ticket(cipheredTicketView, BinasPortImpl.serverKey);
            BinasAuthorizationHandler.userEmailInTicket = ticket.getX();

            ticket.validate();

            System.out.println("ticket validated");
            BinasPortImpl.kcsSessionKey = ticket.getKeyXY();
            MACHandler.sessionKey = (SecretKey) BinasPortImpl.kcsSessionKey;

            // Authxy = {x, Treq}Kxy
            // so: Authcs = {c, Treq}Kcs  - ciphered with the session key between client and server

            // 2. Depois deve abrir o autenticador com a chave de sessão (Kcs) e validá-lo.
            Auth auth = new Auth(cipheredAuthView, BinasPortImpl.kcsSessionKey);
            BinasAuthorizationHandler.userEmailInAuth = auth.getX();
            auth.validate();
            System.out.println("auth validated");



        } catch(KerbyException e){
            // Ticket is invalid! send back to client
            throw new RuntimeException("InvalidTicket");
        }


        return true;
    }

    /** Obter o ticket e o auth a partir dos headers da mensagem soap */
    private boolean retrieveTicketAndAuthFromMessageHeaders(SOAPMessageContext smc){
        // get first header element
        StringWriter sw = new StringWriter();
        CipherClerk clerk = new CipherClerk();

        try{
            // get SOAP envelope header
            SOAPMessage msg = smc.getMessage();
            SOAPPart sp = msg.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();
            SOAPHeader sh = se.getHeader();

            // check header
            if (sh == null) {
                System.out.println("Header not found.");
                return true;
            }

            Name ticketName = se.createName(TICKET_ELEMENT_NAME);

            Iterator it = sh.getChildElements();
            // check header element
            if (!it.hasNext()) {
                System.out.printf("Header element %s not found.%n", TICKET_ELEMENT_NAME);
                return true;
            }


            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            // ----------------- TICKET ----------------------

            SOAPElement ticketSOAPElement = (SOAPElement) it.next();
            Document ticketDocument = builder.parse(new InputSource(new StringReader(ticketSOAPElement.getValue())));

            DOMSource ticketDOMSource = new DOMSource(ticketDocument);
            Node ticketNode = ticketDOMSource.getNode();

            cipheredTicketView = clerk.cipherFromXMLNode(ticketNode);

            // -----------------  AUTH  ----------------------

            SOAPElement authSOAPElement = (SOAPElement) it.next();
            Document authDocument = builder.parse(new InputSource(new StringReader(authSOAPElement.getValue())));

            DOMSource authDOMSource = new DOMSource(authDocument);
            Node authNode = authDOMSource.getNode();

            cipheredAuthView = clerk.cipherFromXMLNode(authNode);

        } catch(SOAPException e){
            e.printStackTrace();
        } catch(ParserConfigurationException e){
            e.printStackTrace();
        } catch(SAXException e){
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        } catch(JAXBException e){
            e.printStackTrace();
        }
        return false;
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