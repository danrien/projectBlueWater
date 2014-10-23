package com.lasthopesoftware.bluewater.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.adapters.FileDetailsAdapter;
import com.lasthopesoftware.bluewater.data.service.access.FileProperties;
import com.lasthopesoftware.bluewater.data.service.access.ImageAccess;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.ISimpleTask.OnCompleteListener;
import com.lasthopesoftware.threading.ISimpleTask.OnExecuteListener;
import com.lasthopesoftware.threading.SimpleTask;

public class ViewFileDetails extends Activity {

	public static final String FILE_KEY = "com.lasthopesoftware.bluewater.activities.ViewFiles.FILE_KEY";
	
	private Bitmap mFileImage;
	
	private static final Set<String> PROPERTIES_TO_SKIP = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
															new String[] {
																FileProperties.AUDIO_ANALYSIS_INFO,
																FileProperties.GET_COVER_ART_INFO,
																FileProperties.IMAGE_FILE,
																FileProperties.KEY,
																FileProperties.STACK_FILES,
																FileProperties.STACK_TOP,
																FileProperties.STACK_VIEW })));
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_file_details);
        
        final int fileKey = this.getIntent().getIntExtra(FILE_KEY, -1);
        
        if (fileKey < 0) {
        	startActivity(new Intent(this, BrowseLibrary.class));
        	finish();
        	return;
        };
        
        final ViewFileDetails _this = this;
        final ListView lvFileDetails = (ListView) findViewById(R.id.lvFileDetails);
        final ProgressBar pbLoadingFileDetails = (ProgressBar) findViewById(R.id.pbLoadingFileDetails);
        final ImageView imgFileThumbnail = (ImageView) findViewById(R.id.imgFileThumbnail);
        final ProgressBar pbLoadingFileThumbnail = (ProgressBar) findViewById(R.id.pbLoadingFileThumbnail);
        final RatingBar rbFileRating = (RatingBar) findViewById(R.id.rbFileRating);
        final TextView tvFileName = (TextView) findViewById(R.id.tvFileName);
        
        lvFileDetails.setVisibility(View.INVISIBLE);
        pbLoadingFileDetails.setVisibility(View.VISIBLE);
        
        imgFileThumbnail.setVisibility(View.INVISIBLE);
        pbLoadingFileThumbnail.setVisibility(View.VISIBLE);
        
        final FileProperties filePropertiesHelper = new FileProperties(fileKey);
        
        tvFileName.setText(getText(R.string.lbl_loading));
        final SimpleTask<Void, Void, String> getFileNameTask = new SimpleTask<Void, Void, String>();
        getFileNameTask.setOnExecuteListener(new OnExecuteListener<Void, Void, String>() {
			
			@Override
			public String onExecute(ISimpleTask<Void, Void, String> owner, Void... params) throws Exception {
				return filePropertiesHelper.getProperty("Name");
			}
		});
        getFileNameTask.addOnCompleteListener(new OnCompleteListener<Void, Void, String>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, String> owner, String result) {
				tvFileName.setText(result);
			}
		});
        getFileNameTask.execute();
        
        final SimpleTask<Void, Void, Float> getRatingsTask = new SimpleTask<Void, Void, Float>();
		getRatingsTask.setOnExecuteListener(new OnExecuteListener<Void, Void, Float>() {
			
			@Override
			public Float onExecute(ISimpleTask<Void, Void, Float> owner, Void... params) throws Exception {
				if (filePropertiesHelper.getProperty("Rating") != null && !filePropertiesHelper.getProperty("Rating").isEmpty())
					return Float.valueOf(filePropertiesHelper.getProperty("Rating"));
				
				return (float) 0;
			}
		});
		getRatingsTask.addOnCompleteListener(new OnCompleteListener<Void, Void, Float>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, Float> owner, Float result) {
//				if (owner.getState() == SimpleTaskState.ERROR && containsIoException(owner.getExceptions())) {
//					resetViewOnReconnect(_file);
//					return;
//				}
				
				rbFileRating.setRating(result);
				rbFileRating.invalidate();
				
				rbFileRating.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
					
					@Override
					public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
						if (!fromUser) return;
						filePropertiesHelper.setProperty("Rating", String.valueOf(Math.round(rating)));
					}
				});
			}
		});
		getRatingsTask.execute();
        
        final SimpleTask<Void, Void, List<Entry<String, String>>> getFilePropertiesTask = new SimpleTask<Void, Void, List<Entry<String, String>>>();
        getFilePropertiesTask.setOnExecuteListener(new OnExecuteListener<Void, Void, List<Entry<String, String>>>() {
			
			@Override
			public List<Entry<String, String>> onExecute(ISimpleTask<Void, Void, List<Entry<String, String>>> owner, Void... params) throws Exception {
				final Map<String, String> fileProperties = filePropertiesHelper.getRefreshedFormattedProperties();
				final ArrayList<Entry<String, String>> results = new ArrayList<Map.Entry<String,String>>(fileProperties.size());
				
				for (Entry<String, String> entry : fileProperties.entrySet()) {
					if (PROPERTIES_TO_SKIP.contains(entry.getKey())) continue;
					results.add(entry);
				}
				
				return results;
			}
		});
        
        getFilePropertiesTask.addOnCompleteListener(new OnCompleteListener<Void, Void, List<Entry<String, String>>>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, List<Entry<String, String>>> owner, List<Entry<String, String>> result) {
				lvFileDetails.setAdapter(new FileDetailsAdapter(_this, R.id.linFileDetailsRow, result));
				pbLoadingFileDetails.setVisibility(View.INVISIBLE);
				lvFileDetails.setVisibility(View.VISIBLE);
			}
		});
        
        getFilePropertiesTask.execute();
                
        ImageAccess.getImage(this, fileKey, new OnCompleteListener<Void, Void, Bitmap>() {
			
			@Override
			public void onComplete(ISimpleTask<Void, Void, Bitmap> owner, Bitmap result) {
				mFileImage = result;
				
				imgFileThumbnail.setImageBitmap(mFileImage);
				imgFileThumbnail.setScaleType(ScaleType.CENTER_INSIDE);
				
				pbLoadingFileThumbnail.setVisibility(View.INVISIBLE);
				imgFileThumbnail.setVisibility(View.VISIBLE);
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		InstantiateSessionConnection.restoreSessionConnection(this);
	}
	
	@Override
	public void onDestroy() {
		if (mFileImage != null) mFileImage.recycle();
		
		super.onDestroy();
	}
}
