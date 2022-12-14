/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dustin Thomson - initial API and implementation and/or initial documentation
 */

package org.eclipse.paho.client.mqttv3.internal;

import android.os.SystemClock;


/**
 * A high resolution timer appropriate for use by most JVMs.
 * <p>
 * This implementation delegates {@link #nanoTime()} to {@link System#nanoTime()}.
 * <p>
 * Note: This implementation is not appropriate for use on Android, as the clock backing {@link System#nanoTime()} stops
 * when the system enters deep sleep.
 */
public class SystemHighResolutionTimer implements HighResolutionTimer {
    @Override
    public long nanoTime() {
        //@Ben 配置Android framework,使Android进入深度休眠后能使用时钟
        return SystemClock.elapsedRealtimeNanos();
        //        return System.nanoTime();
    }
}
