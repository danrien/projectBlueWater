package com.lasthopesoftware.bluewater.servers.library.items.access;

import com.lasthopesoftware.bluewater.servers.connection.ConnectionProvider;
import com.lasthopesoftware.bluewater.servers.library.items.Item;

import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ItemResponse {

	public static List<Item> GetItems(ConnectionProvider connectionProvider, InputStream is) {

		try {
			
			final SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    	final ItemResponseHandler jrResponseHandler = new ItemResponseHandler();
	    	sp.parse(is, jrResponseHandler);
	    	
	    	return jrResponseHandler.items;
		} catch (IOException | ParserConfigurationException | SAXException e) {
			LoggerFactory.getLogger(ItemResponse.class).error(e.toString(), e);
		}
		
		return new ArrayList<>();
	}

}
