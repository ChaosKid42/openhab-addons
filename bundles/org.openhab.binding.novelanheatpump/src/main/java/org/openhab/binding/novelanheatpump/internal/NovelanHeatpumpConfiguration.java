/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.novelanheatpump.internal;

/**
 * The {@link NovelanHeatpumpConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christoph Scholz - Initial contribution
 */
public class NovelanHeatpumpConfiguration {

    // Novelan Heatpump Thing constants
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String CONNECTION_TIMEOUT = "connectionTimeout";
    public static final String POLL_PERIOD = "pollPeriod";

    /**
     * The IP address of the Novelan Heatpump
     */
    public String host;

    /**
     * The port number of the Novelan Heatpump
     */
    public Integer port;

    /**
     * The Socket connection timeout for the Novelan Heatpump
     */
    public Integer connectionTimeout;

    /**
     * The Parameter Poll Period. Can be set in range 1-15 minutes. Default is 1 minute;
     */
    public Integer pollPeriod;
}
