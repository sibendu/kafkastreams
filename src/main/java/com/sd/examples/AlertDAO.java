package com.sd.examples; 

import java.sql.*;   

class AlertDAO { 
	 
	private String url = null;
	private String user = null;
	private String password = null;
	
	public static void main(String args[]) throws Exception {
		 
		AlertDAO dao = new AlertDAO("129.213.158.157:3306/kafka", "root", "password");
		
		dao.processAlert("FREQUENT_TXN", null, "TEST", 5, "High txnx in 1 minute", "NEW");
		
		System.out.println("Alert created .. ");
		
		dao.findAlerts();
		System.out.println("Alerts displayed .. ");
		
	}
	
	public AlertDAO(String url, String user, String password) {
		
//		if(System.getenv("DB_URL") != null) {
//			url = System.getenv("DB_URL");
//		}
//		
//		if(System.getenv("DB_USER") != null) {
//			user = System.getenv("DB_USER");
//		}
//		
//		if(System.getenv("DB_PWD") != null) {
//			password = System.getenv("DB_PWD");
//		}		
		
		this.url = url;
		this.user = user;
		this.password = password;
	}
	
	public void processAlert(String type, Date created_date, String account, Integer no_access, String details,
			String status) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		System.out.println(url+"-"+user+"-"+password);
		Connection con = DriverManager.getConnection("jdbc:mysql://"+url,user, password);
		System.out.println(con);
		
		Statement stmt = con.createStatement();

		String query = "insert into account_alerts (type,created_date,account,no_access,details,status) " + " VALUES ('"
				+ type + "', CURDATE() , '" + account + "' , " + no_access + " , '" + details + "' ,'" + status + "') ";
		
		System.out.println(query);
		
		stmt.execute(query);
		
		con.close();
	}

	public void findAlerts() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/kafka", "root", "password");
			Statement stmt = con.createStatement();

			ResultSet rs = stmt.executeQuery("select * from account_alerts");
			while (rs.next())
				System.out.println(rs.getInt(1) + "  " + rs.getString(2) + "  " + rs.getString(3));
			con.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
