package com.lasthopesoftware.promises.cancellable.specs.GivenAPromiseThatIsCancelled;

import com.lasthopesoftware.promises.CancellablePromise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by david on 10/17/16.
 */

public class WhenTheCancellationIsCalled {

	private Exception thrownException;
	private Exception caughtException;

	@Before
	public void before() {
		thrownException = new Exception();
		final CancellablePromise<String> cancellablePromise = new CancellablePromise<>(
			(resolve, reject, onCancelled) -> onCancelled.runWith(() -> reject.withError(thrownException)));

		cancellablePromise.error((exception, onCancelled) -> { caughtException = exception;	});

		cancellablePromise.cancel();
	}

	@Test
	public void thenTheRejectionIsCorrect() {
		Assert.assertEquals(thrownException, caughtException);
	}
}
