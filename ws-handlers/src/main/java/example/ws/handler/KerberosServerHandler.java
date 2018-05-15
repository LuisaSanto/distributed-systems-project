package example.ws.handler;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import pt.ulisboa.tecnico.sdis.kerby.*;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.io.StringWriter;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 *  This SOAP handler intecerpts the remote calls done by binas-ws-cli for authentication,
 *  and creates a KerbyClient to authenticate with the kerby server in RNL
 */
public class KerberosServerHandler implements SOAPHandler<SOAPMessageContext> {
    public static final String VALID_SERVER_PASSWORD = "nhdchdps";

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
            handleOutboundMessage(smc);
        else
            handleInboundMessage(smc);

        return true;
    }



    /** Handles outbound messages */
    private boolean handleOutboundMessage(SOAPMessageContext smc){



        return true;
    }

    /** Handles inbound messages */
    private boolean handleInboundMessage(SOAPMessageContext smc){
        // TODO BinasAuthorizationHandler

        retrieveTicketAndAuthFromMessageHeaders(smc);

        try{
            // 1. O servidor abre o ticket com a sua chave (Ks) e deve validá-lo.
            Key serverKey = SecurityHelper.generateKeyFromPassword(VALID_SERVER_PASSWORD);
            Ticket ticket = new Ticket(cipheredTicketView, serverKey);

            ticket.validate();
            Key sessionKey = ticket.getKeyXY();

            // Authxy = {x, Treq}Kxy
            // so: Authcs = {c, Treq}Kcs  - ciphered with the session key between client and server

            // 2. Depois deve abrir o autenticador com a chave de sessão (Kcs) e validá-lo.
            Auth auth = new Auth(cipheredAuthView, sessionKey);

            // 3 TODO verificar integridade atraves MACHandler


            // TODO validar request time
            // 4. O servidor responde ao cliente com uma instância da classe RequestTime (da kerby-lib).
            // responde com {Treq}Kc,s   response
            RequestTime requestTime = new RequestTime(new Date());
            requestTime.cipher(sessionKey); // cipher with session key Kc,s

            // TODO efetuar operacao pedida pelo cliente no Binas

            // TODO adicionar coisas contexto

        } catch(KerbyException e){
            e.printStackTrace();
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        } catch(InvalidKeySpecException e){
            e.printStackTrace();
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

            Name ticketName = se.createName(KerberosClientHandler.TICKET_ELEMENT_NAME);

            sh.getChildElements();
            Iterator it = sh.getChildElements(ticketName);
            // check header element
            if (!it.hasNext()) {
                System.out.printf("Header element %s not found.%n", KerberosClientHandler.TICKET_ELEMENT_NAME);
                return true;
            }

            // ----------------- TICKET ----------------------
            SOAPElement ticketSOAPElement = (SOAPElement) it.next();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // get ticket
            Document ticketDocument = builder.parse(ticketSOAPElement.getValue());
            DOMSource ticketDOMSource = new DOMSource(ticketDocument);
            Node ticketNode = ticketDOMSource.getNode();

            cipheredTicketView = clerk.cipherFromXMLNode(ticketNode);

            // -----------------  AUTH  ----------------------
            SOAPElement authSOAPElement = (SOAPElement) it.next();

            Document authDocument = builder.parse(authSOAPElement.getValue());
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