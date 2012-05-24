package com.jldroid.twook.model.facebook;

import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

import com.jdroid.utils.Threads;

public class FacebookChatManager {

	private FacebookAccount mAccount;
	
	
	public FacebookChatManager(FacebookAccount account) {
		mAccount = account;
	}
	
	
	public void connect() {
		Threads.runOnNetworkThread(new Runnable() {
			@Override
			public void run() {
				ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 5222);
				SASLAuthentication.registerSASLMechanism("X-FACEBOOK-PLATFORM", SASLXFacebookPlatformMechanism.class);
			    SASLAuthentication.supportSASLMechanism("X-FACEBOOK-PLATFORM", 0);
				XMPPConnection xmpp = new XMPPConnection(config);
				try
				{
				    xmpp.connect();
				    xmpp.login(FacebookConfiguration.APP_ID, mAccount.getOAuthToken(), "Social Center");
				    xmpp.getChatManager().addChatListener(new ChatManagerListener() {
						@Override
						public void chatCreated(Chat chat, boolean b) {
							Log.i("JDROID", "CHAT CREATED WITH: " + chat.getParticipant() + " THREADID: " + chat.getThreadID());
						}
					});
				    xmpp.getRoster().addRosterListener(new RosterListener() {
						@Override
						public void presenceChanged(Presence pArg0) {
							Log.i("JDROID", "PRESENCE CHANGED FROM: " + pArg0.getFrom() + " AVAILABLE: " + pArg0.isAvailable());
						}
						
						@Override
						public void entriesUpdated(Collection<String> pArg0) {
							Log.i("JDROID", "ENTRIES UPDATED");
						}
						
						@Override
						public void entriesDeleted(Collection<String> pArg0) {
							Log.i("JDROID", "ENTRIES DELETED");
						}
						
						@Override
						public void entriesAdded(Collection<String> pArg0) {
							Log.i("JDROID", "ENTRIES ADDED");
						}
					});
				} catch (XMPPException e)
				{
				    xmpp.disconnect();
				    e.printStackTrace();
				}
			}
		});
	}
	
	
}
