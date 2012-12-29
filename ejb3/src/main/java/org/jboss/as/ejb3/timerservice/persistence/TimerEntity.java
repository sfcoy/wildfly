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

import java.io.Serializable;
import java.util.Date;

import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Stuart Douglas
 */
public class TimerEntity implements Serializable {

    protected final String id;

    protected final  String timedObjectId;

    protected final  Date initialDate;

    protected final  long repeatInterval;

    protected final  Date nextDate;

    protected final  Date previousRun;

    protected final  Serializable info;

    protected final  Object primaryKey;

    protected final  TimerState timerState;

    public TimerEntity(TimerImpl timer) {
        this.id = timer.getId();
        this.initialDate = timer.getInitialExpiration();
        this.repeatInterval = timer.getInterval();
        this.nextDate = timer.getNextExpiration();
        this.previousRun = timer.getPreviousRun();
        this.timedObjectId = timer.getTimedObjectId();
        this.info = timer.getTimerInfo();
        this.primaryKey = timer.getPrimaryKey();

        if(timer.getState() == TimerState.CREATED) {
            //a timer that has been persisted cannot be in the created state
            this.timerState = TimerState.ACTIVE;
        } else {
            this.timerState = timer.getState();
        }
    }

    public String getId() {
        return id;
    }

    public String getTimedObjectId() {
        return timedObjectId;
    }

    public Date getInitialDate() {
        return initialDate;
    }

    public long getInterval() {
        return repeatInterval;
    }

    public Serializable getInfo() {
        return this.info;
    }

    public Date getNextDate() {
        return nextDate;
    }

    public Date getPreviousRun() {
        return previousRun;
    }

    public TimerState getTimerState() {
        return timerState;
    }

    public Object getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Creates a {@link Timer} implementation corresponding to this entity.
     *
     * @param timerService
     * @return
     */
    public TimerImpl createTimer(TimerServiceImpl timerService) {
        return new TimerImpl(this, timerService);
    }

    /**
     * The identity of objects of this class and all it's subclasses is represented by the {@link #id} field.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TimerEntity other = (TimerEntity) obj;
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }


}
