package com.thinkerwolf.gamer.core.mvc;

import com.thinkerwolf.gamer.core.servlet.Filter;
import com.thinkerwolf.gamer.core.servlet.FilterChain;
import com.thinkerwolf.gamer.core.servlet.Request;
import com.thinkerwolf.gamer.core.servlet.Response;

import java.util.Collection;
import java.util.Iterator;

public class ApplicationFilterChain implements FilterChain {

    private Iterator<Filter> iterator;

    public ApplicationFilterChain(Collection<Filter> filters) {
        this.iterator = filters.iterator();
    }

    @Override
    public void doFilter(Invocation invocation, Request request, Response response) throws Exception {
        if (hasNext()) {
            next().doFilter(invocation, request, response, this);
        } else {
            invocation.handle(request, response);
        }
    }

    @Override
    public Filter next() {
        return iterator.next();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
}
