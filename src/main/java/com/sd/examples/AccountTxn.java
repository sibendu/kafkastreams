package com.sd.examples;

import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountTxn {
	private int id;
	private String account;
	private String type;
	//private Timestamp created_date;
	

	protected AccountTxn() {
	}

	protected AccountTxn(String account, String type) {
		this.account = account;
		this.type = type;
		//this.created_date = created_date;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

//	public Timestamp  getCreated_Date() {
//		return created_date;
//	}
//
//	public void setCreated_Date(Timestamp  created_date) {
//		this.created_date = created_date;
//	}
	
}
