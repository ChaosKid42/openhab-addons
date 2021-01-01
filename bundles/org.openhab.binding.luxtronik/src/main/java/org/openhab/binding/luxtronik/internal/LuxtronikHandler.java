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
package org.openhab.binding.luxtronik.internal;

import static org.openhab.binding.luxtronik.internal.LuxtronikBindingConstants.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ValueRange;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Heatpump binding connects to a Luxtronik/Novelan (Siemens) Heatpump with the
 * {@link HeatpumpConnector} and read the internal state array every minute.
 * With the state array each binding will be updated.
 *
 * @author Jan-Philipp Bolle - Initial contribution
 * @author John Cocula - made port configurable
 * @author Hilbrand Bouwkamp - Migrated to openHAB 3
 */
@NonNullByDefault
public class LuxtronikHandler extends BaseThingHandler {

    /** Parameter code for heating operation mode */
    private static int PARAM_HEATING_OPERATION_MODE = 3;
    /** Parameter code for heating temperature */
    private static int PARAM_HEATING_TEMPERATURE = 1;
    /** Parameter code for warmwater operation mode */
    private static int PARAM_WARMWATER_OPERATION_MODE = 4;
    /** Parameter code for warmwater temperature */
    private static int PARAM_WARMWATER_TEMPERATURE = 2;
    /** Parameter code for cooling operation mode */
    private static int PARAM_COOLING_OPERATION_MODE = 108;
    /** Parameter code for cooling release temperature */
    private static int PARAM_COOLING_RELEASE_TEMP = 110;
    /** Parameter code for target temp MK1 */
    private static int PARAM_COOLING_INLET_TEMP = 132;
    /** Parameter code for start cooling after hours */
    private static int PARAM_COOLING_START = 850;
    /** Parameter code for stop cooling after hours */
    private static int PARAM_COOLING_STOP = 851;

    private final Logger logger = LoggerFactory.getLogger(LuxtronikHandler.class);

    private @NonNullByDefault({}) LuxtronikConfiguration config;
    private @Nullable ScheduledFuture<?> refreshTask;

