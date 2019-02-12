package ru.maklas.http;

import com.badlogic.gdx.utils.Array;

import java.util.Iterator;

public class HeaderList implements Iterable<Header> {

    protected Array<Header> headers = new Array<>();

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
}
