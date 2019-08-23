package ru.maklas.http;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/** Cookie is considered to be deleted if it's value was set to empty string or null or 'false' or 'deleted'. **/
public class Cookie {

    public static final String headerKey = "Cookie";
    public static final String DELETED = "deleted";

    final String key;
    volatile String value;
    private boolean deleted;
    private long created;
    private long expires = -1;
    private int maxAge = -1;
    private String domain;
    private String path;
    private boolean secure;
    private boolean httpOnly;


    public Cookie(Cookie cookie) {
        this.key = cookie.key;
        update(cookie);
    }

    /**
     * <p>
     *     <b>Warning!</b>
     * </p>
     * <p>
     *     A cookie created this way might have empty domain. Meaning it will be used for any url.
     *     A much safer choice - Creating cookies with domain name specified.
     * </p>
     */
    public Cookie(String key, String value) {
        if (key == null) throw new RuntimeException();
        this.key = key;
        this.deleted = shouldBeDeleted(value);
        this.value =  deleted ? DELETED : value;
        this.created = System.currentTimeMillis();
    }

    public Cookie(String key, String value, String domain) {
        this(key, value);
        setDomain(domain);
    }

    void update(Cookie cookie) {
        this.value = cookie.value;
        this.deleted = cookie.deleted;
        this.created = cookie.created;
        this.expires = cookie.expires;
        this.maxAge = cookie.maxAge;
        this.domain = cookie.domain;
        this.path = cookie.path;
        this.secure = cookie.secure;
        this.httpOnly = cookie.httpOnly;
    }

    @NotNull
    public String getKey() {
        return key;
    }

    @NotNull
    public String getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    /** Epoch time in milliseconds. -1 for unset **/
    public long getExpires() {
        return expires;
    }

    public void setExpires(long expires) {
        this.expires = expires;
    }

    /** Epoch time in milliseconds.Time at which this cookie was set by the server **/
    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    /** Time in seconds since creation of cookies. -1 for unset **/
    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     *  Specifies those hosts to which the cookie will be sent.
     *  If empty, means it was created manualy and will be allowed for any url
     */
    public String getDomain() {
        return domain != null ? domain : "";
    }

    /**
     * <li>.example.com -> example.com</li>
     * <li>www.example.com -> example.com</li>
     * <li>.www.example.com -> example.com</li>
     * <li>eXamPle.com -> example.com</li>
     */
    public void setDomain(String domain) {
        this.domain = formatDomain(domain);
    }

    private static String formatDomain(String unformatted){
        if (unformatted != null){
            unformatted = unformatted.startsWith(".") ? unformatted.substring(1) : unformatted;
            return unformatted.startsWith("www.") ? unformatted.substring(4).toLowerCase() : unformatted.toLowerCase();
        }
        return null;
    }

    /** Indicates a URL path that must exist in the requested resource before sending the Cookie. Can be empty **/
    public String getPath() {
        return path != null ? path : "";
    }

    public void setPath(String path) {
        this.path = path;
    }

    /** Must only be sent with HTTPS or SSL **/
    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * HTTP-only cookies aren't accessible via JavaScript through the Document.cookie property,
     * the XMLHttpRequest API, or the Request API to mitigate attacks against cross-site scripting (XSS).
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean isExpiresSet(){
        return expires != -1;
    }

    public boolean isMaxAgeSet(){
        return maxAge != -1;
    }

    public boolean appliesToDomain(@NotNull String domain){
        domain = domain.toLowerCase();
        return StringUtils.isNotEmpty(this.domain) &&
                (this.domain.equals(domain) ||
                        (domain.length() > this.domain.length() &&
                                domain.endsWith(this.domain) &&
                                domain.charAt(domain.length() - this.domain.length() - 1) == '.'));
    }

    /**
     * A cookie considered "expired" if
     * Max-Age or Expires parameters are set and too much time has passed
     */
    public boolean isExpired(){
        long now = System.currentTimeMillis();
        return (maxAge != -1 && (maxAge * 1000L + created < now)) || (expires != -1 && expires < now);
    }

    /**
     * A session cookie is cookie that has no Max-Age nor Expires set.
     * In which case it must be deleted after client shuts down
     */
    public boolean isSessionCookie(){
        return isMaxAgeSet() || isExpiresSet();
    }

    /**
     * Cookie was set to empty or "deleted".
     * <b>Warning!</b>. Doesn't take {@link #isExpired()} into account.
     */
    public static boolean shouldBeDeleted(String newValue) {
        return newValue == null || newValue.equals("") || newValue.equalsIgnoreCase(DELETED);
    }

    void setValue(String value) {
        if (value == null || value.equals("") || value.equalsIgnoreCase(DELETED)){
            this.value = DELETED;
            this.deleted = true;
        } else {
            this.value = value;
            this.deleted = false;
        }
    }

