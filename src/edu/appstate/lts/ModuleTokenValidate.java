package edu.appstate.lts;

import com.wowza.wms.application.*;
import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.httpstreamer.model.*;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.*;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.*;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.util.URLUtils;

public class ModuleTokenValidate extends ModuleBase implements IMediaStreamNameAliasProvider2 {
	
	private TokenValidate tokenValidate;
	
	public ModuleTokenValidate() {
		super();
	}

	public void onAppStart(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		getLogger().info("onAppStart: " + fullname);
	}

	public void onAppStop(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		getLogger().info("onAppStop: " + fullname);
	}

	public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
		getLogger().info("onConnect: " + client.getClientId());
		
		tokenValidate = new TokenValidate(client);		
		if (!tokenValidate.validate()) {
			getLogger().info("Client ID " + client.getClientId() + " not valid.");
			client.rejectConnection();
			client.shutdownClient();
		}
		else {
			// get video from Mensch store via token/hash and send to player
			// RTMP
			IApplicationInstance appInstance = client.getAppInstance();
			appInstance.setStreamNameAliasProvider(this);
		}
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client) {
		tokenValidate = new TokenValidate(client);
		getLogger().info("Resolve given name: " + name);
		getLogger().info("Resolve returned name: " + tokenValidate.getFileName());
		return tokenValidate.getFileName();
		//return "sample2.mp4";
		//return name;
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession){
		return null;
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, RTPSession rtpSession) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name,
			ILiveStreamPacketizer liveStreamPacketizer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name, IMediaCaster mediaCaster) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onConnectAccept(IClient client) {
		getLogger().info("onConnectAccept: " + client.getClientId());
	}

	public void onConnectReject(IClient client) {
		getLogger().info("onConnectReject: " + client.getClientId());
	}

	public void onDisconnect(IClient client) {
		getLogger().info("onDisconnect: " + client.getClientId());
	}

	public void onStreamCreate(IMediaStream stream) {
		getLogger().info("onStreamCreate: " + stream.getSrc());
		
		//tokenValidate = new TokenValidate(stream.getClient());		
		//if (!tokenValidate.validate()) {
			//stream.getClient().setShutdownClient(true);
			//stream.getClient().shutdownClient();
		//}
	}

	public void onStreamDestroy(IMediaStream stream) {
		getLogger().info("onStreamDestroy: " + stream.getSrc());
	}

	public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
		getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
		
		tokenValidate = new TokenValidate(httpSession);
		if (!tokenValidate.validate()) {
			httpSession.rejectSession();
			httpSession.shutdown();
		}
	}

	public void onHTTPSessionDestroy(IHTTPStreamerSession httpSession) {
		getLogger().info("onHTTPSessionDestroy: " + httpSession.getSessionId());
	}
	
	public void onCall(String handlerName, IClient client, RequestFunction function, AMFDataList params) {
		getLogger().info("onCall: " + handlerName);
	}
}
