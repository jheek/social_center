package com.jldroid.twook.model.facebook;

/*
 * Adapted by Shai from code posted on Ignite forums:
 * http://community.igniterealtime.org/message/205739#205739
 *
 */
import java.io.IOException;
import java.net.URLEncoder;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.sasl.Sasl;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.Base64;

public class SASLXFacebookPlatformMechanism extends SASLMechanism {
	
    public static final String NAME = "X-FACEBOOK-PLATFORM";
	
	
    private String apiKey = "";
    private String accessToken = "";
	
	
    /**
     * Constructor.
     */
    public SASLXFacebookPlatformMechanism(SASLAuthentication saslAuthentication) {
	super(saslAuthentication);
    }
	
    @Override
    protected void authenticate() throws IOException, XMPPException {
		final StringBuilder stanza = new StringBuilder();
		stanza.append("<auth mechanism=\"").append(getName());
		stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
		stanza.append("</auth>");
			
		// Send the authentication to the server
		getSASLAuthentication().send(new Packet() {
			@Override
			public String toXML() {
				return stanza.toString();
			}
		});
    }
	
    @Override
    public void authenticate(String apiKey, String host, String accessToken) 
	throws IOException, XMPPException
    {
	if (apiKey == null || accessToken == null)
	    throw new IllegalArgumentException("Invalid parameters");

	this.apiKey = apiKey;
	this.accessToken = accessToken;
		
	//this.authenticationId = sessionKey;
	this.password = accessToken; // Needs to be non-null,non-empty for Smack mechanisms, I think...
	this.hostname = host;

	String[] mechanisms = { "X-FACEBOOK-PLATFORM", "DIGEST-MD5" };
	Map<String, String> props = new HashMap<String, String>();
	this.sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, this);
	authenticate();
    }
	
    @Override
    public void authenticate(String username, String host, CallbackHandler cbh) throws IOException, XMPPException {
		String[] mechanisms = { "DIGEST-MD5" };
		Map<String, String> props = new HashMap<String, String>();
		this.sc = Sasl.createSaslClient(mechanisms, null, "xmpp", host, props, cbh);
		authenticate();
    }
	
    @Override
    protected String getName() {
	return NAME;
    }
	
    @Override
    public void challengeReceived(String challenge) throws IOException {
	final StringBuilder stanza = new StringBuilder();
	byte[] response = null;
		
	if (challenge != null) {
	    String decodedChallenge = new String(Base64.decode(challenge));
	    Map<String, String> parameters = getQueryMap(decodedChallenge);
			
	    String version = "1.0";
	    String nonce = parameters.get("nonce");
	    String method = parameters.get("method");
			
	    long callId = new GregorianCalendar().getTimeInMillis() / 1000L;

	    String composedResponse = "api_key=" + URLEncoder.encode(apiKey, "utf-8")
		+ "&access_token=" + URLEncoder.encode(accessToken, "utf-8")
		+ "&call_id=" + callId
		+ "&method=" + URLEncoder.encode(method, "utf-8")
		+ "&nonce=" + URLEncoder.encode(nonce, "utf-8")
		+ "&v=" + URLEncoder.encode(version, "utf-8")
		;
			
	    response = composedResponse.getBytes("utf-8");
	}
		
	String authenticationText = "";
		
	if (response != null)
	    authenticationText = Base64.encodeBytes(response, Base64.DONT_BREAK_LINES);
		
	stanza.append("<response xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
	stanza.append(authenticationText);
	stanza.append("</response>");
		
	// Send the authentication to the server
	getSASLAuthentication().send(new Packet() {
		@Override
		public String toXML() {
			return stanza.toString();
		}
	});
    }
	
    private Map<String, String> getQueryMap(String query) {
	Map<String, String> map = new HashMap<String, String>();
	String[] params = query.split("\\&");
		
	for (String param : params) {
	    String[] fields = param.split("=", 2);
	    map.put(fields[0], (fields.length > 1 ? fields[1] : null));
	}
		
	return map;
    }
}