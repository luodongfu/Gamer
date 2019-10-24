package com.thinkerwolf.gamer.core.adaptor;

import com.thinkerwolf.gamer.common.util.ClassUtils;
import com.thinkerwolf.gamer.core.servlet.Request;
import com.thinkerwolf.gamer.core.servlet.Response;

public class NameInjector implements ParamInjector {

    private String name;

    private Class<?> toClass;

    public NameInjector(String name, Class<?> toClass) {
        this.name = name;
        this.toClass = toClass;
    }

    @Override
    public Object inject(Request request, Response response) throws Exception {
        return ClassUtils.castTo(request.getAttribute(name), toClass);
    }
}
