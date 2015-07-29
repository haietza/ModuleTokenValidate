package edu.appstate.lts;

import com.wowza.wms.module.*;

public class TokenValidate extends ModuleBase {
	
	private String token;
	private String fileName;
	private String referringApp;
	private boolean validTime;
	private int ipAddress;
	
	public TokenValidate(String token) {
		this.token = token;
	}
	
	

}
