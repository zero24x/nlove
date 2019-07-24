package com.nlove.provider;

import java.time.Duration;
import java.time.Instant;

public class HoldedObject<T> {

	private Instant added;
	private T holdedObject;

	public HoldedObject(T holdedObject) {
		this.holdedObject = holdedObject;
		this.added = Instant.now();
	}

	public Duration getAge() {
		return Duration.between(added, Instant.now());
	}

	public T getHoldedObject() {
		return holdedObject;
	}

	public void setHoldedObject(T holdedObject) {
		this.holdedObject = holdedObject;
	}

}
