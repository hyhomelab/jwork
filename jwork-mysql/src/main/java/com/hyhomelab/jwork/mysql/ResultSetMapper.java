package com.hyhomelab.jwork.mysql;

import java.sql.ResultSet;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/5/7 16:19
 */
@FunctionalInterface
public interface ResultSetMapper<T>{
    T map(ResultSet rs) throws Exception;
}
