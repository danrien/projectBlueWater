package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrFsResponse;
import jrAccess.JrSession;

public class JrItem extends JrListing {
	public List<JrListing> mSubItems;
	
	public JrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrItem() {
		super();		
	}
	
	public List<JrListing> getSubItems() {
		
		if (mSubItems == null) {
			mSubItems = new ArrayList<JrListing>();
			if (JrSession.accessDao == null) return mSubItems;
			try {
				List<JrItem> tempSubItems = (List<JrItem>) (new JrFsResponse(JrItem.class)).execute(new String[] { "Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1" }).get();
				mSubItems.addAll(tempSubItems);
				//mSubItems = (List<JrListing>) (new JrFileUtils.SortJrListAsync().execute(new List[] { mSubItems }).get());
				
				if (mSubItems.isEmpty()) {
					List<JrFile> tempFiles = (new JrFileXmlResponse()).execute(new String[] { "Browse/Files", "ID=" + String.valueOf(this.mKey)/*, "Action=Serialize"*/}).get(); 
					mSubItems.addAll(tempFiles);
				}
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
}
