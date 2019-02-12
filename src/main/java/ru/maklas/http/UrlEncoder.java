package ru.maklas.http;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.MapFunction;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class UrlEncoder {

    public static final MapFunction<String, String> jsUri = UrlEncoder::encodeURIComponent;
    public static final MapFunction<String, String> jsUriAndPlus = UrlEncoder::encodeURIComponentPlus;
    public static final MapFunction<String, String> javaUrl = UrlEncoder::encodeJavaUrl;
    public static       MapFunction<String, String> defaultEncoding = javaUrl;

    private Array<KeyValuePair> pairs = new Array<>();
    private MapFunction<String, String> encodingFunction = defaultEncoding;

    public UrlEncoder add(String key, String value){
        pairs.add(new KeyValuePair(key, value));
        return this;
    }

    public UrlEncoder add(String key, long value){
        return add(key, Long.toString(value));
    }

    public UrlEncoder add(String key, boolean value){
        return add(key, Boolean.toString(value));
    }

    public UrlEncoder add(String key, Object value){
        return add(key, String.valueOf(value));
    }

    public Array<KeyValuePair> getPairs() {
        return pairs;
    }

    public UrlEncoder usingJavaEncoding(){
        this.encodingFunction = javaUrl;
        return this;
    }

    public UrlEncoder usingJsEncoding(){
        this.encodingFunction = jsUri;
        return this;
    }

    public UrlEncoder usingJsAndPlus(){
        this.encodingFunction = jsUriAndPlus;
        return this;
    }


    public String encode() {
        if (pairs.size == 0) return "";
        MapFunction<String, String> encodingFunction = this.encodingFunction;
        StringBuilder builder = new StringBuilder();

        for (KeyValuePair pair : pairs) {
            builder.append(encodingFunction.map(pair.key))
                    .append("=")
                    .append(encodingFunction.map(pair.value))
                    .append("&");
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    public byte[] encode(Charset charset){
        return encode().getBytes(charset);
    }


    /**  Кодирует по правилам x-www-form-urlencoded документации {@link URLEncoder} **/
    public static String encodeJavaUrl(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ignore) {}
        return s;
    }

    /** Одноимённый метод в js. Кодирует прямо как там **/
    public static String encodeURIComponent(String s){
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("%21", "!")
                    .replaceAll("%7E", "~")
                    .replaceAll("%27", "'")
                    .replaceAll("%28", "(")
                    .replaceAll("%29", ")");
        } catch (UnsupportedEncodingException ignore) {}
        return s;
    }

    /**  Кодирует по правилам x-www-form-urlencoded документации Wikipedia **/
    public static String encodeURIComponentPlus(String s){
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replaceAll("%21", "!")
                    .replaceAll("%7E", "~")
                    .replaceAll("%27", "'")
                    .replaceAll("%28", "(")
                    .replaceAll("%29", ")");
        } catch (UnsupportedEncodingException ignore) {}
        return s;
    }
}
