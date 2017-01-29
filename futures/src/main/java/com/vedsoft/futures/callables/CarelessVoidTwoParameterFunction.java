package com.vedsoft.futures.callables;

import com.vedsoft.futures.runnables.TwoParameterAction;

/**
 * Created by david on 11/17/16.
 */
class CarelessVoidTwoParameterFunction<ParameterOne, ParameterTwo> implements CarelessTwoParameterFunction<ParameterOne, ParameterTwo, Void> {
	private final TwoParameterAction<ParameterOne, ParameterTwo> action;

	CarelessVoidTwoParameterFunction(TwoParameterAction<ParameterOne, ParameterTwo> action) {
		this.action = action;
	}

	@Override
	public Void resultFrom(ParameterOne paramOne, ParameterTwo paramTwo) {
		action.runWith(paramOne, paramTwo);
		return null;
	}
}