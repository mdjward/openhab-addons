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
package org.openhab.binding.netatmo.internal.presence;

import io.swagger.client.model.NAWelcomeCamera;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.internal.ThingImpl;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openhab.binding.netatmo.internal.NetatmoBindingConstants;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Sven Strohschein
 */
@RunWith(MockitoJUnitRunner.class)
public class NAPresenceCameraHandlerTest {

    private static final String DUMMY_VPN_URL = "https://dummytestvpnaddress.net/restricted/10.255.89.96/9826069dc689e8327ac3ed2ced4ff089/MTU5MTgzMzYwMDrQ7eHHhG0_OJ4TgmPhGlnK7QQ5pZ,,";
    private static final String DUMMY_LOCAL_URL = "http://192.168.178.76/9826069dc689e8327ac3ed2ced4ff089";
    private static final Optional<String> DUMMY_PING_RESPONSE = createPingResponseContent(DUMMY_LOCAL_URL);

    @Mock
    private RequestExecutor requestExecutorMock;
    @Mock
    private TimeZoneProvider timeZoneProviderMock;

    private Thing presenceCameraThing;
    private NAWelcomeCamera presenceCamera;
    private ChannelUID floodlightChannelUID;
    private ChannelUID floodlightAutoModeChannelUID;
    private NAPresenceCameraHandlerAccessible handler;

    @Before
    public void before() {
        presenceCameraThing = new ThingImpl(new ThingTypeUID("netatmo", "NOC"), "1");
        presenceCamera = new NAWelcomeCamera();

        floodlightChannelUID = new ChannelUID(presenceCameraThing.getUID(), NetatmoBindingConstants.CHANNEL_CAMERA_FLOODLIGHT);
        floodlightAutoModeChannelUID = new ChannelUID(presenceCameraThing.getUID(), NetatmoBindingConstants.CHANNEL_CAMERA_FLOODLIGHT_AUTO_MODE);

        handler = new NAPresenceCameraHandlerAccessible(presenceCameraThing, presenceCamera);
    }

