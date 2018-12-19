package ru.maklas.http;

import org.apache.commons.lang3.StringUtils;

public class Header extends KeyValuePair {

    public Header(String key, String value) {
        super(key, value);
    }

    /**
     * @return new Header instance with the same key, but changed value
     */
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
        public static       Header mostRecent = chrome_08_2018;
    }


    public static class Host {
        public static final String key = "Host";
        public static Header of(String s){
            return new Header(key, s);
        }

        public static Header fromUrl(String url){
            String host;
            if (StringUtils.startsWithIgnoreCase(url, "http://")){
                host = url.substring(7);
            } else if (StringUtils.startsWithIgnoreCase(url, "https://")) {
                host = url.substring(8);
            } else host = url;

            char[] chars = host.toCharArray();
            char target = '/';
            int position = -1;
            boolean found = false;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == target){
                    found = true;
                    position = i;
                    break;
                }
            }
            if (found){
                host = host.substring(0, position);
            }
            return Header.Host.of(host);
        }
    }

    //Использовать ли постоянное соединение или же единичные запросы после которых следуюет закртие сокета
    public static class Connection{
        public static final String key = "Connection";
        public static final Header keepAlive = new Header(key, "keep-alive");
        public static final Header close = new Header(key, "close");
    }

    //
    public static class Accept{
        public static final String key = "Accept";
        public static final Header all = new Header(key, "*/*");
        public static final Header html = new Header(key, "text/html");
        public static final Header appJson = new Header(key, "application/json");
        public static final Header appJsonOrJS = new Header(key, "application/json, text/javascript, */*; q=0.01");
        public static final Header textAppImage = new Header(key, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
    }

    //
    public static class Origin{
        public static final String key = "Origin";
    }

    //Контроль кеша. Как в запросах так и в ответах.
    public static class CacheControl{
        public static final String key = "Cache-Control";
        public static final Header noStore_noCache_revalidate = new Header(key, "no-store, no-cache, must-revalidate");
        public static final Header noCache = new Header(key, "no-cache");
        public static final Header pragmaNoCache = new Header("Pragma", "no-cache"); //HTTP 1.0
        public static final Header maxAge0 = new Header(key, "max-age=0");

    }

    public static class RequestedWith{
        public static final String key = "X-Requested-With";
        public static final Header ajax = new Header(key, "XMLHttpRequest");
    }

    public static class AcceptEncoding{
        public static final String key              = "Accept-Encoding";
        public static final Header gzip             = new Header(key, "gzip");
        public static final Header gzipDeflate      = new Header(key, "gzip, deflate");
        public static final Header gzipDeflateBr    = new Header(key, "gzip, deflate, br");
        public static final Header compress         = new Header(key, "compress");
        public static final Header deflate          = new Header(key, "deflate");
        public static final Header br               = new Header(key, "br");
        public static final Header identity         = new Header(key, "identity");
        public static final Header any              = new Header(key, "*");
    }

    public static class ContentEncoding {
        public static final String key = "Content-Encoding";
        public static final Header gzip      = new Header(key, "gzip");
        public static final Header compress  = new Header(key, "compress");
        public static final Header deflate   = new Header(key, "deflate");
        public static final Header identity  = new Header(key, "identity");
        public static final Header br        = new Header(key, "br");
    }

    public static class AcceptLanguage{
        public static final String key          = "Accept-Language";
        public static final Header ru           = new Header(key, "ru-RU,ru");
        public static final Header en           = new Header(key, "en");
        public static final Header ruEn         = new Header(key, "en-US,en");
        public static       Header defaultLang  = new Header(key, "ru,en-US;q=0.9,en;q=0.8");
    }

    //Форматы передачи тела сообщения. Для обычных html страниц - chunked. Можно задавать в запросе AcceptEncoding
    public static class TransferEncoding{
        public static final String key          = "Transfer-Encoding";
        public static final Header chunked      = new Header(key, "chunked");
        public static final Header compress     = new Header(key, "compress");
        public static final Header deflate      = new Header(key, "deflate");
        public static final Header gzip         = new Header(key, "gzip");
        public static final Header identity     = new Header(key, "identity");
    }

    public static class Referer{
        public static final String key = "Referer";
    }

    //В ответе от сервера. Указывает какой вид контента содержится в теле. (MIME-type)
    public static class ContentType{
        public static final String key                          = "Content-Type";
        public static final Header application_x_www_form_url   = new Header(key, "application/x-www-form-urlencoded; charset=UTF-8");
        public static final Header textHtml                     = new Header(key, "text/html; charset=UTF-8");
        public static final Header textPlain                    = new Header(key, "text/plain; charset=UTF-8");
        public static final Header applicationJson              = new Header(key, "application/json; charset=UTF-8");
        public static final Header applicationJS                = new Header(key, "application/javascript; charset=UTF-8");
    }

    public static class SetCookie{
        public static final String key = "Set-Cookie";
    }

    public static class UpgradeInsecure {
        public static final String key = "Upgrade-Insecure-Requests";
        public static final Header one = new Header(key, "1");
    }


    /**
     * <p>
     *      Памятка:
     * </p>
     *
     * <p>
     *     <li>Обязательно указывать метод запроса</li>
     *     <li>Обязательно указывать URL</li>
     *     <li>Не кешируем</li>
     *     <li>Не забываем указывать Accept-тип</li>
     *     <li>Не забываем указывать Cache-Control</li>
     *     <li>Не забываем указывать Язык ру</li>
     *     <li>Не забываем указывать Что тип соединения - единичные запросы (Connection.Close)</li>
     *     <li>Можно использовать gzip архивацию</li>
     *     <li>Проверяем не редиректнули нас</li>
     *     <li>Всегда закрываем соединение в конце</li>
     * </p>
     *
     */
    public static class Note{}


    /**
      combine("ru-RU,ru", "q=0.8,en-US", "q=0.6,en", "q=0.4") -> ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4;
     *
     */
    public static String combine(String... value){
        return combine(';', value);
    }

    public static String combine(char separator, String... value){
        if (value.length == 0) return "";
        if (value.length == 1) return value[0];


        StringBuilder builder = new StringBuilder();
        for (String s : value) {
            builder
                    .append(s)
                    .append(separator);
        }

        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

}
