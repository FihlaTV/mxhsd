/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxhsd.core.room;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.hs.RoomMembership;
import io.kamax.mxhsd.Caches;
import io.kamax.mxhsd.GsonUtil;
import io.kamax.mxhsd.Lists;
import io.kamax.mxhsd.api.event.IEvent;
import io.kamax.mxhsd.api.event.IProtoEvent;
import io.kamax.mxhsd.api.exception.NotFoundException;
import io.kamax.mxhsd.api.federation.IRemoteHomeServer;
import io.kamax.mxhsd.api.room.*;
import io.kamax.mxhsd.api.room.directory.IFederatedRoomAliasLookup;
import io.kamax.mxhsd.api.room.event.*;
import io.kamax.mxhsd.core.GlobalStateHolder;
import io.kamax.mxhsd.core.event.Event;
import io.kamax.mxhsd.core.store.dao.RoomDao;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoomManager implements IRoomManager {

    private Logger log = LoggerFactory.getLogger(RoomManager.class);

    private GlobalStateHolder global;
    private LoadingCache<String, Room> rooms;
    private IAllRoomsHandler arHandler;

    public RoomManager(GlobalStateHolder global) {
        this.global = global;
        this.rooms = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Room>() {
                    @Override
                    public Room load(String key) {
                        log.info("Loading room {} in cache", key);
                        RoomDao dao = global.getStore().findRoom(key).orElseThrow(() -> new NotFoundException(key));
                        log.info("Found room {} in store with extremities {}", key, dao.getExtremities());
                        Room r = new Room(global, dao.getId(), dao.getExtremities());
                        r.addListener(arHandler);
                        log.info("Loaded room {}", key);
                        return r;
                    }
                });

        this.arHandler = new IAllRoomsHandler() {

            private MBassador<IEvent> bus = new MBassador<>(new IPublicationErrorHandler.ConsoleLogger(true));

            @Handler
            private void receiver(IEvent ev) {
                bus.publish(ev);
            }

            @Override
            public void addListener(Object o) {
                bus.subscribe(o);
            }

        };
    }

    private boolean hasRoom(String id) {
        return rooms.asMap().containsKey(id);
    }

    private String getId() {
        String id;
        do {
            id = "!" + RandomStringUtils.randomAlphanumeric(16) + ":" + global.getDomain();
        } while (hasRoom(id));

        log.info("Generated Room ID {}", id);
        return id;
    }

    // TODO make it configurable via JSON data
    private RoomPowerLevels getPowerLevelEvent(IRoomCreateOptions options) {
        return RoomPowerLevels.build().defaults()
                .addUser(options.getCreator().getId(), PowerLevel.Admin) // Adding creator
                .get();
    }

    private Room saveAndLoad(Room r) {
        RoomDao dao = new RoomDao();
        dao.setId(r.getId());
        dao.setExtremities(Lists.map(r.getExtremities(), IProtoEvent::getId));
        global.getStore().putRoom(dao);
        return Caches.get(rooms, r.getId());
    }

    @Override
    public Room createRoom(IRoomCreateOptions options) { // FIXME use RWLock
        String creator = options.getCreator().getId();
        String id = getId();
        Room room = new Room(global, id);

        synchronized (rooms) {
            room.inject(new RoomCreateEvent(creator));
            room.inject(new RoomMembershipEvent(creator, RoomMembership.Join.get(), creator));
            room.inject(new RoomPowerLevelEvent(creator, getPowerLevelEvent(options)));

            options.getPreset().ifPresent(p -> {
                log.info("Checking presets for room  {} creation", id);

                if (StringUtils.equals(p, "public_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "public"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));
                } else if (StringUtils.equals(p, "private_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "invite"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));
                } else if (StringUtils.equals(p, "trusted_private_chat")) {
                    log.info("Applying preset {} for room {}", p, id);
                    room.inject(new RoomJoinRulesEvent(creator, "invite"));
                    room.inject(new RoomHistoryVisibilityEvent(creator, "shared"));

                    RoomPowerLevels pls = room.getCurrentState().getEffectivePowerLevels();
                    long creatorPl = pls.getForUser(creator);
                    RoomPowerLevels.Builder plsBuilder = RoomPowerLevels.Builder.from(pls);
                    options.getInvitees().forEach(iId -> plsBuilder.addUser(iId.getId(), creatorPl));
                    room.inject(new RoomPowerLevelEvent(creator, plsBuilder.get()));
                } else {
                    log.info("Ignoring unknown preset {} for room {}", p, id);
                }
            });

            // FIXME handle initial_state

            // TODO handle name

            // TODO handle topic

            options.getInvitees().forEach(mxId -> {
                room.inject(new RoomMembershipEvent(creator, RoomMembership.Invite.get(), mxId.getId()));
            });

            // TODO handle invite_3pid

            Room roomProcessed = saveAndLoad(room);
            log.info("Room {} created", id);
            return roomProcessed;
        }
    }

    @Override
    public IRoom discoverRoom(String roomId, List<IEvent> initialState, List<IEvent> authChain, IEvent seed) {
        return saveAndLoad(new Room(global, roomId, initialState, authChain, seed));
    }

    private Room joinRemoteRoom(String hs, String roomId, _MatrixID userId) {
        IRemoteHomeServer rHs = global.getHsMgr().get(hs);
        JsonObject protoEv = rHs.makeJoin(roomId, userId).getAsJsonObject("event");
        log.debug("Proto-event for remote join: {}", GsonUtil.getPrettyForLog(protoEv));
        IEvent joinEv = global.getEvMgr().finalize(protoEv);
        JsonObject data = rHs.sendJoin(joinEv);
        log.debug("Remote data before join: {}", GsonUtil.getPrettyForLog(data));

        List<IEvent> state = GsonUtil.asList(data, "state", JsonObject.class)
                .stream().map(Event::new).collect(Collectors.toList());
        state.add(joinEv);
        List<IEvent> authChain = GsonUtil.asList(data, "auth_chain", JsonObject.class)
                .stream().map(Event::new).collect(Collectors.toList());

        synchronized (rooms) {
            log.info("Processing state after join");
            return saveAndLoad(new Room(RoomManager.this.global, roomId, state, authChain, joinEv));
        }
    }

    @Override
    public IAliasRoom getRoom(final IFederatedRoomAliasLookup lookup) {
        return findRoom(lookup.getId()).map(r -> (IAliasRoom) r).orElseGet(() -> {
            List<String> servers = new ArrayList<>(lookup.getServers());

            // We remove ourselves in case the remote server thinks we are already in the room
            servers.remove(global.getDomain());
            if (lookup.getServers().isEmpty()) {
                throw new IllegalArgumentException("Cannot join a room without resident homeservers");
            }

            // We want to try the server that answered the lookup first, if it's part of the room
            if (lookup.getServers().contains(lookup.getSource())) {
                servers.remove(lookup.getSource());
                servers.add(0, lookup.getSource());
            }

            return userId -> {
                for (String server : servers) {
                    try {
                        return joinRemoteRoom(lookup.getSource(), lookup.getId(), userId);
                    } catch (RuntimeException e) {
                        log.warn("Unable to join {} using {}: {}", lookup.getAlias(), server, e.getMessage(), e);
                    }
                }

                throw new RuntimeException("Unable to join " + lookup.getAlias() + ": all servers failed");
            };
        });
    }

    @Override
    public IRoom getRoom(String id) {
        return Caches.get(rooms, id);
    }

    @Override
    public List<String> listRooms() {
        return new ArrayList<>(Lists.map(global.getStore().listRooms(), RoomDao::getId));
    }

    @Override
    public IAllRoomsHandler forAllRooms() {
        return arHandler;
    }

}
