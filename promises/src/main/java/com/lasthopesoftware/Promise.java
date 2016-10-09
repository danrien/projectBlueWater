package com.lasthopesoftware;

import com.vedsoft.futures.runnables.OneParameterRunnable;
import com.vedsoft.futures.runnables.TwoParameterRunnable;

public final class Promise<TResult> extends UnresolvedPromise<Void, TResult> {

	public Promise(TwoParameterRunnable<OneParameterRunnable<TResult>, OneParameterRunnable<Exception>> executor) {
		super(new InternalPromiseExecutor<>(executor));

		execute(null);
	}

}