    public LuxtronikHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // Refresh is done in scheduled refresh.
            return;
        }
        switch (channelUID.getId()) {
            case CHANNEL_HEATING_OPERATION_MODE:
                if (command instanceof StringType) {
                    try {
                        final int value = Integer.parseInt(((StringType) command).toString());
                        if (ValueRange.of(0, 4).isValidValue(value)) {
                            sendParamToHeatpump(PARAM_HEATING_OPERATION_MODE, value);
                        } else {
                            logger.warn("Heatpump heating operation mode for channel {} with value {} is unknown.",
                                    channelUID, value);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Heatpump heating operation mode for channel {} with value {} is unknown.",
                                channelUID, command);
                    }
                } else {
                    logger.warn("Heatpump heating operation mode item {} must be from type:{}.", channelUID,
                            StringType.class.getSimpleName());
                }
                break;
            // old openHAB 1 code from here - remove line when done
            case CHANNEL_HEATING_TEMPERATURE:
                if (command instanceof DecimalType) {
                    float temperature = ((DecimalType) command).floatValue();
                    int value = (int) (temperature * 10.);
                    sendParamToHeatpump(PARAM_HEATING_TEMPERATURE, value);
                } else {
                    logger.warn("Heatpump heating temperature item {} must be from type:{}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
            case CHANNEL_WARMWATER_OPERATION_MODE:
                if (command instanceof StringType) {
                    try {
                        final int value = Integer.parseInt(((StringType) command).toString());
                        if (ValueRange.of(0, 4).isValidValue(value)) {
                            sendParamToHeatpump(PARAM_WARMWATER_OPERATION_MODE, value);
                        } else {
                            logger.warn("Heatpump warmwater operation mode with value {} is unknown.", value);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Heatpump warmwater operation mode for channel {} with value {} is unknown.",
                                channelUID, command);
                    }
                } else {
                    logger.warn("Heatpump warmwater operation mode item {} must be from type: {}.", channelUID,
                            StringType.class.getSimpleName());
                }
                break;
            case CHANNEL_WARMWATER_TEMPERATURE:
                if (command instanceof DecimalType) {
                    float temperature = ((DecimalType) command).floatValue();
                    int value = (int) (temperature * 10.);
                    sendParamToHeatpump(PARAM_WARMWATER_TEMPERATURE, value);
                } else {
                    logger.warn("Heatpump warmwater temperature item {} must be from type: {}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
            case CHANNEL_COOLING_OPERATION_MODE:
                if (command instanceof StringType) {
                    try {
                        final int value = Integer.parseInt(((StringType) command).toString());
                        if (ValueRange.of(0, 1).isValidValue(value)) {
                            sendParamToHeatpump(PARAM_COOLING_OPERATION_MODE, value);
                        } else {
                            logger.warn("Heatpump cooling operation mode with value {} is unknown.", value);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Heatpump cooling operation mode for channel {} with value {} is unknown.",
                                channelUID, command);
                    }
                } else {
                    logger.warn("Heatpump cooling operation mode item {} must be from type: {}.", channelUID,
                            StringType.class.getSimpleName());
                }
                break;
            case CHANNEL_COOLING_RELEASE_TEMPERATURE:
                if (command instanceof DecimalType) {
                    float temperature = ((DecimalType) command).floatValue();
                    int value = (int) (temperature * 10.);
                    sendParamToHeatpump(PARAM_COOLING_RELEASE_TEMP, value);
                } else {
                    logger.warn("Heatpump cooling release temperature item {} must be from type: {}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
            case CHANNEL_COOLING_INLET_TEMP:
                if (command instanceof DecimalType) {
                    float temperature = ((DecimalType) command).floatValue();
                    int value = (int) (temperature * 10.);
                    sendParamToHeatpump(PARAM_COOLING_INLET_TEMP, value);
                } else {
                    logger.warn("Heatpump cooling target temp MK1 item {} must be from type: {}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
            case CHANNEL_COOLING_START_AFTER_HOURS:
                if (command instanceof DecimalType) {
                    float hours = ((DecimalType) command).floatValue();
                    int value = (int) (hours * 10.);
                    sendParamToHeatpump(PARAM_COOLING_START, value);
                } else {
                    logger.warn("Heatpump cooling start after hours item {} must be from type: {}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
            case CHANNEL_COOLING_STOP_AFTER_HOURS:
                if (command instanceof DecimalType) {
                    float hours = ((DecimalType) command).floatValue();
                    int value = (int) (hours * 10.);
                    sendParamToHeatpump(PARAM_COOLING_STOP, value);
                } else {
                    logger.warn("Heatpump cooling stop after hours item {} must be from type: {}.", channelUID,
                            DecimalType.class.getSimpleName());
                }
                break;
        }
    }

    /**
     * Set a parameter on the heatpump.
     *
     * @param param
     * @param value
     */
    private void sendParamToHeatpump(int param, int value) {
        synchronized (this) {
            try (HeatpumpConnector connection = new HeatpumpConnector(config.host, config.port,
                    config.connectionTimeout)) {
                connection.setParam(param, value);
            } catch (UnknownHostException e) {
                logger.warn("The given address '{}' of the heatpump is unknown", getAddress());
            } catch (IOException e) {
                logger.warn("Couldn't establish network connection [address '{}']", getAddress());
            }
        }
    }

    @Override
    public void initialize() {
        LuxtronikConfiguration config = getConfigAs(LuxtronikConfiguration.class);
        this.config = config;

        updateStatus(ThingStatus.UNKNOWN);
        refreshTask = scheduler.scheduleWithFixedDelay(this::refresh, 0, config.pollingInterval, TimeUnit.SECONDS);
    }

    private String getAddress() {
        return config.host + ":" + config.port;
    }

    private ZonedDateTime getZdtFromTimestamp(long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault());
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> refreshTask = this.refreshTask;

        if (refreshTask != null) {
            refreshTask.cancel(true);
            this.refreshTask = null;
        }
    }

    public void refresh() {
        try {
            final int[] heatpumpValues;
            final int[] heatpumpParams;

            synchronized (this) {
                try (HeatpumpConnector connection = new HeatpumpConnector(config.host, config.port,
                        config.connectionTimeout)) {
                    // read all available values
                    heatpumpValues = connection.getValues();
                    // read all parameters
                    heatpumpParams = connection.getParams();
                } catch (UnknownHostException e) {
                    if (isInitialized()) {
                        logger.debug("The given address '{}' of the heatpump is unknown", getAddress());
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    }
                    return;
                } catch (IOException e) {
                    if (isInitialized()) {
                        logger.debug("Couldn't establish network connection [address '{}']", getAddress());
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                    }
                    return;
                }
            }

            // all temperatures are 0.2 degree Celsius exact
            // but use int to save values
            // example 124 is 12.4 degree Celsius

            // workaround for thermal energies
            // the thermal energies can be unreasonably high in some cases, probably due to a sign bug in the firmware
            // trying to correct this issue here
            if (heatpumpValues[151] >= 214748364) {
                heatpumpValues[151] -= 214748364;
            }
            if (heatpumpValues[152] >= 214748364) {
                heatpumpValues[152] -= 214748364;
            }
            if (heatpumpValues[153] >= 214748364) {
                heatpumpValues[153] -= 214748364;
            }
            if (heatpumpValues[154] >= 214748364) {
                heatpumpValues[154] -= 214748364;
            }

            updateState(CHANNEL_TEMPERATURE_SUPPLY, new DecimalType((double) heatpumpValues[10] / 10));
            updateState(CHANNEL_TEMPERATURE_RETURN, new DecimalType((double) heatpumpValues[11] / 10));
            updateState(CHANNEL_TEMPERATURE_REFERENCE_RETURN, new DecimalType((double) heatpumpValues[12] / 10));
            updateState(CHANNEL_TEMPERATURE_OUT_EXTERNAL, new DecimalType((double) heatpumpValues[13] / 10));
            updateState(CHANNEL_TEMPERATURE_HOT_GAS, new DecimalType((double) heatpumpValues[14] / 10));
            updateState(CHANNEL_TEMPERATURE_OUTSIDE, new DecimalType((double) heatpumpValues[15] / 10));
            updateState(CHANNEL_TEMPERATURE_OUTSIDE_AVG, new DecimalType((double) heatpumpValues[16] / 10));
            updateState(CHANNEL_TEMPERATURE_SERVICEWATER, new DecimalType((double) heatpumpValues[17] / 10));
            updateState(CHANNEL_TEMPERATURE_SERVICEWATER_REFERENCE, new DecimalType((double) heatpumpValues[18] / 10));
            updateState(CHANNEL_TEMPERATURE_PROBE_IN, new DecimalType((double) heatpumpValues[19] / 10));
            updateState(CHANNEL_TEMPERATURE_PROBE_OUT, new DecimalType((double) heatpumpValues[20] / 10));
            updateState(CHANNEL_TEMPERATURE_MK1, new DecimalType((double) heatpumpValues[21] / 10));
            updateState(CHANNEL_TEMPERATURE_MK1_REFERENCE, new DecimalType((double) heatpumpValues[22] / 10));
            updateState(CHANNEL_TEMPERATURE_MK2, new DecimalType((double) heatpumpValues[24] / 10));
            updateState(CHANNEL_TEMPERATURE_MK2_REFERENCE, new DecimalType((double) heatpumpValues[25] / 10));
            updateState(CHANNEL_HEATPUMP_SOLAR_COLLECTOR, new DecimalType((double) heatpumpValues[26] / 10));
            updateState(CHANNEL_HEATPUMP_SOLAR_STORAGE, new DecimalType((double) heatpumpValues[27] / 10));
            updateState(CHANNEL_TEMPERATURE_EXTERNAL_SOURCE, new DecimalType((double) heatpumpValues[28] / 10));
            updateState(CHANNEL_TIME_COMPRESSOR1, new DecimalType((double) heatpumpValues[56]));
            updateState(CHANNEL_STARTS_COMPRESSOR1, new DecimalType((double) heatpumpValues[57]));
            updateState(CHANNEL_TIME_COMPRESSOR2, new DecimalType((double) heatpumpValues[58]));
            updateState(CHANNEL_STARTS_COMPRESSOR2, new DecimalType((double) heatpumpValues[59]));
            updateState(CHANNEL_TIME_ZWE1, new DecimalType((double) heatpumpValues[60]));
            updateState(CHANNEL_TIME_ZWE2, new DecimalType((double) heatpumpValues[61]));
            updateState(CHANNEL_TIME_ZWE3, new DecimalType((double) heatpumpValues[62]));
            updateState(CHANNEL_TIME_HETPUMP, new DecimalType((double) heatpumpValues[63]));
            updateState(CHANNEL_TIME_HEATING, new DecimalType((double) heatpumpValues[64]));
            updateState(CHANNEL_TIME_WARMWATER, new DecimalType((double) heatpumpValues[65]));
            updateState(CHANNEL_TIME_COOLING, new DecimalType((double) heatpumpValues[66]));
            updateState(CHANNEL_THERMALENERGY_HEATING, new DecimalType((double) heatpumpValues[151] / 10));
            updateState(CHANNEL_THERMALENERGY_WARMWATER, new DecimalType((double) heatpumpValues[152] / 10));
            updateState(CHANNEL_THERMALENERGY_POOL, new DecimalType((double) heatpumpValues[153] / 10));
            updateState(CHANNEL_THERMALENERGY_TOTAL, new DecimalType((double) heatpumpValues[154] / 10));
            updateState(CHANNEL_MASSFLOW, new DecimalType((double) heatpumpValues[155]));

            updateState(CHANNEL_HEATPUMP_STATE, new StringType(String.valueOf(heatpumpValues[117])));
            updateState(CHANNEL_HEATPUMP_STATE_TIME, new DecimalType((double) heatpumpValues[120]));

            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_0, new DecimalType(heatpumpValues[100]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_1, new DecimalType(heatpumpValues[101]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_2, new DecimalType(heatpumpValues[102]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_3, new DecimalType(heatpumpValues[103]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_4, new DecimalType(heatpumpValues[104]));

            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_TIMESTAMP_0,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[95])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_TIMESTAMP_1,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[96])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_TIMESTAMP_2,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[97])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_TIMESTAMP_3,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[98])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_TIMESTAMP_4,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[99])));

            updateState(CHANNEL_HEATPUMP_SWITCHOFF_ERROR_COUNT, new DecimalType(heatpumpValues[105]));

            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_0, new DecimalType(heatpumpValues[106]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_1, new DecimalType(heatpumpValues[107]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_2, new DecimalType(heatpumpValues[108]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_3, new DecimalType(heatpumpValues[109]));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_4, new DecimalType(heatpumpValues[110]));

            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_TIMESTAMP_0,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[111])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_TIMESTAMP_1,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[112])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_TIMESTAMP_2,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[113])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_TIMESTAMP_3,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[114])));
            updateState(CHANNEL_HEATPUMP_SWITCHOFF_REASON_TIMESTAMP_4,
                    new DateTimeType(getZdtFromTimestamp(heatpumpValues[115])));

            updateState(CHANNEL_HEATPUMP_EXTENDED_STATE, new StringType(String.valueOf(heatpumpValues[119])));

            updateState(CHANNEL_HEATING_TEMPERATURE, new DecimalType(heatpumpParams[PARAM_HEATING_TEMPERATURE] / 10.));
            updateState(CHANNEL_HEATING_OPERATION_MODE,
                    new StringType(String.valueOf(heatpumpParams[PARAM_HEATING_OPERATION_MODE])));
            updateState(CHANNEL_WARMWATER_TEMPERATURE,
                    new DecimalType(heatpumpParams[PARAM_WARMWATER_TEMPERATURE] / 10.));
            updateState(CHANNEL_WARMWATER_OPERATION_MODE,
                    new StringType(String.valueOf(heatpumpParams[PARAM_WARMWATER_OPERATION_MODE])));
            updateState(CHANNEL_COOLING_OPERATION_MODE,
                    new StringType(String.valueOf(heatpumpParams[PARAM_COOLING_OPERATION_MODE])));
            updateState(CHANNEL_COOLING_RELEASE_TEMPERATURE,
                    new DecimalType(heatpumpParams[PARAM_COOLING_RELEASE_TEMP] / 10.));
            updateState(CHANNEL_COOLING_INLET_TEMP, new DecimalType(heatpumpParams[PARAM_COOLING_INLET_TEMP] / 10.));
            updateState(CHANNEL_COOLING_START_AFTER_HOURS, new DecimalType(heatpumpParams[PARAM_COOLING_START] / 10.));
            updateState(CHANNEL_COOLING_STOP_AFTER_HOURS, new DecimalType(heatpumpParams[PARAM_COOLING_STOP] / 10.));

            // read all boolean output signals
            updateState(CHANNEL_OUTPUT_AV, OnOffType.from(heatpumpValues[37] != 0));
            updateState(CHANNEL_OUTPUT_BUP, OnOffType.from(heatpumpValues[38] != 0));
            updateState(CHANNEL_OUTPUT_HUP, OnOffType.from(heatpumpValues[39] != 0));
            updateState(CHANNEL_OUTPUT_MA1, OnOffType.from(heatpumpValues[40] != 0));
            updateState(CHANNEL_OUTPUT_MZ1, OnOffType.from(heatpumpValues[41] != 0));
            updateState(CHANNEL_OUTPUT_VEN, OnOffType.from(heatpumpValues[42] != 0));
            updateState(CHANNEL_OUTPUT_VBO, OnOffType.from(heatpumpValues[43] != 0));
            updateState(CHANNEL_OUTPUT_VD1, OnOffType.from(heatpumpValues[44] != 0));
            updateState(CHANNEL_OUTPUT_VD2, OnOffType.from(heatpumpValues[45] != 0));
            updateState(CHANNEL_OUTPUT_ZIP, OnOffType.from(heatpumpValues[46] != 0));
            updateState(CHANNEL_OUTPUT_ZUP, OnOffType.from(heatpumpValues[47] != 0));
            updateState(CHANNEL_OUTPUT_ZW1, OnOffType.from(heatpumpValues[48] != 0));
            updateState(CHANNEL_OUTPUT_ZW2SST, OnOffType.from(heatpumpValues[49] != 0));
            updateState(CHANNEL_OUTPUT_ZW3SST, OnOffType.from(heatpumpValues[50] != 0));
            updateState(CHANNEL_OUTPUT_FP2, OnOffType.from(heatpumpValues[51] != 0));
            updateState(CHANNEL_OUTPUT_SLP, OnOffType.from(heatpumpValues[52] != 0));
            updateState(CHANNEL_OUTPUT_SUP, OnOffType.from(heatpumpValues[53] != 0));
            updateState(CHANNEL_OUTPUT_MZ2, OnOffType.from(heatpumpValues[54] != 0));
            updateState(CHANNEL_OUTPUT_MA2, OnOffType.from(heatpumpValues[55] != 0));
            updateState(CHANNEL_OUTPUT_MZ3, OnOffType.from(heatpumpValues[138] != 0));
            updateState(CHANNEL_OUTPUT_MA3, OnOffType.from(heatpumpValues[139] != 0));
            updateState(CHANNEL_OUTPUT_FP3, OnOffType.from(heatpumpValues[140] != 0));
            updateState(CHANNEL_OUTPUT_VSK, OnOffType.from(heatpumpValues[166] != 0));
            updateState(CHANNEL_OUTPUT_FRH, OnOffType.from(heatpumpValues[167] != 0));

            if (heatpumpValues.length > 213) {
                updateState(CHANNEL_OUTPUT_AV2, OnOffType.from(heatpumpValues[213] != 0));
                updateState(CHANNEL_OUTPUT_VBO2, OnOffType.from(heatpumpValues[214] != 0));
                updateState(CHANNEL_OUTPUT_VD12, OnOffType.from(heatpumpValues[215] != 0));
                updateState(CHANNEL_OUTPUT_VDH2, OnOffType.from(heatpumpValues[216] != 0));
            }
            if (isInitialized()) {
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (RuntimeException e) {
            logger.debug("Unexpected error for thing {} ", getThing().getUID(), e);
            if (isInitialized()) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
            }
        }
    }
}
