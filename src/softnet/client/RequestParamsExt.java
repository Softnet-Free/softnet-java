package softnet.client;

public class RequestParamsExt extends RequestParams {

	public RequestParamsExt(Object attachment, int waitSeconds) {
		super(attachment, waitSeconds, true);
	}
	
	public RequestParamsExt(Object attachment) {
		super(attachment, 0, true);
	}

	public RequestParamsExt(int waitSeconds) {
		super(null, waitSeconds, true);
	}
	
	public RequestParamsExt() {
		super(true);
	}
}
