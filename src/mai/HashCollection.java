
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

/**
 * This is an implementation of a Hashtable that
 * stores more than one value per key.  This is done by
 * having every key associated with an ArrayList, and every
 * new value being stored in the array is added to the end of
 * @revised Keigh Rim
 *
 */

import java.util.*;

class HashCollection<K,V>{

    private Hashtable<K,ArrayList<V>> mHash;

    public HashCollection(){
        mHash = new Hashtable<K,ArrayList<V>>();
    }

    public HashCollection(Hashtable<K,ArrayList<V>> hash){
        mHash = hash;
    }

    /**
     * Associate yet another value with a key in a Hashtable that allows duplicates.
     * Also use to put the first key/value.
     * Add an entity to a key's arrayList
     *
     * @param key Hashtable key
     * @param value value being added to key's array
     */
    public void putEnt (K key, V value) {
        //just add to tail end of existing ArrayList
        //but only if it's not already there
        try {
            getList(key).add(value);
        } catch (NullPointerException e) {
            ArrayList<V> newlist = new ArrayList<V>();
            newlist.add(value);
            mHash.put(key, newlist);
        }
    }

    /* krim: redundant
    void putAllEnt (K key, V value) {
        ArrayList<V> existing = getList(key);
        if ( existing == null ){
            ArrayList<V> newlist = new ArrayList<V>();
            newlist.add(value);
            mHash.put(key, newlist);
        } else {
            //just add to tail end of existing ArrayList
            //even if the value is already there
            existing.add(value);
        }
    }
    */

    public Hashtable<V,String> getValueHash(){
        Hashtable<V,String> values = new Hashtable<V,String>();
        for (ArrayList<V> list : mHash.values()) {
            if (list != null) {
                for (V value : list) {
                    if (value != null) {
                        values.put(value, "");
                    }
                }
            }
        }
        return(values);
    }

    public void printKeys(){
        for (Enumeration<K> e = mHash.keys() ; e.hasMoreElements() ;) {
            System.out.println(e.nextElement());
        }
    }

    public ArrayList<K> getKeyList(){
        ArrayList<K> keys = new ArrayList<K>();
        for (Enumeration<K> e = mHash.keys() ; e.hasMoreElements() ;) {
            keys.add(e.nextElement());
        }
        return(keys);
    }

    public void printHash(){
        for (Enumeration<K> e = mHash.keys() ; e.hasMoreElements() ;) {
            K ent = e.nextElement();
            System.out.println(ent + ":");
            ArrayList<V> list = getList(ent);
            for (int i=0;i<list.size();i++){
                System.out.println("\t" + list.get(i).toString());
            }
        }

    }

    /**
     * Add all key-value pairs of a new HashCollection to this object
     * @param newHash - target HashCollection
     */

    public void putAll(HashCollection<K,V> newHash){
        Enumeration<K> e = newHash.keys();
        while (e.hasMoreElements()) {
            K key = e.nextElement();
            ArrayList<V> values = newHash.getList(key);
            for (V value : values) {
                putEnt(key, value);
            }
        }
    }

    public void putList(K key, ArrayList<V> list){
        for (V aList : list) {
            putEnt(key, aList);
        }
    }

    public ArrayList<V> getList(K key){
        try {
            return mHash.get(key);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public int size(){
        return(mHash.size());
    }

    public ArrayList<V> remove(K key){
        return mHash.remove(key);
    }

    public void clear() {
        for (Enumeration<K> e = mHash.keys() ; e.hasMoreElements() ;) {
            K ent = e.nextElement();
            remove(ent);
        }
    }

    public boolean containsKey(K key){
        return(mHash.containsKey(key));
    }

    public ArrayList<V> get(K key){
        return(mHash.get(key));
    }

    public Enumeration<K> keys(){
        return(mHash.keys());
    }
}
