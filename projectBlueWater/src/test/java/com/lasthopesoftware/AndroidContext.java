package com.lasthopesoftware;

import org.junit.runner.RunWith;

@RunWith(AndroidContextRunner.class)
public abstract class AndroidContext {

	public abstract void before() throws Exception;
}
