package ru.maklas.http;

public class MutableHeaderList extends HeaderList {

    public void clear(){
        headers.clear();
    }

    public void remove(String key){
        headers.filter(h -> !h.key.equals(key));
    }

}
