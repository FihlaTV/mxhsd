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

package io.kamax.mxhsd.api.room.event;

import io.kamax.mxhsd.api.event.NakedRoomEvent;
import io.kamax.mxhsd.api.room.RoomEventType;
import io.kamax.mxhsd.core.room.RoomPowerLevels;

public class RoomPowerLevelEvent extends NakedRoomEvent {

    private RoomPowerLevels content;

    public RoomPowerLevelEvent(String sender, String roomId, RoomPowerLevels pls) {
        super(RoomEventType.PowerLevels.get(), sender, roomId);
        this.content = pls;
    }

    public RoomPowerLevels getContent() {
        return content;
    }

}