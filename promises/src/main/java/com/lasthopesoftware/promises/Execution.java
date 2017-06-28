package com.lasthopesoftware.promises;

import com.vedsoft.futures.callables.CarelessFunction;
import com.vedsoft.futures.callables.CarelessOneParameterFunction;
import com.vedsoft.futures.callables.CarelessTwoParameterFunction;
import com.vedsoft.futures.runnables.OneParameterAction;

final class Execution {

	static final class InternalExpectedPromiseExecutor<Result> extends EmptyMessenger<Result> {
		private final CarelessFunction<Result> executor;

		InternalExpectedPromiseExecutor(CarelessFunction<Result> executor) {
			this.executor = executor;
		}

		@Override
		public void requestResolution() {
			try {
				sendResolution(executor.result());
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class MessengerTunnel<Result> extends EmptyMessenger<Result> {

		private final OneParameterAction<Messenger<Result>> messengerDestination;

		MessengerTunnel(OneParameterAction<Messenger<Result>> messengerDestination) {
			this.messengerDestination = messengerDestination;
		}

		@Override
		public void requestResolution() {
			messengerDestination.runWith(this);
		}
	}

	static final class PassThroughCallable<PassThroughResult> extends EmptyMessenger<PassThroughResult> {
		private final PassThroughResult passThroughResult;

		PassThroughCallable(PassThroughResult passThroughResult) {
			this.passThroughResult = passThroughResult;
		}

		@Override
		public void requestResolution() {
			sendResolution(passThroughResult);
		}
	}

	static final class Cancellable {

		static final class RejectionDependentCancellableCaller<Result, NewResult> extends ErrorRespondingPromise<Result, NewResult> {
			private final CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, NewResult> onFulfilled;

			RejectionDependentCancellableCaller(CarelessTwoParameterFunction<Throwable, OneParameterAction<Runnable>, NewResult> onFulfilled) {
				this.onFulfilled = onFulfilled;
			}

			@Override
			protected void requestResolution(Throwable throwable) {
				try {
					sendResolution(onFulfilled.resultFrom(throwable, this));
				} catch (Throwable rejection) {
					sendRejection(rejection);
				}
			}
		}

	}

	/**
	 * Created by david on 10/8/16.
	 */
	static final class ExpectedResultPromise<Resolution, Response> extends ResolutionRespondingPromise<Resolution, Response> {
		private final CarelessOneParameterFunction<Resolution, Response> onFulfilled;

		ExpectedResultPromise(CarelessOneParameterFunction<Resolution, Response> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		void requestResponse(Resolution resolution) {
			try {
				sendResolution(onFulfilled.resultFrom(resolution));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}

	static final class ErrorResultExecutor<TResult, TNewResult> extends ErrorRespondingPromise<TResult, TNewResult> {
		private final CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled;

		ErrorResultExecutor(CarelessOneParameterFunction<Throwable, TNewResult> onFulfilled) {
			this.onFulfilled = onFulfilled;
		}

		@Override
		protected void requestResolution(Throwable throwable) {
			try {
				sendResolution(onFulfilled.resultFrom(throwable));
			} catch (Throwable rejection) {
				sendRejection(rejection);
			}
		}
	}
}