    @Test
    public void testHandleCommand_Switch_Floodlight_on() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(2)).executeGETRequest(any()); //1.) execute ping + 2.) execute switch on
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22on%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_Floodlight_off() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.OFF);

        verify(requestExecutorMock, times(2)).executeGETRequest(any()); //1.) execute ping + 2.) execute switch off
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22off%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_Floodlight_off_with_AutoMode_on() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.AUTO);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));

        handler.handleCommand(floodlightChannelUID, OnOffType.OFF);

        verify(requestExecutorMock, times(2)).executeGETRequest(any()); //1.) execute ping + 2.) execute switch off
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22auto%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_Floodlight_on_address_changed() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);
        //1.) execute ping + 2.) execute switch on
        verify(requestExecutorMock, times(2)).executeGETRequest(any());
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22on%22%7D");

        handler.handleCommand(floodlightChannelUID, OnOffType.OFF);
        //1.) execute ping + 2.) execute switch on + 3.) execute switch off
        verify(requestExecutorMock, times(3)).executeGETRequest(any());
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22on%22%7D");
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22off%22%7D");

        final String newDummyVPNURL = DUMMY_VPN_URL + "2";
        final String newDummyLocalURL = DUMMY_LOCAL_URL + "2";
        final Optional<String> newDummyPingResponse = createPingResponseContent(newDummyLocalURL);

        when(requestExecutorMock.executeGETRequest(newDummyVPNURL + "/command/ping")).thenReturn(newDummyPingResponse);

        presenceCamera.setVpnUrl(newDummyVPNURL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);
        //1.) execute ping + 2.) execute switch on + 3.) execute switch off + 4.) execute ping + 5.) execute switch on
        verify(requestExecutorMock, times(5)).executeGETRequest(any());
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22on%22%7D");
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22off%22%7D");
        verify(requestExecutorMock).executeGETRequest(newDummyLocalURL + "/command/floodlight_set_config?config=%7B%22mode%22:%22on%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_Floodlight_unknown_command() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, RefreshType.REFRESH);

        verify(requestExecutorMock, never()).executeGETRequest(any()); //nothing should get executed on a refresh command
    }

    @Test
    public void testHandleCommand_Switch_FloodlightAutoMode_on() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);

        handler.handleCommand(floodlightAutoModeChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(2)).executeGETRequest(any()); //1.) execute ping + 2.) execute switch auto-mode on
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22auto%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_FloodlightAutoMode_off() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(DUMMY_PING_RESPONSE);

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);

        handler.handleCommand(floodlightAutoModeChannelUID, OnOffType.OFF);

        verify(requestExecutorMock, times(2)).executeGETRequest(any()); //1.) execute ping + 2.) execute switch off
        verify(requestExecutorMock).executeGETRequest(DUMMY_LOCAL_URL + "/command/floodlight_set_config?config=%7B%22mode%22:%22off%22%7D");
    }

    @Test
    public void testHandleCommand_Switch_FloodlightAutoMode_unknown_command() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightAutoModeChannelUID, RefreshType.REFRESH);

        verify(requestExecutorMock, never()).executeGETRequest(any()); //nothing should get executed on a refresh command
    }

    /**
     * The request "fails" because there is no response content of the ping command.
     */
    @Test
    public void testHandleCommand_Request_failed() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(1)).executeGETRequest(any()); //1.) execute ping
    }

    @Test
    public void testHandleCommand_without_VPN() {
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, never()).executeGETRequest(any()); //no executions because the VPN URL is still unknown
    }

    @Test
    public void testHandleCommand_Ping_failed_NULL_Response() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(Optional.of(""));

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(1)).executeGETRequest(any()); //1.) execute ping
    }

    @Test
    public void testHandleCommand_Ping_failed_empty_Response() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(Optional.of(""));

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(1)).executeGETRequest(any()); //1.) execute ping
    }

    @Test
    public void testHandleCommand_Ping_failed_wrong_Response() {
        when(requestExecutorMock.executeGETRequest(DUMMY_VPN_URL + "/command/ping")).thenReturn(Optional.of("{ \"message\":  \"error\" }"));

        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        handler.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, times(1)).executeGETRequest(any()); //1.) execute ping
    }

    @Test
    public void testHandleCommand_Module_NULL() {
        NAPresenceCameraHandler handlerWithoutModule = new NAPresenceCameraHandler(presenceCameraThing, timeZoneProviderMock);
        handlerWithoutModule.handleCommand(floodlightChannelUID, OnOffType.ON);

        verify(requestExecutorMock, never()).executeGETRequest(any()); //no executions because the thing isn't initialized
    }

    @Test
    public void testGetNAThingProperty_Common_Channel() {
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(NetatmoBindingConstants.CHANNEL_CAMERA_STATUS));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_On() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.ON);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_Off() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.OFF);
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_Auto() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.AUTO);
        //When the floodlight is set to auto-mode it is currently off.
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_without_LightModeState() {
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_Module_NULL() {
        NAPresenceCameraHandler handlerWithoutModule = new NAPresenceCameraHandler(presenceCameraThing, timeZoneProviderMock);
        assertEquals(UnDefType.UNDEF, handlerWithoutModule.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_FloodlightAutoMode_Floodlight_Auto() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.AUTO);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_FloodlightAutoMode_Floodlight_On() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.ON);
        //When the floodlight is initially on (on starting the binding), there is no information about if the auto-mode
        // was set before. Therefore the auto-mode is detected as deactivated / off.
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_FloodlightAutoMode_Floodlight_Off() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.ON);
        //When the floodlight is initially off (on starting the binding), the auto-mode isn't set.
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_Scenario_with_AutoMode() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.AUTO);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));

        //The auto-mode was initially set, after that the floodlight was switched on by the user.
        // In this case the binding should still know that the auto-mode is/was set.
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.ON);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightChannelUID.getId()));

        //After that the user switched off the floodlight.
        // In this case the binding should still know that the auto-mode is/was set.
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.OFF);
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_Floodlight_Scenario_without_AutoMode() {
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.OFF);
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));

        //The auto-mode wasn't set, after that the floodlight was switched on by the user.
        // In this case the binding should still know that the auto-mode isn't/wasn't set.
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.ON);
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.ON, handler.getNAThingProperty(floodlightChannelUID.getId()));

        //After that the user switched off the floodlight.
        // In this case the binding should still know that the auto-mode isn't/wasn't set.
        presenceCamera.setLightModeStatus(NAWelcomeCamera.LightModeStatusEnum.OFF);
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
        assertEquals(OnOffType.OFF, handler.getNAThingProperty(floodlightChannelUID.getId()));
    }

    @Test
    public void testGetNAThingProperty_FloodlightAutoMode_Module_NULL() {
        NAPresenceCameraHandler handlerWithoutModule = new NAPresenceCameraHandler(presenceCameraThing, timeZoneProviderMock);
        assertEquals(UnDefType.UNDEF, handlerWithoutModule.getNAThingProperty(floodlightAutoModeChannelUID.getId()));
    }

    @Test
    public void testGetStreamURL() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        String streamURL = handler.getStreamURL("dummyVideoId");
        assertNotNull(streamURL);
        assertEquals(DUMMY_VPN_URL + "/vod/dummyVideoId/index.m3u8", streamURL);
    }

    @Test
    public void testGetStreamURL_local() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        presenceCamera.setIsLocal(true);

        String streamURL = handler.getStreamURL("dummyVideoId");
        assertNotNull(streamURL);
        assertEquals(DUMMY_VPN_URL + "/vod/dummyVideoId/index_local.m3u8", streamURL);
    }

    @Test
    public void testGetStreamURL_not_local() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);
        presenceCamera.setIsLocal(false);

        String streamURL = handler.getStreamURL("dummyVideoId");
        assertNotNull(streamURL);
        assertEquals(DUMMY_VPN_URL + "/vod/dummyVideoId/index.m3u8", streamURL);
    }

    @Test
    public void testGetStreamURL_without_VPN() {
        String streamURL = handler.getStreamURL("dummyVideoId");
        assertNull(streamURL);
    }

    @Test
    public void testGetLivePictureURLState() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);

        State livePictureURLState = handler.getLivePictureURLState();
        assertEquals(new StringType(DUMMY_VPN_URL + "/live/snapshot_720.jpg"), livePictureURLState);
    }

    @Test
    public void testGetLivePictureURLState_without_VPN() {
        State livePictureURLState = handler.getLivePictureURLState();
        assertEquals(UnDefType.UNDEF, livePictureURLState);
    }

    @Test
    public void testGetLiveStreamState() {
        presenceCamera.setVpnUrl(DUMMY_VPN_URL);

        State liveStreamState = handler.getLiveStreamState();
        assertEquals(new StringType(DUMMY_VPN_URL + "/live/index.m3u8"), liveStreamState);
    }

    @Test
    public void testGetLiveStreamState_without_VPN() {
        State liveStreamState = handler.getLiveStreamState();
        assertEquals(UnDefType.UNDEF, liveStreamState);
    }

    private static Optional<String> createPingResponseContent(final String localURL) {
        return Optional.of("{\"local_url\":\"" + localURL + "\",\"product_name\":\"Welcome Netatmo\"}");
    }

    private interface RequestExecutor {

        Optional<String> executeGETRequest(String url);
    }

    private class NAPresenceCameraHandlerAccessible extends NAPresenceCameraHandler {

        public NAPresenceCameraHandlerAccessible(Thing thing, NAWelcomeCamera presenceCamera) {
            super(thing, timeZoneProviderMock);
            module = presenceCamera;
        }

        @Override
        protected @NonNull Optional<@NonNull String> executeGETRequest(@NonNull String url) {
            return requestExecutorMock.executeGETRequest(url);
        }

        @Override
        protected @NonNull State getLivePictureURLState() {
            return super.getLivePictureURLState();
        }

        @Override
        protected @NonNull State getLiveStreamState() {
            return super.getLiveStreamState();
        }
    }
}
