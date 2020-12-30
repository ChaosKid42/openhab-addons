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

    /**
     * The IP address of the Novelan Heatpump
     */
    public String host;

    /**
     * The port number of the Novelan Heatpump
     */
    public Integer port;

    /**
     * The socket connection timeout for the Novelan Heatpump
     */
    public Integer connectionTimeout;

    /**
     * The polling interval in s
     */
    public Integer pollingInterval;
}
