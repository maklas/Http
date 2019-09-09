# Http
Http library for managing http settings, requests and responses. Works with PC and Android.
What this library is - basically a convenient wrapper around `HttpUrlConnection` with documented methods and response parsing automatization

Example:
```java
public static void main(String[] args){
    setup(); //Initialize default settings

    CookieStore cookieStore = new CookieStore(); //Create cookie store for managing cookies (optional)
    cookieStore.setCookie(new Cookie("CookieKey", "CookieValue")); //assign cookies
    
    Request request = ConnectionBuilder.get("https://google.com")
            .h(Header.UserAgent.mostRecent)
            .h(Header.AcceptLanguage.en)
            .h(Header.AcceptEncoding.gzip)
            .h(Header.Accept.textAppImageOrAny)
            .assignCookieStore(cookieStore)
            .build(); //build request
    
    Response response = request.send(); //send request
    System.out.println(response.getTrace()); //print all information
}


public static void setup(){
    Http.setDefaultCookieHandlerByJFX(false); //Forbids using default java's CookieHandler
    Http.setDefaultKeepAlive(true);
    Http.setAutoAddHostHeader(true); 
}
```

## Do not use if
1. You need to support java 9 or above. Library relies on `HttpUrlConnection` to work (is deprecated since java 9)
2. You have a need to download binary data. This library can only receive string data (html, xml, json, plain text, etc.)
3. You have a need to download data straight into files. No `OutputStream` is available, all Strings are loaded directly to ram

## You can use it if
1. You need a library for making API request with string data as a response
2. You need browser-like behaviour for simulating user activity
3. You want to make a bot that allows managing multiple accounts at the same time. (Multiple cookie storages)

## Features
1. Http version 1.1
2. Making full blown requests with header customization
3. Allows requests over proxies
4. Manages cookies (storing, creation and deletion).
5. Having multiple CookieStores allows for simulation of multiple users. Say for a multi-account bot.
6. No need to worry about url encoding and Content-Type
7. Supports gzip and deflate Content-Encoding
8. Much easier to control request timeout.
9. Add it to your project with [Jitpack](https://jitpack.io/#maklas/Http)!