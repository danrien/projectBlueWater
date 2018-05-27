package com.lasthopesoftware.resources.notifications.specs;

import android.app.Notification;
import android.support.v4.app.NotificationCompat;

public class FakeNotificationCompatBuilder extends NotificationCompat.Builder {
	private final Notification returnNotification;

	public static NotificationCompat.Builder newFakeBuilder(Notification returnNotification) {
		return new FakeNotificationCompatBuilder(returnNotification);
	}

	@SuppressWarnings("ConstantConditions")
	public FakeNotificationCompatBuilder(Notification returnNotification) {
		super(null, null);

		this.returnNotification = returnNotification;
	}

	@Override
	public Notification build() {
		return returnNotification;
	}
}