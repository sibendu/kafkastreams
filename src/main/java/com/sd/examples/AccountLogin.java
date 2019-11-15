package com.sd.examples;

import java.sql.Timestamp;

public class AccountLogin {
	private int id;
	private String account;
	private String channel;
	private Timestamp  createdDate;
	

	protected AccountLogin() {
	}

	protected AccountLogin(String account, String channel, Timestamp  createdDate) {
		this.account = account;
		this.channel = channel;
		this.createdDate = createdDate;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public Timestamp  getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Timestamp  createdDate) {
		this.createdDate = createdDate;
	}
	
}
