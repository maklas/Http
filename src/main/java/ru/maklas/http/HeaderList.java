package ru.maklas.http;

import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

/** Header list. Used for storage and access.**/
public class HeaderList implements Iterable<Header> {

    Array<Header> headers = new Array<>();

    @Override
    public Iterator<Header> iterator() {
        return headers.iterator();
    }

    public void addAll(HeaderList headers) {
        this.headers.addAll(headers.headers);
    }

    public HeaderList add(Header header) {
        headers.add(header);
        return this;
    }

    /** Adds header. If there already was a header with the same Key, it gets replaced **/
    public HeaderList addUnique(Header header) {
        if (header == null || header.key == null) return this;
        String key = header.key;
        int targetIndex = -1;
        for (int i = 0; i < headers.size; i++) {
            if (key.equalsIgnoreCase(headers.get(i).key)){
                targetIndex = i;
                break;
            }
        }
        if (targetIndex == -1){
            headers.add(header);
        } else {
            headers.set(targetIndex, header);
        }
        return this;
    }

    public void remove(String key) {
        Iterator<Header> iter = headers.iterator();
        while (iter.hasNext()){
            if (key.equalsIgnoreCase(iter.next().key)){
                iter.remove();
            }
        }
    }

    /** Doesn't care about upper/lower case **/
    public Header getHeader(String key){
        return getHeader(key, false);
    }

    public Header getHeader(String key, boolean caseSensitive){
        for (Header header : headers) {
            if (equals(key, header.key, caseSensitive)){
                return header;
            }
        }
        return null;
    }

    public Array<Header> getHeaders(String key, boolean caseSensitive){
        Array<Header> arr = new Array<>();

        for (Header header : headers) {
            if (equals(key, header.key, caseSensitive)){
                arr.add(header);
            }
        }

        return arr;
    }

    protected boolean equals(String s1, String s2, boolean caseSensitive){
        return caseSensitive ? s1.equals(s2) : s1.equalsIgnoreCase(s2);
    }

    public int size(){
        return headers.size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Header header : headers) {
            builder.append(header.key)
                    .append(": ")
                    .append(header.value).append('\n');
        }
        return builder.toString();
    }

    public HeaderList replaceIfNotPresent(Header header) {
        if (header == null || header.key == null) return this;
        String key = header.key;
        for (int i = 0; i < headers.size; i++) {
            if (key.equals(headers.get(i).key)){
                return this;
            }
        }
        headers.add(header);
        return this;
    }
}


