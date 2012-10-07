package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrStdXmlResponse;
import jrAccess.JrSession;

public class JrCategory extends JrListing {
	private List<JrItem> mCategoryItems;
	private List<JrItem> mSortedCategoryItems;
	
	public JrCategory(int key, String value) {
		super(key, value);
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrCategory() {
		super();
	}
	
	public List<JrItem> getCategoryItems() {
		if (mCategoryItems == null) {
			mCategoryItems = new ArrayList<JrItem>(0);
			
			if (JrSession.accessDao == null) return mCategoryItems;
			
			try {
				mCategoryItems = JrFileUtils.transformListing(JrItem.class, (new GetJrStdXmlResponse()).execute(new String[] { "Browse/Children", "ID=" + String.valueOf(this.mKey) }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategoryItems;
	}
	
	public List<JrItem> getSortedCategoryItems() {
		if (mSortedCategoryItems == null) {
			mSortedCategoryItems = getCategoryItems();
			JrFileUtils.sortSubItems(mSortedCategoryItems);
		}
		
		return mSortedCategoryItems;
	}

}
