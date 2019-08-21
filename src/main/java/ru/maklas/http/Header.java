package ru.maklas.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Header extends KeyValuePair {

    public Header(String key, String value) {
        super(key, value);
    }

    /** @return new Header instance with the same key, but changed value **/
    public Header withValue(String value){
        return new Header(key, value);
    }

    @Override
    public String toString() {
        return key + ": " + value;
    }








    public static class UserAgent {
        public static final String key = "User-Agent";

        public static final Header chrome_07_2018 = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36");
        public static final Header chrome_08_2018 = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
        public static final Header chrome_12_2018 = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        public static final Header chrome_04_2019 = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
        public static final Header chrome_07_2019 = new Header(key, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36");
        public static       Header mostRecent = chrome_07_2019;

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class Host {
        public static final String key = "Host";
        public static Header of(String s){
            return new Header(key, s);
        }

        public static Header fromUrl(URL url){
            return new Header(key, url.getHost());
        }

        public static Header fromUrl(String url){
            URL javaUrl = null;
            try {
                javaUrl = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return new Header(key, javaUrl != null ? javaUrl.getHost() : "");
        }
    }

    //Использовать ли постоянное соединение или же единичные запросы после которых следуюет закртие сокета
    public static class Connection{
        public static final String key = "Connection";
        public static final Header keepAlive = new Header(key, "keep-alive");
        public static final Header close = new Header(key, "close");

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class Accept{
        public static final String key = "Accept";
        public static final Header all = new Header(key, "*/*");
        public static final Header html = new Header(key, "text/html");
        public static final Header appJson = new Header(key, "application/json");
        public static final Header appJsonOrJS = new Header(key, "application/json, text/javascript, */*; q=0.01");
        public static final Header textAppImageOrAny = new Header(key, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");

        public static Header with(String val){
            return new Header(key, val);
        }

        public static Header multi(String... vals){
            return multiComma(key, vals);
        }
    }

    public static class Origin {
        public static final String key = "Origin";

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    //Контроль кеша. Как в запросах так и в ответах.
    public static class CacheControl{
        public static final String key = "Cache-Control";
        public static final Header noStore_noCache_revalidate = new Header(key, "no-store, no-cache, must-revalidate");
        public static final Header noCache = new Header(key, "no-cache");
        public static final Header pragmaNoCache = new Header("Pragma", "no-cache"); //HTTP 1.0
        public static final Header maxAge0 = new Header(key, "max-age=0");
        public static final Header privat = new Header(key, "private");

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class RequestedWith{
        public static final String key = "X-Requested-With";
        public static final Header ajax = new Header(key, "XMLHttpRequest");

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class AcceptEncoding {
        public static final String key              = "Accept-Encoding";
        public static final Header gzip             = new Header(key, "gzip");
        public static final Header gzipDeflate      = new Header(key, "gzip, deflate");
        public static final Header compress         = new Header(key, "compress");
        public static final Header deflate          = new Header(key, "deflate");
        public static final Header identity         = new Header(key, "identity");
        public static final Header any              = new Header(key, "*");

        public static Header with(String val){
            return new Header(key, val);
        }

        public static Header multi(String... values){
            return Header.multiComma(key, values);
        }
    }

    public static class ContentEncoding {
        public static final String key = "Content-Encoding";
        public static final Header gzip      = new Header(key, "gzip");
        public static final Header compress  = new Header(key, "compress");
        public static final Header deflate   = new Header(key, "deflate");
        public static final Header identity  = new Header(key, "identity");
        public static final Header br        = new Header(key, "br");

        public static Header with(String val){
            return new Header(key, val);
        }

        public static Header multi(String... values){
            return Header.multiComma(key, values);
        }
    }

    public static class AcceptLanguage{

        public static final String key          = "Accept-Language";
        public static final Header en           = new Header(key, "en");
        public static final Header ru           = new Header(key, "ru-RU,ru");
        public static final Header de           = new Header(key, "de");
        public static final Header fr           = new Header(key, "fr");
        public static       Header defaultLang  = en;


        public static Header with(String val){
            return new Header(key, val);
        }
    }

    //Форматы передачи тела сообщения. Для обычных html страниц - chunked. Можно задавать в запросе AcceptEncoding
    public static class TransferEncoding {
        public static final String key          = "Transfer-Encoding";
        public static final Header chunked      = new Header(key, "chunked");
        public static final Header compress     = new Header(key, "compress");
        public static final Header deflate      = new Header(key, "deflate");
        public static final Header gzip         = new Header(key, "gzip");
        public static final Header identity     = new Header(key, "identity");

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class Referer{
        public static final String key = "Referer";

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    //В ответе от сервера. Указывает какой вид контента содержится в теле. (MIME-type)
    public static class ContentType extends Header {
        public static final String key                  = "Content-Type";
        public static final ContentType textPlain       = new ContentType("text/plain; charset=UTF-8");
        public static final ContentType textHtml        = new ContentType("text/html; charset=UTF-8");
        public static final ContentType textXml         = new ContentType("text/xml; charset=UTF-8");
        public static final ContentType appXml          = new ContentType("application/xml; charset=UTF-8");
        public static final ContentType appJson         = new ContentType("application/json; charset=UTF-8");
        public static final ContentType appJS           = new ContentType("application/javascript; charset=UTF-8");
        public static final ContentType form_urlencoded = new ContentType("application/x-www-form-urlencoded; charset=UTF-8");
        public static final ContentType formData        = new ContentType("multipart/form-data; charset=UTF-8");

        ContentType(String value) {
            super(key, value);
        }

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class SetCookie{
        public static final String key = "Set-Cookie";

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    public static class UpgradeInsecure {
        public static final String key = "Upgrade-Insecure-Requests";
        public static final Header one = new Header(key, "1");

        public static Header with(String val){
            return new Header(key, val);
        }
    }

    //Только в ответе от сервера. Указывает на директорию файла. например "/documents/foo.json"
    public static class ContentLocation{
        public static final String key = "Content-Location";
    }

    public static class DateHeader {
        public static final String key = "Date";
        private static SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");


        public static Header fromDate(Date date){
            return new Header(key, format.format(date));
        }


        public static Date fromHeader(Header header) throws ParseException {
            return format.parse(header.value);
        }

    }

    //редирект
    public static class Location {
        public static final String key = "Location";
    }

    private static Header multiComma(String key, String... encodings){
        if (encodings == null || encodings.length == 0) return new Header(key, "");
        else if (encodings.length == 1) return new Header(key, encodings[0]);

        StringBuilder sb = new StringBuilder(encodings[0]);
        for (int i = 1; i < encodings.length; i++) {
            sb.append(", ").append(encodings[i]);
        }
        return new Header(key, sb.toString());
    }

}
