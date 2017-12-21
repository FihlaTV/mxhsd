/*
 * mxhsd - Corporate Matrix Homeserver
 * Copyright (C) 2017 Maxime Dor
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

package io.kamax.mxhsd.core.session.server;

import io.kamax.mxhsd.api.room.directory.IRoomAliasLookup;
import io.kamax.mxhsd.api.session.server.IServerRoomDirectory;
import io.kamax.mxhsd.core.HomeserverState;

import java.util.List;
import java.util.Optional;

public class ServerRoomDirectory implements IServerRoomDirectory {

    private HomeserverState global;

    public ServerRoomDirectory(HomeserverState global) {
        this.global = global;
    }

    @Override
    public List<String> getAliases(String roomId) {
        return global.getRoomDir().getAliases(roomId);
    }

    @Override
    public Optional<IRoomAliasLookup> lookup(String alias) {
        return global.getRoomDir().lookup(alias);
    }

}