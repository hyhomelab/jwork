package com.hyhomelab.jwork;

import java.io.Serializable;

/**
 * @author hyhomelab
 * @email hyhomelab@hotmail.com
 * @date 2025/4/29 15:44
 */
public interface Trigger extends Serializable {

    long nextTimeSec();

}
