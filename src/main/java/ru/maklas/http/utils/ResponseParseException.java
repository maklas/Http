package ru.maklas.http.utils;

import ru.maklas.http.Response;

import java.io.*;

/** Ошибка при парсинге ответа от сервера. **/
public class ResponseParseException extends Exception {

    Response response;
    ResponseBean bean;

    public ResponseParseException(Response response, ResponseBean bean, Throwable cause) {
        super(cause);
        this.response = response;
        this.bean = bean;
    }

    /** Ответ полученный от сервера который не получилось запарсить **/
    public Response getResponse() {
        return response;
    }

    /** Бин который использовался для парсинга. Не null и может содержать частичные данные. Потому желательно для всех бинов сделать **/
    public ResponseBean getBean() {
        return bean;
    }

    @Override
    public String getMessage() {
        return "Failed to parse response into bean " + bean.getClass().getSimpleName();
    }

    @Override
    public void printStackTrace(PrintWriter w){
        String beanAsString = null;
        try {
            beanAsString = bean.toString();
        } catch (Exception e) {
            beanAsString = "EXCEPTION on ResponseBean.toString()";
        }
        w.println("Bean: " + beanAsString);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        response.printTrace(bos);
        w.println(new String(bos.toByteArray()));
        w.flush();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
        printStackTrace(new PrintWriter(s));
    }

    public void printStackTrace(OutputStream out){
        printStackTrace(new PrintWriter(out));
    }
}
