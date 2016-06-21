package com.lasthopesoftware.bluewater.servers.library.items.media.files;

import android.widget.ImageButton;

import com.lasthopesoftware.bluewater.shared.LazyViewFinder;

public class BaseMenuViewHolder {
	private final LazyViewFinder<ImageButton> viewFileDetailsButtonFinder;
	private final LazyViewFinder<ImageButton> playButtonFinder;

	public BaseMenuViewHolder(final LazyViewFinder<ImageButton> viewFileDetailsButtonFinder, final LazyViewFinder<ImageButton> playButtonFinder) {
		this.viewFileDetailsButtonFinder = viewFileDetailsButtonFinder;
		this.playButtonFinder = playButtonFinder;
	}

	public ImageButton getViewFileDetailsButton() {
		return viewFileDetailsButtonFinder.findView();
	}

	public ImageButton getPlayButton() {
		return playButtonFinder.findView();
	}
}
