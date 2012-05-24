package com.jldroid.twook.model;

public class ChatMessage implements Comparable<ChatMessage> {

	public long ID;
	public User sender;
	public boolean isMe;
	
	public String text;
	
	public long time;
	
	public ChatMessage() {
		
	}
	
	public ChatMessage(IAccount account, Message msg) {
		this(msg.ID, msg.sender, account.getUser().id == msg.sender.id, msg.text, msg.updatedTime);
	}

	public ChatMessage(long ID, User sender, boolean isMe, String text, long time) {
		this.ID = ID;
		this.sender = sender;
		this.isMe = isMe;
		this.text = text;
		this.time = time;
	}
	
	@Override
	public int compareTo(ChatMessage pAnother) {
		long l1 = time;
		long l2 = pAnother.time;
		return l1 > l2 ? 1 : (l1 < l2 ? -1 : 0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (ID ^ (ID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ChatMessage))
			return false;
		ChatMessage other = (ChatMessage) obj;
		if (ID == -1 && other.ID == -1) {
			return false;
		}
		if (ID != other.ID)
			return false;
		return true;
	}
}
