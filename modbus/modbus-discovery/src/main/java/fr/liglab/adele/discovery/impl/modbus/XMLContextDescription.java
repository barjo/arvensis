package fr.liglab.adele.discovery.impl.modbus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLContextDescription {
	private SAXParser parseur;

	public XMLContextDescription() {
		SAXParserFactory fabrique = SAXParserFactory.newInstance();
		try {
			parseur = fabrique.newSAXParser();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Parser error while initializing XML parser", e);
		} catch (SAXException e) {
			throw new RuntimeException("SAX error while initializing XML parser", e);
		}
	}

	
	public Map load(InputStream i) {
		ListAttributs handler = new ListAttributs();
		try {
			parseur.parse(i, handler);
		} catch (SAXException e) {
			throw new RuntimeException("SAX error while parsing " + i, e);
		} catch (IOException e) {
			throw new RuntimeException("IO error while parsing " + i, e);
		}

		return handler.getMap();
	}
 
	private class ListAttributs extends DefaultHandler {

		private boolean inAttributes;
		private StringBuffer buffer;
		private String o;
		private Map map;

		public ListAttributs() {
			super();
			map = new HashMap();
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {

			if (qName.equalsIgnoreCase("binaryRelation"))
				return;

			if (qName.equalsIgnoreCase("attributes")) {
				inAttributes = true;
				return;
			}

			buffer = new StringBuffer();

			if (qName.equalsIgnoreCase("attribute"))
				return;

			if (qName.equalsIgnoreCase("objects"))
				return;

			if (qName.equalsIgnoreCase("object")) {
				o = attributes.getValue("id");
				map.put(o, new HashSet());
				return;
			}

			throw new SAXException("Balise " + qName + " inconnue.");
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			if (qName.equalsIgnoreCase("binaryRelation"))
				return;

			if (qName.equalsIgnoreCase("attributes")) {
				inAttributes = false;
				return;
			}

			if (qName.equalsIgnoreCase("attribute")) {
				if (!inAttributes) {
					Set attributes = (Set) map.get(o);
					attributes.add(buffer.toString());
				}

				buffer = null;

				return;
			}

			if (qName.equalsIgnoreCase("objects") || qName.equalsIgnoreCase("object"))
				return;

			throw new SAXException("Balise " + qName + " inconnue.");
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			String lecture = new String(ch, start, length);
			if (buffer != null)
				buffer.append(lecture);
		}

		public Map getMap() {
			return map;
		}
	}

}
