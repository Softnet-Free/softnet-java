package softnet.client;

import softnet.asn.*;

public class RequestParams {
	public final Object attachment;
	public final int waitSeconds; 
	public final SequenceEncoder sessionTag;
		
	public RequestParams(Object attachment,	int waitSeconds) {
		if(waitSeconds < 0)
			throw new IllegalArgumentException("'waitSeconds' must not be negative.");
		this.waitSeconds = waitSeconds >= 5 ? waitSeconds : 5;
		this.attachment = attachment;
		this.sessionTag = null; 		
	}
	
	public RequestParams(Object attachment) {
		this.attachment = attachment;
		this.waitSeconds = 0;
		this.sessionTag = null; 		
	}

	public RequestParams(int waitSeconds) {
		if(waitSeconds < 0)
			throw new IllegalArgumentException("'waitSeconds' must not be negative.");
		this.waitSeconds = waitSeconds >= 5 ? waitSeconds : 5;
		this.attachment = null;
		this.sessionTag = null; 		
	}

	protected RequestParams(boolean withSessionTag) {
		this.waitSeconds = 0;
		this.attachment = null;
		
		if(withSessionTag) {
			asnEncoder = new ASNEncoder(); 
			sessionTag = asnEncoder.Sequence();
		} else
			sessionTag = null;
	}

	protected RequestParams(Object attachment, int waitSeconds, boolean withSessionTag) {
		if(waitSeconds < 0)
			throw new IllegalArgumentException("'waitSeconds' must not be negative.");
		this.waitSeconds = waitSeconds >= 5 ? waitSeconds : 5;
		this.attachment = attachment;
		
		if(withSessionTag) {
			asnEncoder = new ASNEncoder(); 
			sessionTag = asnEncoder.Sequence();
		} else
			sessionTag = null;
	}

	private ASNEncoder asnEncoder;

	public byte[] getSessionTagEncoding() {
		if(sessionTag == null)
			return null;		
		byte[] encoding = asnEncoder.getEncoding();
		if(encoding.length > 64)
			throw new IllegalArgumentException("The maximum data size in a session tag is limited to 64 bytes.");
		return encoding;
	}
}
