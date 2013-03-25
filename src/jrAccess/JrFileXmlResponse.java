package jrAccess;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrFileSystem.JrFile;

import org.xml.sax.SAXException;

import xmlwise.XmlElement;

import android.R.xml;
import android.os.AsyncTask;

public class JrFileXmlResponse extends AsyncTask<String, Void, List<JrFile>> {

	@Override
	protected List<JrFile> doInBackground(String... params) {
		List<JrFile> returnFiles = new ArrayList<JrFile>();
		
		JrConnection conn;
		try {
			conn = new JrConnection(params);
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser sp = parserFactory.newSAXParser();
	    	JrFileXmlHandler jrFileXml = new JrFileXmlHandler();
	    	sp.parse(conn.getInputStream(), jrFileXml);
	    	
	    	returnFiles = jrFileXml.getFiles();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return returnFiles;
	}
	
	protected void SimpleParse(InputStream is) {
		return;
	}

}
