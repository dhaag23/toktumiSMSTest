/* © Copyright 2010 to the present, Toktumi, Inc.  */

package com.toktumi.toktumiSMSTest;

import java.io.Serializable;

public class Recipient implements Serializable {
	private static final long serialVersionUID = 1L;

	public String recipientNumber;
	public int trys;
	public int sendMessageIndex;
	
	public Recipient(String recipientNumber) {
		this.recipientNumber = recipientNumber;
		this.trys = 0;
		this.sendMessageIndex = 0;
	}

	@Override
	public String toString() {
		return recipientNumber;
	}
}
