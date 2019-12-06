package ru.maklas.http.utils;

import ru.maklas.http.Response;

public interface ResponseBean {

	void parse(Response resp) throws Exception;

}
