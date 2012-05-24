package com.jldroid.twook.model.twitter;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

public class SimpleUserStreamListener implements UserStreamListener {

	@Override
	public void onDeletionNotice(StatusDeletionNotice pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onScrubGeo(long pArg0, long pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatus(Status pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTrackLimitationNotice(int pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onException(Exception pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBlock(User pArg0, User pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDeletionNotice(long pArg0, long pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDirectMessage(DirectMessage pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFavorite(User pArg0, User pArg1, Status pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFollow(User pArg0, User pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFriendList(long[] pArg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRetweet(User pArg0, User pArg1, Status pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnblock(User pArg0, User pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnfavorite(User pArg0, User pArg1, Status pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListCreation(User pArg0, UserList pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListDeletion(User pArg0, UserList pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListMemberAddition(User pArg0, User pArg1, UserList pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListMemberDeletion(User pArg0, User pArg1, UserList pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListSubscription(User pArg0, User pArg1, UserList pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListUnsubscription(User pArg0, User pArg1, UserList pArg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserListUpdate(User pArg0, UserList pArg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserProfileUpdate(User pArg0) {
		// TODO Auto-generated method stub

	}

}
