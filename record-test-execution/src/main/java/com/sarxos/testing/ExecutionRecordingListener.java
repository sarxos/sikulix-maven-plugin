package com.sarxos.testing;

import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;


public class ExecutionRecordingListener extends RunListener {

	@Override
	public void testStarted(Description description) throws Exception {
		System.out.println("ababa " + description.getDisplayName());
		super.testStarted(description);
	}
}
