/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.timerservice.persistence;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * The persisted attributes corresponding to an EJB Timeout method. An EJB timeout method may have zero args, or a single
 * {@link javax.ejb.Timer} arg. This class includes zero arg behaviour. See {@link TimeoutMethodWithTimer} for one arg
 * behaviour.
 *
 * @author Jaikiran Pai
 * @author Stuart Douglas
 * @author steve.coy
 */
public class TimeoutMethod implements Serializable {

    protected Long id;

    protected String declaringClass;

    protected String methodName;

    private transient String cachedToString;

    public TimeoutMethod(String declaringClass, String methodName) {
        this.declaringClass = declaringClass;
        this.methodName = methodName;
    }

    public Long getId() {
        return id;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    /**
     * @param classLoader the class loader containing the timeout method's classes.
     * @return the {@link java.lang.reflect.Method} represented by this object.
     */
    public Method findTimeoutMethod(ClassLoader classLoader) {
        for (Class<?> klass = this.lookupMethodDeclaringClass(classLoader); klass != null; klass = klass.getSuperclass()) {
            for (Method method : klass.getDeclaredMethods()) {
                if (this.matchesWith(method)) {
                    return method;
                }
            }
        }
        throw MESSAGES.failToFindTimeoutMethod(this);
    }

    /**
     * @param method
     * @return true if the method name and argument list matches this instance
     */
    public boolean matchesWith(Method method) {
        return method.getParameterTypes().length == 0 && method.getName().equals(methodName);
    }

    /**
     * @param classLoader
     * @return the {@link Class} object corresponding to this TimeoutMethod's declaring class
     */
    private Class<?> lookupMethodDeclaringClass(ClassLoader classLoader) {
        try {
            return Class.forName(declaringClass, false, classLoader);
        } catch (ClassNotFoundException cnfe) {
            // This should be impossible at this point...
            throw MESSAGES.failToLoadDeclaringClassOfTimeOut(declaringClass);
        }
    }

    @Override
    public String toString() {
        if (this.cachedToString == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.declaringClass);
            sb.append(".");
            sb.append(this.methodName);
            sb.append("(");
            sb.append(this.argsAsString());
            sb.append(")");
            this.cachedToString = sb.toString();
        }
        return this.cachedToString;
    }

    protected String argsAsString() {
        return "";
    }
}
