package com.jldroid.twook.model;

import java.util.ArrayList;
import com.jdroid.utils.SortedArrayList;


public interface IAccount {
	
	public User getUser();
	
	public int getColor();
	public void setColor(int color);
	
	public ArrayList<ColumnMessagesProvider> getProviders();
	
	public User loadUserInfo(long id);
	
	public void updateGlobalData(INetworkCallback callback);
	
	public void updateFriends(INetworkCallback callback);
	
	public SortedArrayList<Chat> getChats();
	public Chat findMessageChat(Message msg);
	public boolean sendChatMsg(Chat chat, String text);
	
	public ISearchableColumn createSearchColumn(String query);
	
	public void searchPeopleLocal(SortedArrayList<User> target, String query);
	public boolean searchPeople(SortedArrayList<User> target, String query);
	
	public Message findMessage(long msgPreID, long msgID, int type);
	
	public User findUser(long id);
	
	public ColumnMessagesProvider addUserProvider(User user);
	public void removeUserProvider(ColumnMessagesProvider provider);
}