    public static Cookie fromSetCookieValue(URL url, String value){
        Cookie cookie;
        String[] portions = value.split(";");
        String[] keyValuePart = portions[0].split("=");

        if (keyValuePart.length == 1) {
            cookie = new Cookie(keyValuePart[0], "");
        } else {
            cookie = new Cookie(keyValuePart[0].trim(), keyValuePart[1].trim());
        }
        try {
            cookie.fillAttributes(url.getHost().toLowerCase(), portions);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return cookie;
    }

    //Fills other fields : httpOnly, secure, expires, maxAge, domain, path.
    private void fillAttributes(String lowerCaseHost, String[] portions) {
        for (int i = 1; i < portions.length; i++) {
            String[] split = portions[i].split("=");
            if (split.length == 1) {
                String modificator = split[0].trim();
                if ("HttpOnly".equalsIgnoreCase(modificator)) {
                    httpOnly = true;
                } else if ("Secure".equalsIgnoreCase(modificator)){
                    secure = true;
                }
            } else {
                String mKey = split[0].trim();
                String mValue = split[1].trim();
                if ("Expires".equalsIgnoreCase(mKey)){
                    expires = parseExpires(mValue);
                } else if ("Max-Age".equalsIgnoreCase(mKey)){
                    try {
                        maxAge = Integer.parseInt(mValue);
                    } catch (Throwable ignore) { }
                } else if ("Domain".equalsIgnoreCase(mKey)){
                    String domain = formatDomain(mValue);
                    if (domain.equals(lowerCaseHost) ||
                            (domain.length() > lowerCaseHost.length() && domain.endsWith(lowerCaseHost) && domain.charAt(domain.length() - lowerCaseHost.length() - 1) == '.')) {
                        this.domain = domain;
                    }
                } else if ("Path".equalsIgnoreCase(mKey)){
                    path = mValue;
                }
            }
        }

        if (domain == null){
            setDomain(lowerCaseHost);
        }

        if (path == null){
            path = "";
        }
    }

    private static long parseExpires(String date) {
        if (date.length() == 29){ //Sun, 06 Nov 1994 08:49:37 GMT
            try {
                return parseDate29(date);
            } catch (Exception e) {
                try {
                    return parseDateJava29(date);
                } catch (ParseException e1) {
                    return -1;
                }
            }
        } else if (date.length() == 24){ //Sun Nov  6 08:49:37 1994
            try {
                return parseDate24(date);
            } catch (Throwable e){
                try {
                    return parseDateJava24(date);
                } catch (Exception e1) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static long parseDateJava29(String httpDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.parse(httpDate).getTime();
    }

    private static long parseDateJava24(String httpDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.parse(httpDate).getTime();
    }

    //RFC 822, updated by RFC 1123 -> Sun, 06 Nov 1994 08:49:37 GMT
    private static long parseDate29(String httpDate){
        int day = Integer.parseInt(httpDate.substring(5, 7));
        int month = parseMonth(httpDate.substring(8, 11));
        int year = Integer.parseInt(httpDate.substring(12, 16));
        int hrs = Integer.parseInt(httpDate.substring(17, 19));
        int mins = Integer.parseInt(httpDate.substring(20, 22));
        int secs = Integer.parseInt(httpDate.substring(23, 25));
        return toEpochSec(day, month, year, hrs, mins, secs);
    }

    //ANSI C's asctime() format -> Sun Nov  6 08:49:37 1994
    private static long parseDate24(String httpDate){
        int day = Integer.parseInt(httpDate.substring(8, 10).trim());
        int month = parseMonth(httpDate.substring(4, 7));
        int year = Integer.parseInt(httpDate.substring(20, 24));
        int hrs = Integer.parseInt(httpDate.substring(11, 13));
        int mins = Integer.parseInt(httpDate.substring(14, 16));
        int secs = Integer.parseInt(httpDate.substring(17, 19));

        return toEpochSec(day, month, year, hrs, mins, secs);
    }

    private static int parseMonth(String month) {
        switch (month){
            case "Jan": return 0;
            case "Feb": return 1;
            case "Mar": return 2;
            case "Apr": return 3;
            case "May": return 4;
            case "Jun": return 5;
            case "Jul": return 6;
            case "Aug": return 7;
            case "Sep": return 8;
            case "Oct": return 9;
            case "Nov": return 10;
            case "Dec": return 11;
        }
        return 0;
    }

    private static long secBeforeYear(int year){
        int diff = year - 1970;
        int superLeapDays = year >= 2000 ? 1 + (year - 2001) / 400: 0;
        int ignoredLeapDays = year >= 2000 ? 1 + (year - 2001) / 100: 0;
        int regularLeapDays = ((year - 1) / 4) - 492;
        return ((diff * 365L) + (regularLeapDays - ignoredLeapDays + superLeapDays)) * 24 * 60 * 60;
    }

    private static int secBeforeMonth(int year, int month){
        boolean isLeap = isLeapYear(year);
        int total = 0;
        for (int i = 0; i < month; i++) {
            total += daysInMonth(isLeap, i) * 24 * 60 * 60;
        }
        return total;
    }

    private static int daysInMonth(boolean isLeap, int month) {
        if (month >= 7) month++;
        if (month % 2 == 0) return 31;
        if (month == 1) return isLeap? 29 : 28;
        return 30;
    }

    private static boolean isLeapYear(int year) {
        return year % 400 == 0 || (year % 100 != 0 && year % 4 == 0);
    }

    private static long toEpochSec(int day, int month, int year, int hrs, int mins, int secs) {
        final int secInDay = 86400;
        final int secInHr = 3600;
        final int secInMin = 60;
        return (secBeforeYear(year) + secBeforeMonth(year, month) + ((day - 1) * secInDay) + (hrs * secInHr) + (mins * secInMin) + secs) * 1000L;
    }

    @Override
    public String toString() {
        return "{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", created=" + created +
                ", expires=" + expires +
                ", maxAge=" + maxAge +
                ", domain='" + domain + '\'' +
                ", path='" + path + '\'' +
                ", secure=" + secure +
                ", httpOnly=" + httpOnly +
                '}';
    }
}
