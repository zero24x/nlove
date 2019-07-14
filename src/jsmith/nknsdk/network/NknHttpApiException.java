package jsmith.nknsdk.network;

public class NknHttpApiException extends Exception {

	private int errorCode;
	private String errorMsg;

	public NknHttpApiException(int errorCode, String data) {
		this.errorCode = errorCode;
		this.errorMsg = data;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	
	
}
