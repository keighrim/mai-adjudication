
/*
 * MAI - Multi-document Adjudication Interface
 * 
 * Copyright Amber Stubbs (astubbs@cs.brandeis.edu)
 * Department of Computer Science, Brandeis University
 * 
 * MAI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package mai;

import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * XMLHandler extends the sax DefaultHandler to work specifically with 
 * the stand-off XML format used in MAE/MAI.
 * 
 * @author Amber Stubbs
 * @revised Keigh Rim
 */

class XMLHandler extends DefaultHandler {
    private HashCollection<String, Hashtable<String, String>> newTags
            = new HashCollection<String, Hashtable<String, String>>();
    private boolean mHasText = false;
    private String mText = "";

    XMLHandler() {
    }

    @Override
    public void startElement(String nsURI, String strippedName, String tagName, Attributes atts)
            throws SAXException {

        if (tagName.equalsIgnoreCase("text")) {
            mHasText = true;
        } else {
            Hashtable<String, String> tag = new Hashtable<String, String>();
            for (int i = 0; i < atts.getLength(); i++) {
                String name = atts.getQName(i);
                String value = atts.getValue(i);
                tag.put(name, value);
                // add by krim: for legacy support
                convertLegXml(tag);
                newTags.putEnt(tagName, tag);
            }
        }
    }

    @Override
    public void endElement(String nsURI, String localName, String tagName) {
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (mHasText) {
            mText = new String(ch, start, length);
            mHasText = false;
        }
    }


    HashCollection<String, Hashtable<String, String>> returnTagHash() {
        return newTags;
    }

    public String getText() {
        return mText;
    }

    /**
     * add by krim:
     * Used to convers start-end attributes for old version to new 'spans' attribute.
     *
     * @param tag a HashTable of (attribute, value) entities
     */

    private void convertLegXml(Hashtable<String, String> tag) {

        if (!tag.containsKey("spans")) {
            if (tag.containsKey("start") && tag.containsKey("end")) {
                String start = tag.remove("start");
                String end = tag.remove("end");
                tag.put("spans", start + MaiMain.SPANDELIMITER + end);
            }
        }
    }
}

